package com.marketplace.auth.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to extract or generate correlation ID for request tracing.
 * Correlation ID is stored in MDC for logging and propagated to downstream services.
 */
@Component
@Order(1)
public class CorrelationIdFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_MDC_KEY = "correlationId";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        try {
            // Extract correlation ID from request header or generate new one
            String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.trim().isEmpty()) {
                correlationId = UUID.randomUUID().toString();
                log.debug("Generated new correlation ID: {}", correlationId);
            } else {
                log.debug("Using correlation ID from request: {}", correlationId);
            }

            // Store in MDC for logging
            MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

            // Add to response header
            httpResponse.setHeader(CORRELATION_ID_HEADER, correlationId);

            // Continue filter chain
            chain.doFilter(request, response);
            
        } finally {
            // Always clear MDC to avoid memory leaks
            MDC.remove(CORRELATION_ID_MDC_KEY);
        }
    }
}
