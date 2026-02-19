Place exported realm JSON files in this directory for auto-import at container startup.

Example:
- `room-booking-backend-realm.json`

The Keycloak container starts with `--import-realm` and reads from:
- `/opt/keycloak/data/import`
