package com.marketplace.shipping.filter;

import com.marketplace.shipping.security.AuthenticatedUser;
import com.marketplace.shipping.security.JwtValidationService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Filter to validate JWT and set authenticated user in request attributes.
 */
@Component
public class JwtValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtValidationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    public static final String AUTHENTICATED_USER_ATTRIBUTE = "authenticatedUser";

    private final JwtValidationService jwtValidationService;

    public JwtValidationFilter(JwtValidationService jwtValidationService) {
        this.jwtValidationService = jwtValidationService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length());

            try {
                Claims claims = jwtValidationService.validateToken(token);

                UUID userId = jwtValidationService.extractUserId(claims);
                String email = jwtValidationService.extractEmail(claims);
                List<String> roles = jwtValidationService.extractRoles(claims);

                AuthenticatedUser authenticatedUser = new AuthenticatedUser(userId, email, roles);
                request.setAttribute(AUTHENTICATED_USER_ATTRIBUTE, authenticatedUser);

                log.debug("JWT validated successfully for user: {}", userId);

            } catch (Exception e) {
                log.warn("JWT validation failed: {}", e.getMessage());
                // Don't block the request - let controller handle authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}
