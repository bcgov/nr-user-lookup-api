package ca.bc.gov.nrs.userlookup.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration. Declares a bearer-token (JWT) security
 * scheme so the Swagger UI "Authorize" dialog accepts a Keycloak access token.
 */
@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "User Lookup API",
        version = "1.0",
        description = "BCeID / IDIR lookup proxy. All endpoints require a "
            + "Keycloak bearer token carrying the relevant scope."))
@SecurityScheme(
    name = "bearerAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT")
public class OpenApiConfig {
}
