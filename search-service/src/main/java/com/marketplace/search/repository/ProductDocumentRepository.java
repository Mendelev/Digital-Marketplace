package com.marketplace.search.repository;

import com.marketplace.search.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for ProductDocument CRUD operations.
 */
@Repository
public interface ProductDocumentRepository extends ElasticsearchRepository<ProductDocument, String> {

    Optional<ProductDocument> findByProductId(String productId);

    void deleteByProductId(String productId);

    List<ProductDocument> findByStatus(String status);

    List<ProductDocument> findByCategoryName(String categoryName);
}
