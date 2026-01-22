# Building a University Study Room Booking System using Domain-Driven Design and Spring Modulith

This repo is my **learning playground** for building a real-ish campus system using:

- **Spring Boot (backend)**
- **Spring Security + JWT**
- **Spring Modulith (modular monolith boundaries)**
- **DDD-style domain modeling (bounded contexts, aggregates, value objects)**
- **React + TypeScript (frontend)**

The app is intentionally small enough to finish, but messy enough to be interesting.

---

## Why this project exists

University study rooms are scarce. The failure modes are predictable:

- People book “just in case” and never show up (`NO_SHOW`).
- A few power users keep booking and block everyone (`HOARDING`).
- Staff end up being the enforcement layer (`MANUAL POLING`).
- Conflicts happen because the system doesn’t answer: *who is allowed here right now?* (`DISPUTES`).

This project tries to simulate one simple idea inforced in my univerisity **check-in**.

If you don’t check in within a grace period (example: **15 minutes after start**), the booking becomes `NO_SHOW`, the room is freed, and your no-show counter increases (which can lead to a temporary ban).

This is the whole “business value” in one sentence:
> Increase room utilization without human interference.

---

## The business problem (in plain terms)

1. Students book study rooms in fixed increments (ex: 30 minutes).
2. Bookings must respect:
   - blackout windows (maintenance/exams)
   - no overlaps
   - max duration and quotas
   - booking windows (ex: only book up to a week ahead)
3. When the time starts, the booking enters a **check-in required** phase.
4. If the student checks in before the deadline → booking is valid.
5. If not → it becomes a **no-show**, the room becomes available again, and penalties may apply.
6. Admins can override/cancel bookings and manage blackouts and user bans.
7. Everything important should leave a trail (audit logs).

---

## Booking lifecycle (the “rules engine” part)

A booking lifecycle follows these stages/statuses:

- `CONFIRMED`
- `CHECK_IN_REQUIRED`
- `CHECKED_IN`
- `COMPLETED`
- `CANCELLED`
- `NO_SHOW`

*Example transitions*:

- `CONFIRMED → CHECK_IN_REQUIRED` at start time  
- `CHECK_IN_REQUIRED → CHECKED_IN` when check-in succeeds  
- `CHECK_IN_REQUIRED → NO_SHOW` when deadline passes  
- `CONFIRMED → CANCELLED` before start time (policy decides if “late cancel” matters)  
- `CHECKED_IN → COMPLETED` when end time passes  

Hard rules (examples):

- You **cannot check in** unless status is `CHECK_IN_REQUIRED`.
- You **cannot cancel** after end time.
- Admin cancel **requires a reason**.

---

## Why DDD here (and not “just CRUD”)

This project is not hard because of controllers.
It’s hard because of **rules** and **time** and **concurrency**:

- overlapping intervals must be correct
- policies must be enforced consistently
- time-based transitions must happen predictably
- two users trying to book the same room at the same time must not create two bookings

DDD helps because it forces the application to put rules where they belong:

- in the **domain model**
- behind **aggregates** (not random `if` statements in controllers)
- using **value objects** (like a `TimeRange`) that make invalid states hard to represent

The goal is “boring correctness”.

---

## Why Spring Modulith (modular monolith)

Spring Modulith framework is used to build a modular monolith architecture, bringing the best of both worlds, development velocity of a `monolith`, with the scalability, security,
and fault-tolerance of `microservices`: 

- modules map closely to bounded contexts
- dependencies between modules are explicit
- the codebase becomes self-documenting 
- extraction later is possible, but not mandatory

---

## Bounded contexts 
<img width="1920" height="1080" alt="Borrow" src="https://github.com/user-attachments/assets/5caf9f59-7e9a-494d-9a66-5a8938b117bc" />

### `identity` — users, roles, bans
- roles: `STUDENT`, `STAFF`

- ban/suspension status
- JWT integration

### `rooms` — room catalog + blackout windows
- room metadata (location, capacity, type, equipment)
- blackouts / maintenance periods

### `booking` — booking aggregate + conflict checks + policies
- create/cancel bookings
- enforce quotas, alignment, booking window
- prevent overlaps

### `checkin` — check-in validation + no-show processing
- validate booking code/token during check-in window
- mark no-show when deadline passes
- trigger penalty increments

### `shared` — small shared kernel (keep tiny)
- `UserId`, `RoomId`, `TimeRange`, etc.
- shared primitives, not shared business logic

---

## Repository structure

