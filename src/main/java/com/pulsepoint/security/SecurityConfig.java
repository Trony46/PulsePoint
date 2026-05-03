package com.pulsepoint.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Enable CORS using the configuration source below
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // Disable CSRF — APIs don't use browser cookies so CSRF doesn't apply
                .csrf(AbstractHttpConfigurer::disable)

                // Don't create HTTP sessions — every request is self-contained
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // All endpoints are permitted at the Spring Security level
                // Our ApiKeyFilter handles the actual ingest protection
                .authorizeHttpRequests(auth ->
                        auth.anyRequest().permitAll())

                // Register our filter — runs before Spring's built-in auth filter
                .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. Define the exact CORS rules
    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Allow requests from any origin (including file:///)
        config.setAllowedOriginPatterns(List.of("*"));

        // Allow any HTTP method (GET, POST, OPTIONS, PATCH, etc.)
        config.setAllowedMethods(List.of("*"));

        // Allow any header (crucial for your custom X-Api-Key header)
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Apply these rules to all endpoints in the application
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}