package com.pasalapeli.notification.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * Validador de Audience (aud) del JWT, identico al del BFF:
 * verifica que el token este emitido para nuestra API.
 */
@Slf4j
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    private final String expectedAudience;
    private final String expectedAppIdUri;

    public AudienceValidator(String expectedAudience, String expectedAppIdUri) {
        this.expectedAudience = expectedAudience;
        this.expectedAppIdUri = expectedAppIdUri;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        List<String> audiences = jwt.getAudience();

        if (audiences != null && (audiences.contains(expectedAudience) || audiences.contains(expectedAppIdUri))) {
            return OAuth2TokenValidatorResult.success();
        }

        log.warn("Rechazo de token JWT: Audience invalido. Esperado '{}' o '{}', recibido: {}",
                expectedAudience, expectedAppIdUri, audiences);

        OAuth2Error error = new OAuth2Error("invalid_token", "El Audience (aud) del JWT no coincide con el recurso autorizado", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
