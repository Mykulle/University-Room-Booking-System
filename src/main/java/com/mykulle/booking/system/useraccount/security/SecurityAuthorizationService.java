package com.mykulle.booking.system.useraccount.security;

import com.mykulle.booking.system.useraccount.api.AuthorizationService;
import com.mykulle.booking.system.useraccount.api.CurrentUserProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityAuthorizationService implements AuthorizationService {

    private final CurrentUserProvider currentUserProvider;
    private final boolean securityEnabled;

    public SecurityAuthorizationService(
            CurrentUserProvider currentUserProvider,
            @Value("${app.security.enabled:false}") boolean securityEnabled
    ) {
        this.currentUserProvider = currentUserProvider;
        this.securityEnabled = securityEnabled;
    }

    @Override
    public boolean hasRole(String role) {
        if (!securityEnabled) {
            return false;
        }

        if (role == null || role.isBlank()) {
            return false;
        }

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || authentication instanceof AnonymousAuthenticationToken
                || !authentication.isAuthenticated()) {
            return false;
        }

        var roleAuthority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return authentication.getAuthorities()
                .stream()
                .map(grantedAuthority -> grantedAuthority.getAuthority())
                .anyMatch(authority -> authority.equalsIgnoreCase(roleAuthority) || authority.equalsIgnoreCase(role));
    }

    @Override
    public void requireStaff() {
        if (!securityEnabled) {
            return;
        }

        if (!hasRole("STAFF")) {
            throw new AccessDeniedException("Staff role required");
        }
    }

    @Override
    public void requireOwnerOrStaff(String ownerUserId) {
        if (!securityEnabled) {
            return;
        }

        if (hasRole("STAFF")) {
            return;
        }

        var currentUserId = currentUserProvider.currentUser().subject();
        if (ownerUserId == null || !ownerUserId.equals(currentUserId)) {
            throw new AccessDeniedException("Access denied");
        }
    }
}
