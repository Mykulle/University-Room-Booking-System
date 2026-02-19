package com.mykulle.booking.system.useraccount.security;

import com.mykulle.booking.system.useraccount.api.CurrentUserProvider;
import com.mykulle.booking.system.useraccount.api.UserAccount;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityAuthorizationServiceTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void requireOwnerOrStaff_allowsOwner() {
        var provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(new UserAccount("student-1", null, null, null, List.of("STUDENT")));

        setAuthentication("student-1", "ROLE_STUDENT");
        var service = new SecurityAuthorizationService(provider, true);

        assertThatCode(() -> service.requireOwnerOrStaff("student-1")).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrStaff_allowsStaff() {
        var provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(new UserAccount("staff-1", null, null, null, List.of("STAFF")));

        setAuthentication("staff-1", "ROLE_STAFF");
        var service = new SecurityAuthorizationService(provider, true);

        assertThatCode(() -> service.requireOwnerOrStaff("student-1")).doesNotThrowAnyException();
    }

    @Test
    void requireOwnerOrStaff_throwsForDifferentUserWithoutStaffRole() {
        var provider = mock(CurrentUserProvider.class);
        when(provider.currentUser()).thenReturn(new UserAccount("student-2", null, null, null, List.of("STUDENT")));

        setAuthentication("student-2", "ROLE_STUDENT");
        var service = new SecurityAuthorizationService(provider, true);

        assertThatThrownBy(() -> service.requireOwnerOrStaff("student-1"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    private static void setAuthentication(String principal, String authority) {
        var token = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                "n/a",
                List.of(() -> authority)
        );
        SecurityContextHolder.getContext().setAuthentication(token);
    }
}
