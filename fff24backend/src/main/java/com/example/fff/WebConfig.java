package com.example.fff;





import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration class for customizing web application configurations.
 * Implements the WebMvcConfigurer interface to override and provide
 * additional configuration options for the Spring MVC.
 *
 * This specific configuration enables Cross-Origin Resource Sharing (CORS)
 * mappings globally for the application. The settings allow all origins,
 * HTTP methods (GET, POST, PUT, DELETE, OPTIONS), and headers, while exposing
 * the "Authorization" header to the client.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Configures global Cross-Origin Resource Sharing (CORS) settings for the application.
     * Overrides the default CORS settings by allowing requests from all origins,
     * permitting a set of specified HTTP methods, and enabling all headers.
     * Additionally, exposes the "Authorization" header to the client.
     *
     * @param registry the {@code CorsRegistry} used to define CORS settings for the application
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*") // Allow all headers
                .exposedHeaders("Authorization");
    }
}



