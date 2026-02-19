package com.mykulle.booking.system.useraccount.security;

import com.mykulle.booking.system.useraccount.api.CurrentUserProvider;
import com.mykulle.booking.system.useraccount.api.UserAccount;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    private static final UserAccount ANONYMOUS = new UserAccount("anonymous", null, null, null, List.of());

    @Override
    public UserAccount currentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(authentication)) {
            return ANONYMOUS;
        }

        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken) {
            return fromJwt(authentication, jwtAuthenticationToken.getToken());
        }

        var principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return fromJwt(authentication, jwt);
        }

        return new UserAccount(
                authentication.getName(),
                null,
                null,
                null,
                extractRoles(authentication)
        );
    }

    private static UserAccount fromJwt(Authentication authentication, Jwt jwt) {
        return new UserAccount(
                valueOrFallback(jwt.getSubject(), authentication.getName()),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getClaimAsString("email"),
                extractRoles(authentication)
        );
    }

    private static List<String> extractRoles(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .map(SecurityContextCurrentUserProvider::normalizeRole)
                .distinct()
                .toList();
    }

    private static String normalizeRole(String authority) {
        if (authority == null) {
            return "";
        }
        return authority.startsWith("ROLE_")
                ? authority.substring("ROLE_".length())
                : authority;
    }

    private static String valueOrFallback(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }

    private static boolean isAuthenticated(Authentication authentication) {
        return authentication != null
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.isAuthenticated();
    }
}
