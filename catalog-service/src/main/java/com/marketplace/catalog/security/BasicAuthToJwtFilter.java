package com.marketplace.catalog.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Filter that converts HTTP Basic Authentication to JWT Bearer token.
 * This enables Swagger UI users to authenticate with username/password
 * instead of manually copying JWT tokens.
 */
@Component
public class BasicAuthToJwtFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;

    public BasicAuthToJwtFilter(
            RestTemplate restTemplate,
            @Value("${auth-service.base-url:http://localhost:8080}") String authServiceUrl) {
        this.restTemplate = restTemplate;
        this.authServiceUrl = authServiceUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        String authHeader = request.getHeader("Authorization");
        
        // Check if request uses Basic Auth
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                // Decode Basic Auth credentials
                String base64Credentials = authHeader.substring("Basic ".length());
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                String[] parts = credentials.split(":", 2);
                
                if (parts.length == 2) {
                    String email = parts[0];
                    String password = parts[1];
                    
                    // Call Auth Service to get JWT token
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    
                    Map<String, String> loginRequest = Map.of(
                            "email", email,
                            "password", password
                    );
                    
                    HttpEntity<Map<String, String>> httpEntity = new HttpEntity<>(loginRequest, headers);
                    
                    ResponseEntity<Map> authResponse = restTemplate.exchange(
                            authServiceUrl + "/api/v1/auth/login",
                            HttpMethod.POST,
                            httpEntity,
                            Map.class
                    );
                    
                    if (authResponse.getStatusCode() == HttpStatus.OK && authResponse.getBody() != null) {
                        String accessToken = (String) authResponse.getBody().get("accessToken");
                        
                        if (accessToken != null) {
                            // Create a wrapper that replaces Basic Auth with Bearer token
                            jakarta.servlet.http.HttpServletRequestWrapper wrappedRequest = 
                                new jakarta.servlet.http.HttpServletRequestWrapper(request) {
                                    @Override
                                    public String getHeader(String name) {
                                        if ("Authorization".equalsIgnoreCase(name)) {
                                            return "Bearer " + accessToken;
                                        }
                                        return super.getHeader(name);
                                    }
                                };
                            
                            filterChain.doFilter(wrappedRequest, response);
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                // If Basic Auth conversion fails, return 401
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"error\":\"Authentication failed\",\"message\":\"Invalid credentials\"}");
                return;
            }
        }
        
        // Continue with original request if not Basic Auth
        filterChain.doFilter(request, response);
    }
}
