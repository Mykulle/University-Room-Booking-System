package com.mykulle.booking.system.useraccount.api;

import java.util.List;

/**
 * Represents a user account with relevant information such as subject, first name, last name, email, and roles.
 */
public record UserAccount(
        String subject,
        String firstName,
        String lastName,
        String email,
        List<String> roles
) {
}
