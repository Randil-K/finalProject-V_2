# Community-Based Ocean & Coastal Cleanup Management System

Group 8 · Institute of Technology, University of Moratuwa.

A platform connecting citizens, volunteer divers, government authorities and organisations
around one loop for Sri Lanka's coastline: **report a polluted site → let the community
verify it → an administrator approves it → the government authority approves it and it becomes a
cleanup project owned by the reporter → clean it up and record the result.**

Volunteer divers (with certificates) and organisations (with a website link) are verified by an
administrator before they can sign in.

```
├── frontend/   React + Vite, built on the Tideline design system
└── backend/    Spring Boot 3.3 + JPA + JWT, MySQL
```

## Run both

```bash
cd frontend && npm install && npm run dev
```

```bash
cd backend && ./mvnw spring-boot:run
```

The backend needs MySQL running first — in XAMPP, Control Panel → MySQL → Start. It creates the
`tideline` database and its tables on the first run. Without a database server, start it with
`./mvnw spring-boot:run -Dspring-boot.run.profiles=h2` instead.

Frontend on `http://localhost:5173`, API on `http://localhost:8080`. The frontend reads and
writes live API data — Vite proxies `/api` to the backend, so there is no CORS setup in
development. Start the backend first; every seeded account uses `password123`, and the login
screen lists them.

See [frontend/README.md](frontend/README.md) and [backend/README.md](backend/README.md) for
details, demo accounts and the full endpoint list.

## Module coverage

| # | Module | Frontend | Backend |
| --- | --- | --- | --- |
| 1 | User registration & profile | yes | yes (certificate upload, admin account verification) |
| 2 | Pollution reporting | yes | yes (photo and video evidence upload) |
| 3 | Community verification & trust | yes | yes (75% threshold, configurable) |
| 4 | Admin review & moderation | yes | yes |
| 5 | Government approval workflow | yes | yes |
| 6 | Location-based alerts | yes | yes (Haversine radius + escalation steps) |
| 7 | Project progress monitoring | yes | yes |
| 8 | Diver employment & opportunities | yes | yes |
| 9 | Reporting & analytics | yes | yes |

## Still to do

- Geocoding for location validation, and an embedded pollution map (screens link to
  OpenStreetMap for now).
- Scheduled reminders to volunteers (module 6) and a complaints channel (module 4).
- A hosted deployment — the backend's Dockerfile is ready (see `backend/README.md`), and the
  image still needs a run against real MySQL.
