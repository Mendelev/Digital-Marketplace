package com.marketplace.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Security configuration for password encoding.
 */
@Configuration
public class SecurityConfig {

    /**
     * BCrypt password encoder with strength factor 12.
     * Provides strong password hashing with computational cost suitable for authentication.
     *
     * @return configured BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
