package com.foodflow.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of("http://localhost:5173"));
        // ↑ Explicitly allow the React dev server origin.
        //   In production this would be your actual domain.

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        // ↑ OPTIONS is critical — browsers send a preflight OPTIONS request
        //   before POST/PATCH/DELETE to check if CORS is allowed.
        //   Without OPTIONS here, every non-GET request fails.

        config.setAllowedHeaders(List.of("*"));
        // ↑ Allow all headers including Authorization, Content-Type etc.

        config.setAllowCredentials(true);
        // ↑ Required if you ever use cookies. Safe to include.

        config.setMaxAge(3600L);
        // ↑ Browser caches the preflight response for 1 hour.
        //   Reduces preflight requests on repeated API calls.

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // ↑ Apply this CORS config to ALL routes — auth, orders, restaurants, ws.

        return new CorsWebFilter(source);
    }
}