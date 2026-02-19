# Study Room Booking System — Shared Authentication Module (Spring Modulith + Spring Security + OAuth2 JWT)

This branch adds a **shared authentication/identity module** to a University Study Room Booking System.

It introduces a `useraccount` Spring Modulith module that exposes a **stable identity contract** (`useraccount::identity`) and integrates:

- **Spring Security (Resource Server)**
- **OAuth2 / OpenID Connect (Keycloak)**
- **JWT-based authentication**
- **Role + ownership authorization rules**
- **Spring Modulith boundary enforcement** for security APIs

The goal is to enforce consistent authorization rules across bounded contexts **without scattering security checks across controllers**.

---

## Tech stack (security-focused)

- Java 21 + Spring Boot
- Spring Security (OAuth2 Resource Server)
- Keycloak (OIDC provider / Identity Provider)
- JWT (signed access tokens)
- Spring Modulith (named interfaces + module boundary verification)
- PostgreSQL (persistence)

---

## New business requirement (this branch)

The system must support **authenticated users** and enforce **authorization rules** consistently.

### 1) Authentication requirement
All non-doc endpoints require a valid JWT access token issued by Keycloak.

- Requests must include:
  - `Authorization: Bearer <access_token>`
- Backend validates:
  - token signature
  - issuer
  - token expiry
  - roles/authorities (after conversion)

### 2) Authorization requirement
The system must enforce **who can do what** based on:

- **Staff role**: `STAFF`
- **Booking ownership**: only booking owner (creator) or staff can mutate bookings

Rules:
- Staff-only room catalog operations:
  - `POST /rooms`
  - `PUT /rooms/**`
  - `DELETE /rooms/**`
- Booking ownership enforcement:
  - booking creator is persisted as `bookedByUserId` (derived from JWT claim `sub`)
  - only booking owner or staff can:
    - cancel booking
    - check-in booking

### 3) Modulith requirement (shared contract)
Authentication/authorization must be exposed to other modules only through the named interface:

- `com.mykulle.booking.system.useraccount::identity`

Other modules may depend on `::identity`, but must not access security internals directly.

<img width="407" height="669" alt="components-_size_24_RoomBookingSystemApplication__size_" src="https://github.com/user-attachments/assets/f530842e-d5c2-4ef5-9c36-35e2da4ce089" />

---

## Concepts (plain English)

### Spring Security (Resource Server)
Your backend is a **resource server**: it hosts protected resources (rooms, bookings).  
It does not store passwords. Instead, it trusts tokens issued by Keycloak.

### OAuth2 / OpenID Connect (OIDC)
Keycloak is the **identity provider** (IdP).  
OAuth2/OIDC defines how a user/client obtains tokens, and how APIs validate them.

### JWT (JSON Web Token)
A JWT is a signed token containing claims like:

- `sub` → stable user identifier (used as `bookedByUserId`)
- `email`, `given_name`, `family_name`
- roles (realm/client roles)

Because it is signed, the backend can verify it without calling Keycloak for every request.

---

## How the authentication flow works (end-to-end)

### 1) User logs in to Keycloak
The user authenticates with Keycloak (via browser login, password grant for local testing, etc.).

### 2) Keycloak issues an access token (JWT)
Keycloak returns an `access_token` (JWT). This token is the “passport” for API calls.

### 3) Client calls backend with Bearer token
Client includes:

`Authorization: Bearer <access_token>`

### 4) Spring Security validates the token
Spring Security (resource server) checks:
- issuer matches configured `issuer-uri`
- signature matches Keycloak public keys
- token is not expired

### 5) JWT roles are converted to Spring authorities
A converter (Keycloak-specific) reads token claims like:
- `realm_access.roles`
- `resource_access.{clientId}.roles`
- scopes

and transforms them into Spring authorities such as:
- `ROLE_STAFF`

### 6) Application layer enforces authorization rules
Authorization decisions happen at the **application service boundary**, not scattered in controllers.

- `CatalogManagement` calls `AuthorizationService.requireStaff()`
- `BookingManagement` calls `AuthorizationService.requireOwnerOrStaff(ownerId)`

### 7) Allowed → proceed, Denied → 401/403
- Missing/invalid token → **401 Unauthorized**
- Valid token but insufficient permissions → **403 Forbidden**

---

## Module design (why a shared auth module exists)

### `useraccount` module (shared)
Exports `useraccount::identity` as the only allowed dependency surface.

Public contract includes:
- `UserAccount` (identity DTO)
- `CurrentUserProvider` (retrieve current authenticated user)
- `AuthorizationService` (requireStaff / requireOwnerOrStaff / hasRole)

Internal implementation includes:
- Security filter chain (resource server configuration)
- JWT converter (Keycloak → Spring authorities)
- Security-context-based providers

### Consuming modules
- `catalog` depends on `useraccount::identity`
- `reservation` depends on `useraccount::identity`

This prevents other modules from directly importing Spring Security internals and keeps security policy consistent.

---

## Local setup and run (backend)

### Prerequisites
- Java 21
- Maven 3.9+
- Docker Desktop (with Docker Compose)

### 1) Start infrastructure (PostgreSQL + Keycloak)
From project root:

```bash
docker compose up -d
```

This starts:
- PostgreSQL on `localhost:5432`
- Keycloak on `http://localhost:8083`

Keycloak Admin Console:
- URL: `http://localhost:8083/admin`
- Username: `admin`
- Password: `admin`

### 1.1) Realm import (automatic)
The compose setup mounts `keycloak-realm/` into Keycloak import path and runs with `--import-realm`.

Included realm file:
- `keycloak-realm/room-booking-backend-realm.json`

Imported defaults:
- Realm: `room-booking-backend`
- Client: `room-booking-backend` (public OIDC, direct access grants enabled)
- Realm role: `STAFF`
- Users:
  - `staff1` / `staff1_password` (has `STAFF`)
  - `student1` / `student1_password`
  - `student2` / `student2_password`

If changes do not apply:
1. Delete realm `room-booking-backend` in Keycloak.
2. Restart Keycloak container.

### 2) Run backend

```bash
mvn spring-boot:run
```

Backend base URL: `http://localhost:8080`
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 3) Run tests

```bash
mvn test
```

Spring Modulith docs/diagrams are generated during normal test execution at:
- `target/spring-modulith-docs`

### 4) Stop infrastructure

```bash
docker compose down
```
