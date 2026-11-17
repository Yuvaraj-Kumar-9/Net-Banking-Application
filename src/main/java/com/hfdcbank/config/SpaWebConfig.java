package com.hfdcbank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the production React build from {@code classpath:/static}.
 *
 * <p>The Docker image (and the optional {@code -Pfrontend} Maven profile) copies
 * {@code frontend/dist} into the backend jar, so the API and the UI share one
 * origin. That keeps the HttpOnly JWT cookie first-party and avoids CORS in
 * production.</p>
 *
 * <p>Client-side routes such as {@code /dashboard} do not exist as files, so
 * they fall back to {@code index.html}. API, Swagger and error paths never fall
 * back: they keep their normal JSON 404 response.</p>
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String resourcePath, Resource location) throws IOException {
                        Resource requested = location.createRelative(resourcePath);
                        if (requested.exists() && requested.isReadable()) {
                            return requested;
                        }

                        // Never mask a missing API endpoint or tooling asset.
                        String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
                        if (path.startsWith("api/") || path.startsWith("v3/api-docs")
                                || path.startsWith("swagger-ui") || path.startsWith("error")
                                || path.contains(".")) {
                            return null;
                        }

                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
