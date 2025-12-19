package com.marketplace.catalog.service;

import com.marketplace.catalog.domain.model.Category;
import com.marketplace.catalog.domain.repository.CategoryRepository;
import com.marketplace.catalog.exception.CategoryNotFoundException;
import com.marketplace.catalog.exception.DuplicateCategoryException;
import com.marketplace.shared.dto.catalog.CategoryResponse;
import com.marketplace.shared.dto.catalog.CreateCategoryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {
    
    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);
    private final CategoryRepository categoryRepository;
    
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
    
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.name())) {
            throw new DuplicateCategoryException(request.name());
        }
        
        Category category = new Category();
        category.setName(request.name());
        category.setDescription(request.description());
        
        if (request.parentCategoryId() != null) {
            Category parentCategory = categoryRepository.findById(request.parentCategoryId())
                .orElseThrow(() -> new CategoryNotFoundException(request.parentCategoryId()));
            category.setParentCategory(parentCategory);
        }
        
        category = categoryRepository.save(category);
        log.info("Created category: {} (id: {})", category.getName(), category.getId());
        
        return toResponse(category);
    }
    
    @Transactional(readOnly = true)
    public CategoryResponse getCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        return toResponse(category);
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponse> getTopLevelCategories() {
        return categoryRepository.findByParentCategoryIsNull().stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubcategories(Long parentCategoryId) {
        return categoryRepository.findByParentCategoryId(parentCategoryId).stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }
    
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new CategoryNotFoundException(id));
        
        categoryRepository.delete(category);
        log.info("Deleted category: {} (id: {})", category.getName(), id);
    }
    
    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            category.getParentCategory() != null ? category.getParentCategory().getId() : null,
            category.getParentCategory() != null ? category.getParentCategory().getName() : null,
            category.getCreatedAt(),
            category.getUpdatedAt()
        );
    }
}
