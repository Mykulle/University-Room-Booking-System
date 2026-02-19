package com.mykulle.booking.system.useraccount.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtAuthenticationConverterTest {

    @Test
    void convert_extractsRealmAndClientRoles() {
        var converter = new KeycloakJwtAuthenticationConverter("room-booking-backend");
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user-123")
                .claim("preferred_username", "student1")
                .claim("scope", "profile email")
                .claim("realm_access", Map.of("roles", List.of("staff", "student")))
                .claim("resource_access", Map.of(
                        "room-booking-backend", Map.of("roles", List.of("manager"))
                ))
                .build();

        var authentication = (JwtAuthenticationToken) converter.convert(jwt);
        var authorities = authentication.getAuthorities().stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .toList();

        assertThat(authentication.getName()).isEqualTo("student1");
        assertThat(authorities).contains("ROLE_STAFF", "ROLE_STUDENT", "ROLE_MANAGER");
        assertThat(authorities).contains("SCOPE_profile", "SCOPE_email");
    }

    @Test
    void convert_fallsBackToSubjectWhenPreferredUsernameMissing() {
        var converter = new KeycloakJwtAuthenticationConverter("room-booking-backend");
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "subject-user")
                .build();

        var authentication = (JwtAuthenticationToken) converter.convert(jwt);

        assertThat(authentication.getName()).isEqualTo("subject-user");
    }
}
