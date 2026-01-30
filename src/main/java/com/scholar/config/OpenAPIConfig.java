package com.scholar.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger documentation configuration.
 */
@Configuration
public class OpenAPIConfig {

    @Value("${server.port:9090}")
    private int serverPort;

    @Bean
    public OpenAPI scholarOpenAPI() {
        String securitySchemeName = "bearerAuth";
        Server devServer = new Server();
        devServer.setUrl("http://localhost:" + serverPort + "/api");
        devServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setName("Scholar Team");
        contact.setEmail("support@scholar.com");

        License license = new License()
            .name("Proprietary")
            .url("https://scholar.com/license");

        Info info = new Info()
            .title("Scholar Backend API")
            .version("1.0.0")
            .description("Enterprise-grade CV matching and outreach platform API. " +
                        "Provides comprehensive endpoints for CV management, keyword-based professor matching, " +
                        "and automated email campaigns with multi-tenant support.")
            .contact(contact)
            .license(license);

        return new OpenAPI()
            .info(info)
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                )
            )
            .servers(List.of(devServer));
    }
}
