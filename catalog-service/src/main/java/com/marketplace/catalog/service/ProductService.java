package com.marketplace.catalog.service;

import com.marketplace.catalog.domain.enums.ProductStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.marketplace.catalog.domain.model.Category;
import com.marketplace.catalog.domain.model.Product;
import com.marketplace.catalog.domain.repository.CategoryRepository;
import com.marketplace.catalog.domain.repository.ProductRepository;
import com.marketplace.catalog.event.ProductEventPublisher;
import com.marketplace.catalog.exception.CategoryNotFoundException;
import com.marketplace.catalog.exception.ForbiddenException;
import com.marketplace.catalog.exception.ProductNotFoundException;
import com.marketplace.shared.dto.catalog.CreateProductRequest;
import com.marketplace.shared.dto.catalog.ProductResponse;
import com.marketplace.shared.dto.catalog.UpdateProductRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ProductService {
    
    private static final Logger log = LoggerFactory.getLogger(ProductService.class);
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageValidationService imageValidationService;
    private final PriceHistoryService priceHistoryService;
    private final ProductEventPublisher eventPublisher;
    private final SearchServiceClient searchServiceClient;
    
    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, ImageValidationService imageValidationService, PriceHistoryService priceHistoryService, ProductEventPublisher eventPublisher, SearchServiceClient searchServiceClient) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.imageValidationService = imageValidationService;
        this.priceHistoryService = priceHistoryService;
        this.eventPublisher = eventPublisher;
        this.searchServiceClient = searchServiceClient;
    }
    
    public ProductResponse createProduct(CreateProductRequest request, UUID effectiveSellerId) {
        // Validate image URLs BEFORE saving
        imageValidationService.validateImageUrls(request.imageUrls());
        
        Category category = categoryRepository.findById(request.categoryId())
            .orElseThrow(() -> new CategoryNotFoundException(request.categoryId()));
        
        Product product = new Product();
        product.setSellerId(effectiveSellerId);
        product.setName(request.name());
        product.setDescription(request.description());
        product.setBasePrice(request.basePrice());
        product.setCategory(category);
        product.setAvailableSizes(request.availableSizes());
        product.setAvailableColors(request.availableColors());
        product.setStockPerVariant(request.stockPerVariant());
        product.setImageUrls(request.imageUrls());
        product.setStatus(ProductStatus.ACTIVE);
        
        product = productRepository.save(product);
        log.info("Created product: {} (id: {}) for seller: {}", product.getName(), product.getId(), effectiveSellerId);
        
        // Publish event with idempotency
        eventPublisher.publishProductCreated(product);
        
        // Synchronous search service indexing
        searchServiceClient.indexProduct(product);
        
        return toResponse(product);
    }
    
    public ProductResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        // Validate new image URLs if provided
        if (request.imageUrls() != null && request.imageUrls().length > 0) {
            imageValidationService.validateImageUrls(request.imageUrls());
        }
        
        // Track price changes
        if (request.basePrice() != null && 
            request.basePrice().compareTo(product.getBasePrice()) != 0) {
            priceHistoryService.recordPriceChange(
                product.getId(), 
                product.getBasePrice(), 
                request.basePrice(), 
                product.getSellerId()
            );
            product.setBasePrice(request.basePrice());
        }
        
        // Update fields
        if (request.name() != null) product.setName(request.name());
        if (request.description() != null) product.setDescription(request.description());
        if (request.availableSizes() != null) product.setAvailableSizes(request.availableSizes());
        if (request.availableColors() != null) product.setAvailableColors(request.availableColors());
        if (request.stockPerVariant() != null) product.setStockPerVariant(request.stockPerVariant());
        if (request.imageUrls() != null && request.imageUrls().length > 0) product.setImageUrls(request.imageUrls());
        if (request.status() != null) product.setStatus(ProductStatus.valueOf(request.status()));
        
        product = productRepository.save(product);
        log.info("Updated product: {} (id: {})", product.getName(), productId);
        
        // Publish event with idempotency
        eventPublisher.publishProductUpdated(product);
        
        // Synchronous search service update
        searchServiceClient.updateProduct(product);
        
        return toResponse(product);
    }
    
    @Transactional(readOnly = true)
    public ProductResponse getProduct(UUID productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        return toResponse(product);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(Long categoryId, UUID sellerId, ProductStatus status, Pageable pageable) {
        return productRepository.findByFilters(categoryId, sellerId, status, pageable)
            .map(this::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsBySeller(UUID sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
            .map(this::toResponse);
    }
    
    @Transactional(readOnly = true)
    public Page<ProductResponse> getFeaturedProducts(Pageable pageable) {
        return productRepository.findByFeaturedTrue(pageable)
            .map(this::toResponse);
    }
    
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        productRepository.delete(product);
        log.info("Deleted product: {} (id: {})", product.getName(), productId);
        
        // Publish event
        eventPublisher.publishProductDeleted(productId);
        
        // Remove from search index
        searchServiceClient.deleteProductFromIndex(productId);
    }
    
    /**
     * Verifies that the user owns the product (or is admin)
     * @throws ForbiddenException if user doesn't own the product
     */
    public void verifyOwnership(UUID productId, UUID userId, boolean isAdmin) {
        if (isAdmin) {
            return; // Admins can access all products
        }
        
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ProductNotFoundException(productId));
        
        if (!product.getSellerId().equals(userId)) {
            throw new ForbiddenException("You don't have permission to access this product");
        }
    }
    
    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSellerId(),
            product.getName(),
            product.getDescription(),
            product.getBasePrice(),
            product.getCategory().getId(),
            product.getCategory().getName(),
            product.getAvailableSizes(),
            product.getAvailableColors(),
            product.getStockPerVariant(),
            product.getImageUrls(),
            product.getStatus().name(),
            product.isFeatured(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
