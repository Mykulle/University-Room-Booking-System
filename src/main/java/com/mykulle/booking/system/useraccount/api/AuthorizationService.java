package com.mykulle.booking.system.useraccount.api;

/**
 * Authorization checks exposed as shared module API for bounded contexts.
 */
public interface AuthorizationService {

    boolean hasRole(String role);

    void requireStaff();

    void requireOwnerOrStaff(String ownerUserId);
}
