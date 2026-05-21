package com.visco.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    final String securitySchemeName = "bearerAuth";

    return new OpenAPI()
      .info(
        new Info()
          .title("Visco SG API")
          .version("0.0.1-SNAPSHOT")
          .description("API para el sistema de gestión de inventario y compras de Visco")
          .contact(
            new Contact()
              .name("Visco Team")
              .email("support@visco.com")
          )
          .license(
            new License()
              .name("Apache 2.0")
              .url("https://www.apache.org/licenses/LICENSE-2.0")
          )
      )
      .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
      .components(
        new io.swagger.v3.oas.models.Components()
          .addSecuritySchemes(
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
