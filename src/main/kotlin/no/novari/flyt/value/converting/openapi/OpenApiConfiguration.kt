package no.novari.flyt.value.converting.openapi

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import no.novari.flyt.webresourceserver.UrlPaths.INTERNAL_API
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class OpenApiConfiguration {
    @Bean
    fun valueConvertingOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("FINT Flyt Value Converting API")
                    .description("Internt API for administrasjon av verdikonverteringer.")
                    .version("1.0"),
            ).components(
                Components().addSecuritySchemes(
                    BEARER_AUTH,
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT"),
                ),
            ).addSecurityItem(SecurityRequirement().addList(BEARER_AUTH))

    @Bean
    fun valueConvertingGroupedOpenApi(): GroupedOpenApi =
        GroupedOpenApi
            .builder()
            .group("value-convertings")
            .pathsToMatch(
                "$INTERNAL_API/value-convertings",
                "$INTERNAL_API/value-convertings/**",
            ).build()

    @Bean
    @Order(-1)
    fun openApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain =
        http
            .securityMatcher(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs",
                "/v3/api-docs/**",
                "/v3/api-docs.yaml",
            ).authorizeHttpRequests { requests -> requests.anyRequest().permitAll() }
            .build()

    private companion object {
        const val BEARER_AUTH = "bearerAuth"
    }
}
