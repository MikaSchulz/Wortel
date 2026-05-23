package me.eyetealer.wortel.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class WebConfig(
    @Value("\${wortel.cors.allowed-origins:*}") private val allowedOrigins: String,
) : WebMvcConfigurer {

    override fun addCorsMappings(registry: CorsRegistry) {
        val origins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val mapping = registry.addMapping("/api/**")
            .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .exposedHeaders("Location")
            .maxAge(3600)

        if (origins.size == 1 && origins[0] == "*") {
            mapping.allowedOriginPatterns("*")
        } else {
            mapping.allowedOrigins(*origins.toTypedArray())
        }
    }
}
