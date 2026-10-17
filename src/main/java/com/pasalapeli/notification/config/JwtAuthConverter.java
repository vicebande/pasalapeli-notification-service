package com.pasalapeli.notification.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Convierte claims de roles de Azure AD (roles, scp) en GrantedAuthority de Spring Security.
 * Identico al del BFF para que los roles coincidan en todos los servicios.
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter defaultGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        String principalClaimValue = jwt.getClaimAsString("preferred_username");
        if (principalClaimValue == null) {
            principalClaimValue = jwt.getClaimAsString("email");
        }
        if (principalClaimValue == null) {
            principalClaimValue = jwt.getSubject();
        }
        return new JwtAuthenticationToken(jwt, authorities, principalClaimValue);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        Set<GrantedAuthority> authorities = new HashSet<>(defaultGrantedAuthoritiesConverter.convert(jwt));

        List<String> roles = jwt.getClaimAsStringList("roles");
        if (roles != null) {
            authorities.addAll(roles.stream()
                    .map(role -> role.startsWith("ROLE_") ? new SimpleGrantedAuthority(role) : new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toSet()));
        }

        String scp = jwt.getClaimAsString("scp");
        if (scp != null) {
            for (String scope : scp.split(" ")) {
                authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
            }
        }

        authorities.add(new SimpleGrantedAuthority("ROLE_CLIENTE"));

        return authorities;
    }
}
