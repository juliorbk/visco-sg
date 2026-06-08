package com.visco.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the OpenAPI 3.0 specification document for the Visco SG API.
 * Includes API metadata (title, version, contact, license) and a bearer-token
 * security scheme for JWT authentication in Swagger UI.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Builds the OpenAPI specification with API metadata and a bearer-token
     * security scheme for JWT. The resulting bean is consumed by Swagger UI.
     *
     * @return the configured {@link OpenAPI} instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            .info(
                new Info()
                    .title("Visco SG API")
                    .version("1.0.0")
                    .description("API para el sistema de gestión de inventario y compras de Visco")
                    .contact(new Contact().name("Visco Team").email("support@visco.com"))
                    .license(
                        new License()
                            .name("Apache 2.0")
                            .url("https://www.apache.org/licenses/LICENSE-2.0")
                    )
            )
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(
                new io.swagger.v3.oas.models.Components().addSecuritySchemes(
                    securitySchemeName,
                    new SecurityScheme()
                        .name(securitySchemeName)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Introduce el token JWT obtenido en /api/auth/login")
                )
            );
    }
}
