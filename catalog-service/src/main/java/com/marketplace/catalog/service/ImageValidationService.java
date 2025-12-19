package com.marketplace.catalog.service;

import com.marketplace.catalog.exception.InvalidImageUrlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class ImageValidationService {
    
    private static final Logger log = LoggerFactory.getLogger(ImageValidationService.class);
    
    private final WebClient webClient;
    
    public ImageValidationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024))
            .build();
    }
    
    /**
     * Validates that all image URLs are accessible via HTTP HEAD request
     * @throws InvalidImageUrlException if any URL is not accessible
     */
    public void validateImageUrls(String[] imageUrls) {
        if (imageUrls == null || imageUrls.length == 0) {
            throw new InvalidImageUrlException("At least one image URL is required");
        }
        
        List<String> invalidUrls = new ArrayList<>();
        
        for (String url : imageUrls) {
            if (!isValidUrl(url)) {
                invalidUrls.add(url + " (invalid format)");
                continue;
            }
            
            try {
                var response = webClient.head()
                    .uri(url)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(Duration.ofSeconds(3))
                    .block();
                
                if (response != null && !response.getStatusCode().is2xxSuccessful()) {
                    invalidUrls.add(url + " (status: " + response.getStatusCode().value() + ")");
                }
            } catch (WebClientResponseException e) {
                invalidUrls.add(url + " (HTTP " + e.getStatusCode().value() + ")");
                log.warn("Image URL validation failed for {}: {}", url, e.getMessage());
            } catch (Exception e) {
                invalidUrls.add(url + " (not accessible: " + e.getMessage() + ")");
                log.warn("Image URL validation failed for {}: {}", url, e.getMessage());
            }
        }
        
        if (!invalidUrls.isEmpty()) {
            throw new InvalidImageUrlException(
                "Invalid or inaccessible image URLs: " + String.join(", ", invalidUrls)
            );
        }
    }
    
    private boolean isValidUrl(String url) {
        try {
            new URL(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
