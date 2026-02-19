package com.mykulle.booking.system.useraccount.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Converts Keycloak JWT claims into Spring Security authorities.
 */
public class KeycloakJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final Converter<Jwt, Collection<GrantedAuthority>> scopeAuthorities = new JwtGrantedAuthoritiesConverter();
    private final String clientId;

    public KeycloakJwtAuthenticationConverter(String clientId) {
        this.clientId = clientId;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        var authorities = new LinkedHashSet<GrantedAuthority>();

        var fromScopes = scopeAuthorities.convert(jwt);
        if (fromScopes != null) {
            authorities.addAll(fromScopes);
        }

        authorities.addAll(extractRealmRoleAuthorities(jwt));
        authorities.addAll(extractClientRoleAuthorities(jwt));

        var principalName = Optional.ofNullable(jwt.getClaimAsString("preferred_username"))
                .filter(name -> !name.isBlank())
                .orElse(jwt.getSubject());

        return new JwtAuthenticationToken(jwt, authorities, principalName);
    }

    private Set<GrantedAuthority> extractRealmRoleAuthorities(Jwt jwt) {
        var realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null) {
            return Set.of();
        }

        return readRoleAuthorities(realmAccess.get("roles"));
    }

    private Set<GrantedAuthority> extractClientRoleAuthorities(Jwt jwt) {
        var resourceAccess = jwt.getClaimAsMap("resource_access");
        if (resourceAccess == null || clientId == null || clientId.isBlank()) {
            return Set.of();
        }

        var clientClaims = resourceAccess.get(clientId);
        if (!(clientClaims instanceof Map<?, ?> clientMap)) {
            return Set.of();
        }

        return readRoleAuthorities(clientMap.get("roles"));
    }

    private Set<GrantedAuthority> readRoleAuthorities(Object rolesClaim) {
        if (!(rolesClaim instanceof Collection<?> roles)) {
            return Set.of();
        }

        return roles.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)))
                .map(GrantedAuthority.class::cast)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }
}
