package com.marketplace.search.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.json.JsonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.StringReader;
import java.util.Map;

/**
 * Initializes Elasticsearch index on application startup.
 */
@Component
public class IndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndexInitializer.class);

    private final ElasticsearchClient elasticsearchClient;
    private final SearchProperties searchProperties;

    public IndexInitializer(ElasticsearchClient elasticsearchClient,
                           SearchProperties searchProperties) {
        this.elasticsearchClient = elasticsearchClient;
        this.searchProperties = searchProperties;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String indexName = searchProperties.index().name();

        log.info("Checking if index '{}' exists", indexName);

        boolean exists = elasticsearchClient.indices()
                .exists(ExistsRequest.of(e -> e.index(indexName)))
                .value();

        if (!exists) {
            log.info("Index '{}' does not exist. Creating...", indexName);
            createIndex(indexName);
            log.info("Index '{}' created successfully", indexName);
        } else {
            log.info("Index '{}' already exists", indexName);
        }
    }

    private void createIndex(String indexName) throws Exception {
        String mappings = """
                {
                  "properties": {
                    "productId": { "type": "keyword" },
                    "name": {
                      "type": "text",
                      "fields": {
                        "keyword": { "type": "keyword" },
                        "suggest": {
                          "type": "completion",
                          "contexts": [
                            {
                              "name": "status",
                              "type": "category"
                            }
                          ]
                        }
                      }
                    },
                    "description": { "type": "text" },
                    "basePrice": { "type": "scaled_float", "scaling_factor": 100 },
                    "categoryName": {
                      "type": "text",
                      "fields": { "keyword": { "type": "keyword" } }
                    },
                    "sellerId": { "type": "keyword" },
                    "status": { "type": "keyword" },
                    "availableSizes": { "type": "keyword" },
                    "availableColors": { "type": "keyword" },
                    "thumbnailUrl": { "type": "keyword" },
                    "featured": { "type": "boolean" },
                    "createdAt": { "type": "date" },
                    "updatedAt": { "type": "date" }
                  }
                }
                """;

        elasticsearchClient.indices().create(CreateIndexRequest.of(c -> c
                .index(indexName)
                .settings(IndexSettings.of(s -> s
                        .numberOfShards(String.valueOf(searchProperties.index().numberOfShards()))
                        .numberOfReplicas(String.valueOf(searchProperties.index().numberOfReplicas()))
                ))
                .withJson(new StringReader(mappings))
        ));
    }
}
