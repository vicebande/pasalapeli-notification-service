package com.pasalapeli.notification.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Seguridad del notification-service (defensa en profundidad; el BFF ya valida en el borde):
 *
 * - AZURE_AUTH_ENABLED=true  -> JWT de Azure AD (firma JWKS + issuer + audience + roles).
 * - AZURE_AUTH_ENABLED=false -> modo demo: identidad por cabeceras X-Dev-User-* (reenviadas por el BFF).
 *
 * Reglas:
 *  - /api/notificaciones/admin/** = solo ADMIN.
 *  - El resto de /api/** = autenticado; la propiedad (ownership) del buzon se valida
 *    en NotificacionService comparando el principal autenticado con el correo del usuario.
 *  - Actuator/health y /error = publicos (healthcheck de Docker).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthConverter jwtAuthConverter;

    @Value("${azure.activedirectory.enabled:false}")
    private boolean azureEnabled;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:https://login.microsoftonline.com/common/discovery/v2.0/keys}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:https://login.microsoftonline.com/common/v2.0}")
    private String issuerUri;

    @Value("${azure.activedirectory.client-id:00000000-0000-0000-0000-000000000000}")
    private String clientId;

    @Value("${azure.activedirectory.app-id-uri:api://00000000-0000-0000-0000-000000000000}")
    private String appIdUri;

    @Value("${app.cors.allowed-origins:*}")
    private String corsAllowedOrigins;

    @PostConstruct
    void warnIfDemoMode() {
        if (!azureEnabled) {
            log.warn("AZURE_AUTH_ENABLED=false: MODO DEMO ACTIVO — las cabeceras X-Dev-User-* son confiables. "
                    + "NO usar en produccion; establecer AZURE_AUTH_ENABLED=true.");
        }
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/notificaciones/admin/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/**").authenticated()
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/error").permitAll()
                        .anyRequest().authenticated()
                );

        if (azureEnabled) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                    .jwt(jwt -> jwt
                            .decoder(jwtDecoder())
                            .jwtAuthenticationConverter(jwtAuthConverter)
                    )
                    .authenticationEntryPoint((request, response, authException) -> {
                        log.warn("401 en Notification Service: {}", authException.getMessage());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                                "status", HttpStatus.UNAUTHORIZED.value(),
                                "error", "Unauthorized",
                                "message", "Token JWT ausente, expirado o invalido",
                                "timestamp", LocalDateTime.now().toString()
                        ));
                    })
                    .accessDeniedHandler((request, response, accessDeniedException) -> {
                        log.warn("403 en Notification Service: {}", accessDeniedException.getMessage());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        new ObjectMapper().writeValue(response.getOutputStream(), Map.of(
                                "status", HttpStatus.FORBIDDEN.value(),
                                "error", "Forbidden",
                                "message", "No tiene los permisos necesarios para este recurso",
                                "timestamp", LocalDateTime.now().toString()
                        ));
                    })
            );
        } else {
            // Modo demo: filtro no-bean, anadido solo a la cadena de seguridad
            http.addFilterBefore(new DevMockAuthFilter(azureEnabled), UsernamePasswordAuthenticationFilter.class);
        }

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        if (!azureEnabled) {
            return token -> null;
        }

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(clientId, appIdUri);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator));
        return decoder;
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();
        configuration.setAllowedOrigins(java.util.List.of(corsAllowedOrigins.split(",")));
        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(java.util.Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin",
                "Access-Control-Request-Method", "Access-Control-Request-Headers", "X-Dev-User-Role", "X-Dev-User-Email"));

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
