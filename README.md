# Building a University Study Room Booking System using Domain-Driven Design and a Modular Monolith approach

This repository contains a University Study Room Booking System built with:

- Java
- Spring Boot
- H2 (development database)
- DDD-style domain modeling (bounded contexts, aggregates, value objects)

The codebase is structured as a modular monolith using package-level boundaries today. A future branch will introduce Spring Modulith to make module boundaries explicit and enforceable.

---

## Why this project exists

University study rooms are scarce. The failure modes are predictable:

- People book "just in case" and never show up (`NO_SHOW`).
- A few power users keep booking and block everyone (`HOARDING`).
- Staff end up being the enforcement layer (`MANUAL_POLICING`).
- Conflicts happen because the system cannot answer: who is allowed here right now (`DISPUTES`).

This project focuses on one enforcement mechanism: time-limited check-in.

If a student does not check in within the allowed grace period after the booking start, the booking becomes `NO_SHOW`, the room becomes available again for that timeslot, and the system remains consistent without staff intervention.

Business value in one sentence:
Increase room utilization without human interference.

---

## The business problem (in plain terms)

1. Students book study rooms in fixed time increments.
2. A booking is not valid unless the student checks in within the allowed check-in window.
3. If check-in succeeds, the booking remains valid for the timeslot.
4. If check-in fails (deadline passes), the booking becomes a no-show and the room becomes available again.
5. Administrators manage the room catalog (add and remove rooms).
6. The system must prevent overlapping bookings and enforce booking state transitions correctly.

This is intentionally scoped to the core booking and check-in rules. Authentication and authorization are not implemented yet.

---

## Booking lifecycle (the rules engine)

A booking follows these statuses:

- `CONFIRMED`
- `CHECK_IN_REQUIRED`
- `CHECKED_IN`
- `COMPLETED`
- `CANCELLED`
- `NO_SHOW`

Example transitions:

- `CONFIRMED` -> `CHECK_IN_REQUIRED` at start time
- `CHECK_IN_REQUIRED` -> `CHECKED_IN` when check-in succeeds
- `CHECK_IN_REQUIRED` -> `NO_SHOW` when the check-in deadline passes
- `CHECKED_IN` -> `COMPLETED` when end time passes
- `CONFIRMED` -> `CANCELLED` before start time

Hard rules (examples):

- You cannot check in unless status is `CHECK_IN_REQUIRED`.
- A booking becomes `NO_SHOW` only when the check-in deadline is reached.
- Overlapping bookings must never result in two valid bookings for the same room and timeslot.

---

## Room availability model

This system treats room availability as time-based.

Current implementation uses a room status concept:

- Room status is `ACTIVE` when there is a booking in the target timeslot with status `CONFIRMED` or `CHECKED_IN`.
- Room status is `INACTIVE` otherwise.

In other words, "room status" here behaves like occupancy for a given time range, derived from bookings.

---

## Breaking the domain into subdomains

This domain can be split into subdomains based on who does what:

- Booking and check-in
    - Main actor: student
    - Core concepts: booking lifecycle, time rules, check-in enforcement, no-show handling

- Room catalog management
    - Main actor: administrator
    - Core concepts: room creation, removal, and room information

There can be more subdomains (identity, reporting, notifications, penalties). Since they are not part of the current requirements, they are intentionally out of scope.

This is a first attempt at decomposition. It may be right or wrong. The goal is to make a reasonable cut based on current understanding and improve it over time.

---

## Building the solution with bounded contexts

For each subdomain, we solve the subdomain problem by designing a bounded context. In a modular monolith, these bounded contexts are also the modules of the application.

Today, module boundaries are expressed as packages:

- `rooms` module
- `booking` module

A future branch will add Spring Modulith to enforce these boundaries and validate dependencies between modules.

---

## What Domain-Driven Design means here

Domain-Driven Design (DDD) is a way to structure software around business rules instead of around controllers and database tables.

In this project, DDD is applied in a lightweight way:

- Bounded contexts separate concerns (`rooms`, `booking`)
- Aggregates keep invariants consistent over time (booking lifecycle is a natural aggregate)
- Domain behavior lives with domain state (avoid anemic domain models)
- Repositories isolate persistence details from domain logic
- DTOs protect the domain model from leaking into the API layer

DDD matters here because the core difficulty is correctness under time and concurrency, not CRUD wiring.

---

## What a modular monolith means here

A modular monolith is:

- one deployable application
- with explicit internal module boundaries
- where modules expose small public APIs and keep internals private

You keep the operational simplicity of a monolith while still designing with boundaries. This reduces accidental coupling and makes future extraction possible if needed.

This repo currently uses package boundaries to represent modules. Spring Modulith is planned to make module boundaries explicit and testable.

---

## Bounded contexts (modules)

### `rooms` module

Responsibility: room catalog and room-level data.

Owns:

- Room entity and persistence
- Room creation and removal
- Room metadata and room state representation

Public API shape (typical):

- `RoomManagement` as the module facade
- `RoomDTO` as the outbound contract
- `RoomController` as the HTTP API

### `booking` module

Responsibility: booking lifecycle and check-in enforcement.

Owns:

- Booking entity and persistence
- Booking creation and cancellation
- Booking state transitions
- Check-in rules and no-show transitions
- Room availability views based on booking state (example DTO: `RoomAvailabilityDTO`)

Public API shape (typical):

- `BookingManagement` as the module facade
- `BookingDTO` as the outbound contract
- `BookingController` as the HTTP API

---

## Repository structure

Current package layout:

```text
src/main/java
└── com/mykulle/booking/system
    ├── RoomBookingSystemApplication
    ├── booking
    │   ├── Booking
    │   ├── BookingController
    │   ├── BookingDTO
    │   ├── BookingManagement
    │   ├── BookingMapper
    │   ├── BookingRepository
    │   └── RoomAvailabilityDTO
    └── rooms
        ├── Room
        ├── RoomController
        ├── RoomDTO
        ├── RoomManagement
        ├── RoomMapper
        └── RoomRepository
