# Waste2Value — Circular Waste Marketplace Platform

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.3-blue.svg)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-5.6-blue.svg)](https://www.typescriptlang.org/)
[![Tailwind CSS](https://img.shields.io/badge/Tailwind-3.4-38bdf8.svg)](https://tailwindcss.com/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Flyway](https://img.shields.io/badge/Flyway-10.x-red.svg)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker-Ready-2496ed.svg)](https://www.docker.com/)

**Waste2Value** is a production-style circular waste-management marketplace that connects:

**Waste Generators → Certified Collectors → Material Processors → Sustainable Value**

The platform connects commercial and industrial waste generators with verified logistics collectors and industrial recyclers/processors, ensuring complete traceability, transparent financial settlements, concurrent allocation safety, and participant trust.

---

## 1. System Architecture & Circular Flow

```
                      ┌────────────────────────────────────────┐
                      │            WASTE GENERATOR             │
                      │  Creates DRAFT ──► Publishes to POSTED │
                      └───────────────────┬────────────────────┘
                                          │
                                          │ 1. Listing Browse & Request Pickup
                                          ▼
                      ┌────────────────────────────────────────┐
                      │          CERTIFIED COLLECTOR           │
                      │  Submits Pickup Bid with Proposed Rate │
                      └───────────────────┬────────────────────┘
                                          │
                                          │ 2. Safe Allocation (Pessimistic Write Lock)
                                          │    Listing: POSTED ──► ALLOCATED
                                          │    Competing bids auto-rejected
                                          ▼
                      ┌────────────────────────────────────────┐
                      │         COLLECTION & COMPLETION        │
                      │  Records actual weighed quantity       │
                      │  Listing: ALLOCATED ──► COMPLETED      │
                      │  Transaction automatically logged      │
                      └───────────────────┬────────────────────┘
                                          │
                                          │ 3. Material Drop-off & Facility Intake
                                          ▼
                      ┌────────────────────────────────────────┐
                      │          INDUSTRIAL PROCESSOR          │
                      │  Validates category capability limits  │
                      │  Enforces max capacity (currentLoadKg) │
                      │  Records Material Receipt & Intake     │
                      └───────────────────┬────────────────────┘
                                          │
                                          ▼
                      ┌────────────────────────────────────────┐
                      │           TRUST & SETTLEMENT           │
                      │  • Persisted Reviews (1-5★)            │
                      │  • Dynamic Avg Rating Updates          │
                      │  • In-App Real-time Notifications      │
                      │  • Admin Dispute Mediation & Resolves  │
                      └────────────────────────────────────────┘
```

---

## 2. Technology Stack

### Backend
- **Java 21 (LTS)** with **Spring Boot 3.3.5**
- **Spring Data JPA** & **Hibernate 6** with Pessimistic Concurrency Locking and Optimistic Versioning (`@Version`)
- **Spring Security 6** with Stateless JWT Authentication and Method Security (`@PreAuthorize`)
- **Token Security:** Refresh token rotation with SHA-256 MySQL hashing and whole-family reuse revocation
- **Database:** MySQL 8 (`InnoDB`, `utf8mb4_unicode_ci`)
- **Database Migrations:** Flyway (`V1__init_schema.sql`, `V2__seed_data.sql`, `V3__phase2_schema.sql`, `V4__phase2_seed_data.sql`)
- **Documentation:** Springdoc OpenAPI 3 / Swagger UI (`/swagger-ui.html`)
- **Testing:** JUnit 5, Mockito, Spring Test (49 unit and integration tests passing)

### Frontend
- **React 18.3+** with **Vite 6**
- **TypeScript 5.6**
- **Tailwind CSS 3.4** (clean, classic marketplace design, responsive mobile drawer)
- **Axios** with Bearer token injection and automatic 401 refresh token rotation with request queueing
- **React Router v6** with role-based route protection (`ProtectedRoute`)

### Infrastructure
- **Docker & Docker Compose** (MySQL 8, Multi-stage Java 21 build, Nginx Alpine SPA web server)
- **Nginx** reverse proxying `/api/` to backend service

---

## 3. Core Domains & Features

### 3.1 Collector Management
- Profile setup: Vehicle specification/class, service area coverage, maximum operating radius (km).
- Admin verification workflow: Only verified collectors can submit pickup requests.
- Metrics dashboard: Available market lots, active requests, accepted pickups, total collected kg, transaction values, and average rating.

### 3.2 Processor Management & Capabilities
- Facility profile: Registered trade/environmental license number, plant description, maximum storage capacity (kg), current load (kg).
- Capability Declaration: Define accepted categories, minimum batch threshold, maximum batch threshold, and offered purchase price per unit.
- Material Intake Receipts: Records incoming drops from collectors, validates category capability, enforces strict facility capacity limits (`currentLoadKg + quantity <= maxCapacityKg`), and updates load transactionally.

### 3.3 Pickup Workflow & Safe Allocation
- Status lifecycle: `REQUESTED` ➔ `ACCEPTED` ➔ `REJECTED` ➔ `CANCELLED` ➔ `COMPLETED`.
- **Concurrency & Allocation Safety:** When a generator accepts a pickup request, a pessimistic write lock is acquired on the listing. The listing transitions from `POSTED` to `ALLOCATED`, the winning bid becomes `ACCEPTED`, and all other competing bids on that listing are automatically updated to `REJECTED` with system notifications.
- **Pickup Completion:** Allocated collector records actual collected weight and completion notes. Transitions both request and listing to `COMPLETED`, increments collector's total pickups, and generates a financial `Transaction` record.

### 3.4 Financial Transactions & Calculations
- Server-side calculation using `BigDecimal` (`totalAmount = actualQuantity * agreedPrice`).
- Strict decimal scale and rounding; no floating-point arithmetic.
- Prevents duplicate transaction records for the same completed transfer.

### 3.5 Ratings & Reviews
- Eligible participants of completed pickups can rate each other (1 to 5 stars) with qualitative feedback.
- Self-reviews are strictly forbidden. Duplicate reviews for the same event are rejected.
- Persisted reviews dynamically update the target collector's or processor's `avgRating`.

### 3.6 In-App Notifications
- Persistent notification model: Real-time REST polling for unread alerts.
- Notifies users on pickup bids, accepted allocations, completions, new reviews, deliveries, and dispute updates.
- Interactive notification bell in Navbar with unread counter and "Mark all as read".

### 3.7 Disputes & Reports
- Allows users to report issues against listings, pickups, transactions, or user accounts.
- Target existence is validated against the database.
- Administrator moderation: review incident description and update status (`RESOLVED`, `DISMISSED`, `UNDER_REVIEW`) with administrative audit notes.

---

## 4. API Endpoints Reference

### Authentication & Users
- `POST /api/v1/auth/register` — Public registration (ADMIN blocked)
- `POST /api/v1/auth/login` — Returns access token, refresh token, user details
- `POST /api/v1/auth/refresh` — Refresh token rotation with family reuse protection
- `POST /api/v1/auth/logout` — Revokes refresh token
- `GET /api/v1/users/me` — Current profile
- `PUT /api/v1/users/me` — Update personal profile

### Collectors
- `POST /api/v1/collectors/me` — Create/update current collector profile (COLLECTOR)
- `GET /api/v1/collectors/me` — Get current collector profile (COLLECTOR)
- `GET /api/v1/collectors` — Public listing of verified collectors
- `GET /api/v1/collectors/{id}` — Public collector details
- `GET /api/v1/admin/collectors/pending` — Unverified collector queue (ADMIN)
- `POST /api/v1/admin/collectors/{id}/verify` — Approve/suspend collector (ADMIN)

### Processors & Capabilities
- `POST /api/v1/processors/me` — Create/update current processor profile (PROCESSOR)
- `GET /api/v1/processors/me` — Get current processor profile (PROCESSOR)
- `GET /api/v1/processors` — Public listing of verified processors
- `GET /api/v1/processors/me/capabilities` — List current capabilities (PROCESSOR)
- `POST /api/v1/processors/me/capabilities` — Declare accepted category & rates (PROCESSOR)
- `PUT /api/v1/processors/me/capabilities/{id}` — Update capability (PROCESSOR)
- `DELETE /api/v1/processors/me/capabilities/{id}` — Delete capability (PROCESSOR)
- `PATCH /api/v1/processors/me/capabilities/{id}/active` — Toggle capability active state (PROCESSOR)
- `GET /api/v1/processors/capabilities/category/{catId}` — Find facilities accepting category (Public)
- `GET /api/v1/admin/processors/pending` — Unverified processor queue (ADMIN)
- `POST /api/v1/admin/processors/{id}/verify` — Approve/suspend processor (ADMIN)

### Pickup Requests
- `POST /api/v1/listings/{listingId}/pickup-requests` — Submit pickup bid (COLLECTOR)
- `GET /api/v1/listings/{listingId}/pickup-requests` — List bids on listing (Owner/ADMIN)
- `GET /api/v1/pickup-requests/my` — Collector's submitted requests (COLLECTOR)
- `GET /api/v1/pickup-requests/received` — Generator's received requests (GENERATOR)
- `GET /api/v1/pickup-requests/{id}` — Detail view (Authorized parties/ADMIN)
- `POST /api/v1/pickup-requests/{id}/respond` — Accept/decline bid (GENERATOR)
- `POST /api/v1/pickup-requests/{id}/cancel` — Cancel pickup request (COLLECTOR)
- `POST /api/v1/pickup-requests/{id}/complete` — Record actual weight & complete (COLLECTOR)
- `GET /api/v1/pickup-requests` — Platform audit overview (ADMIN)

### Material Receipts
- `POST /api/v1/processor-receipts` — Record incoming delivery (PROCESSOR)
- `GET /api/v1/processor-receipts/my` — List facility intake receipts (PROCESSOR)
- `GET /api/v1/processor-receipts/deliveries` — List drops by collector (COLLECTOR)
- `GET /api/v1/processor-receipts` — Overview of all material receipts (ADMIN)

### Transactions
- `GET /api/v1/transactions/my` — Transactions for current user (Generator, Collector, Processor)
- `GET /api/v1/transactions/{id}` — Specific transaction details
- `GET /api/v1/transactions` — Platform financial ledger (ADMIN)

### Reviews & Ratings
- `POST /api/v1/reviews` — Submit review for completed pickup (Authenticated participant)
- `GET /api/v1/reviews/user/{userId}` — Public rating & review summary for a user
- `GET /api/v1/reviews/my` — Reviews submitted by current user

### Notifications
- `GET /api/v1/notifications` — List notifications for current user
- `GET /api/v1/notifications/unread-count` — Count of unread notifications
- `PATCH /api/v1/notifications/{id}/read` — Mark notification read
- `PATCH /api/v1/notifications/read-all` — Mark all notifications read

### Reports & Disputes
- `POST /api/v1/reports` — Submit report on listing, pickup, transaction, or user
- `GET /api/v1/reports/my` — List submitted reports for current user
- `GET /api/v1/admin/reports` — List all dispute reports (ADMIN)
- `PATCH /api/v1/admin/reports/{id}` — Resolve or dismiss report with admin note (ADMIN)

### Dashboard Metrics
- `GET /api/v1/dashboard/generator` — Real metrics for generator
- `GET /api/v1/dashboard/collector` — Real metrics for collector
- `GET /api/v1/dashboard/processor` — Real metrics for processor
- `GET /api/v1/dashboard/admin` — Real platform-wide operational stats

---

## 5. Pre-seeded Demo Accounts

| Role | Email | Password | Pre-seeded Context |
|---|---|---|---|
| **Admin** | `admin@waste2value.com` | `Admin@123456` | Platform management, verification queues, dispute moderation |
| **Generator** | `generator@waste2value.com` | `Generator@123456` | Has multiple warehouse addresses, active PET Bottles & Cardboard listings |
| **Collector** | `collector@waste2value.com` | `Collector@123456` | Verified EV Flatbed Truck profile (60 km radius), ready to request pickups |
| **Processor** | `processor@waste2value.com` | `Processor@123456` | Verified Polymer Recovery facility with 25,000 kg capacity and active capabilities |

---

## 6. Running the Application

### Option A: Running with Docker Compose (Recommended)
```bash
docker compose up --build -d
```
- **Web Application:** `http://localhost:80` (or `http://localhost:3000` depending on port mapping)
- **Backend API:** `http://localhost:8080/api/v1`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`

### Option B: Local Development Mode

#### 1. Backend
Ensure MySQL is running locally on port 3306 with database `waste2value`.
```powershell
cd backend
$env:JAVA_HOME = "C:\Users\sysadmin\.jdks\jdk-21.0.12.1+1"
.\mvnw.cmd spring-boot:run
```

#### 2. Frontend
```powershell
cd frontend
npm install
npm run dev
```
Open `http://localhost:5173` in your browser.

#### 3. Running Backend Tests
```powershell
cd backend
$env:JAVA_HOME = "C:\Users\sysadmin\.jdks\jdk-21.0.12.1+1"
.\mvnw.cmd test -o
```
*(49 unit and integration tests covering security, state transitions, concurrent allocation, and capacity checks)*

---

## 7. Database Migrations (Flyway)
- `V1__init_schema.sql`: Core schema (users, refresh_tokens, addresses, categories, listings, listing_images).
- `V2__seed_data.sql`: Seed data for root/sub categories, demo accounts, addresses, and initial listings.
- `V3__phase2_schema.sql`: Schema for collector_profiles, processor_profiles, processor_capabilities, pickup_requests, transactions, processor_receipts, reviews, notifications, reports.
- `V4__phase2_seed_data.sql`: Verified profiles for City Collector and Circular Processor, along with active polymer capabilities.
