package com.marketplace.search.service;

import com.marketplace.search.document.ProductDocument;
import com.marketplace.search.exception.IndexingException;
import com.marketplace.search.repository.ProductDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service for indexing products in Elasticsearch.
 */
@Service
public class IndexingService {

    private static final Logger log = LoggerFactory.getLogger(IndexingService.class);

    private final ProductDocumentRepository productDocumentRepository;

    public IndexingService(ProductDocumentRepository productDocumentRepository) {
        this.productDocumentRepository = productDocumentRepository;
    }

    /**
     * Index a new product document.
     */
    public void indexProduct(ProductDocument document) {
        try {
            log.info("Indexing product: {}", document.getProductId());
            productDocumentRepository.save(document);
            log.info("Product indexed successfully: {}", document.getProductId());
        } catch (Exception e) {
            log.error("Failed to index product: {}", document.getProductId(), e);
            throw new IndexingException("Failed to index product: " + document.getProductId(), e);
        }
    }

    /**
     * Update an existing product document (upsert operation).
     */
    public void updateProduct(ProductDocument document) {
        try {
            log.info("Updating product: {}", document.getProductId());
            productDocumentRepository.save(document); // Elasticsearch save is upsert
            log.info("Product updated successfully: {}", document.getProductId());
        } catch (Exception e) {
            log.error("Failed to update product: {}", document.getProductId(), e);
            throw new IndexingException("Failed to update product: " + document.getProductId(), e);
        }
    }

    /**
     * Delete a product document from index.
     */
    public void deleteProduct(String productId) {
        try {
            log.info("Deleting product: {}", productId);
            productDocumentRepository.deleteByProductId(productId);
            log.info("Product deleted successfully: {}", productId);
        } catch (Exception e) {
            log.error("Failed to delete product: {}", productId, e);
            throw new IndexingException("Failed to delete product: " + productId, e);
        }
    }

    /**
     * Check if a product exists in the index.
     */
    public boolean productExists(String productId) {
        return productDocumentRepository.findByProductId(productId).isPresent();
    }
}
