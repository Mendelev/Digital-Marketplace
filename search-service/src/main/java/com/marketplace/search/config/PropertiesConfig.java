package com.marketplace.search.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class to explicitly enable SearchProperties.
 */
@Configuration
@EnableConfigurationProperties(SearchProperties.class)
public class PropertiesConfig {
}
