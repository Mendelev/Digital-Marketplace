package com.marketplace.search.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Search Service.
 */
@ConfigurationProperties(prefix = "search")
public record SearchProperties(
        IndexConfig index,
        PaginationConfig pagination,
        KafkaConfig kafka
) {
    public record IndexConfig(
            String name,
            int numberOfShards,
            int numberOfReplicas
    ) {}

    public record PaginationConfig(
            int defaultPageSize,
            int maxPageSize
    ) {}

    public record KafkaConfig(
            String topic
    ) {}
}
