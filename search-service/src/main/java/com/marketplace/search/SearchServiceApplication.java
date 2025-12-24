package com.marketplace.search;

import com.marketplace.search.config.SearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Main application class for Search Service.
 * Provides product search functionality with Elasticsearch.
 */
@SpringBootApplication
@EnableKafka
@ConfigurationPropertiesScan
@EnableConfigurationProperties(SearchProperties.class)
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
