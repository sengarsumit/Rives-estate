# Rives Estate — Backend API

A Spring Boot REST API for a real-estate rental platform: property listings with
image uploads, JWT-cookie authentication, and role-based access for renters
(`USER`) and property managers (`DEALER`), with an `ADMIN` role for user
administration.

## 🚀 Features

- **Auth** — signup, signin, refresh, logout, and a `/me` endpoint. JWTs are
  issued as **httpOnly cookies** (a short-lived access token and a longer-lived
  refresh token), not `Authorization` headers.
- **Role-based access** — enforced on the backend with method-level
  `@PreAuthorize`. Dealers can only modify their own properties.
- **Property management** — dealers create, update, delete and list their own
  properties; anyone signed in can browse and search.
- **Search** — locality search with pagination and validated sorting.
- **Images** — dealers upload multiple images per property (validated and stored
  on Cloudinary); metadata is tracked in a `PropertyImage` table, and images are
  removed from Cloudinary when their property is deleted.
- **Consistent errors** — a global handler returns a uniform JSON error body with
  correct HTTP status codes; passwords and tokens are never serialized.

## 🗄️ Tech Stack

| Technology | Purpose |
| --- | --- |
| Spring Boot 3.5 | Core framework |
| Spring Security + JJWT | Cookie-based JWT auth & authorization |
| Spring Data JPA / Hibernate | Persistence (ORM) |
| PostgreSQL | Relational storage |
| Cloudinary | Image storage & delivery |
| ModelMapper | Entity ↔ DTO mapping |
| Lombok | Boilerplate reduction |
| Maven | Build & dependency management |

## 🛠️ Getting Started

### Prerequisites

- **Java 24** — the build targets Java 24 (`<java.version>24</java.version>` in
  `pom.xml`). On an earlier JDK you can still build/test by overriding the
  release, e.g. `./mvnw test -Dmaven.compiler.release=21`.
- **PostgreSQL** running locally, with a database created:
  `CREATE DATABASE estate;` — or skip installing it and use `docker compose up db`
  (see [Docker](#-docker) below).
- **Maven** (or use the bundled `./mvnw` wrapper)
- **Cloudinary account** (for image uploads)

### 1. Clone

```bash
git clone https://github.com/sengarsumit/Rives-estate.git
cd Rives-estate
```

### 2. Configure

Create `src/main/resources/application.properties` (git-ignored — never commit
real credentials). The application reads these keys:

```properties
spring.application.name=Rives-estate

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/estate
spring.datasource.username=your_db_user
spring.datasource.password=your_db_password
server.port=8081
spring.jpa.hibernate.ddl-auto=update

# JWT signing key (HMAC secret, at least 256 bits / 32 characters for HS256)
jwt.secret.key=your_long_random_secret

# Cloudinary
cloudinary.cloud-name=your_cloud_name
cloudinary.api-key=your_api_key
cloudinary.api-secret=your_api_secret

# CORS + WebSocket allowed origin - wherever the frontend is served from
app.frontend-origin=http://localhost:5173
```

`ddl-auto=update` means Hibernate creates/updates the schema from the entities on
startup — no manual migrations.

### 3. Run

```bash
./mvnw spring-boot:run        # starts on http://localhost:8081
./mvnw clean install          # build
./mvnw test                   # run the test suite
```

## 🐳 Docker

A root `Dockerfile` (multi-stage: Maven build, then a slim JRE runtime) and
`docker-compose.yml` (Postgres + the backend) let you skip installing Postgres
and Java locally:

```bash
cp .env.example .env    # fill in real values - .env is git-ignored
docker compose up       # Postgres on :5432, backend on :8081
```

`docker compose up db` starts just Postgres, if you'd rather run the backend
itself with `./mvnw spring-boot:run` for faster iteration. See `.env.example`
for every variable the compose file expects (DB credentials, JWT secret,
Cloudinary credentials, and the frontend's origin for CORS/WebSocket).

## 🔐 Authentication model

- `signin` sets two `httpOnly`, `secure`, `SameSite=Lax` cookies: `accessToken`
  (15 min) and `refreshToken` (7 days). The browser sends them automatically; the
  frontend uses `withCredentials: true` and never reads the token values.
- Because cookies are `secure(true)`, auth flows require HTTPS (browsers exempt
  `localhost` in most cases).
- Protected requests are authenticated by an `accessToken` **cookie** (not a
  header). When the access token expires, call `POST /api/auth/refresh`.
- **CORS** is locked to the Vite dev origin `http://localhost:5173` with
  credentials allowed — keep this in sync with wherever the frontend is served.

## 🔑 API Endpoints

Base URL: `http://localhost:8081`

### Auth — `/api/auth`

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/signup` | Public | Register a `USER` or `DEALER` (`ADMIN` is rejected) → `201` |
| POST | `/api/auth/signin` | Public | Authenticate; sets `accessToken` + `refreshToken` cookies → `200` |
| POST | `/api/auth/refresh` | Public (refresh cookie) | Issue a new `accessToken` cookie → `200` |
| POST | `/api/auth/logout` | Public | Clear both auth cookies → `200` |
| GET | `/api/auth/me` | Authenticated | Current user (`id, username, email, role`) → `200` |

### Properties — `/properties`

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| POST | `/properties/create` | DEALER | Create a listing (`title`, `address` required) → `201`; duplicate title → `409` |
| GET | `/properties/all` | Authenticated | List all properties |
| GET | `/properties/{id}` | Authenticated | Get one property → `404` if missing |
| GET | `/properties/mine` | DEALER | The caller's own listings |
| GET | `/properties/search/locality` | Authenticated | Paginated locality search (see params below) |
| PATCH | `/properties/{id}` | DEALER (owner) | Partial update → `403` non-owner, `404` missing |
| DELETE | `/properties/delete/{id}` | DEALER (owner) | Delete (also removes its Cloudinary images) |
| POST | `/properties/{propertyId}/upload-images` | DEALER (owner) | Upload images (`multipart/form-data`) |

**Search query params** (`/properties/search/locality`):

| Param | Default | Notes |
| --- | --- | --- |
| `locality` | — | Case-insensitive `contains` match; blank returns all |
| `page` | `0` | Zero-based page index |
| `size` | `10` | Page size |
| `sortBy` | `title` | One of `title`, `rental`, `locality` (anything else → `400`) |
| `sortDir` | `desc` | `asc` or `desc` |

Returns a Spring `Page` of `PropertyResponseDTO` (`content`, `totalPages`,
`totalElements`, `number`, `size`, `first`, `last`).

### Users — `/api/v1/users/`

| Method | Endpoint | Access | Description |
| --- | --- | --- | --- |
| GET | `/api/v1/users/all` | ADMIN | List all users |
| PATCH | `/api/v1/users/{username}` | Self or ADMIN | Update account (role changes are ADMIN-only) |
| DELETE | `/api/v1/users/{username}` | Self or ADMIN | Delete account |

## 🖼️ Image Upload

- `Content-Type: multipart/form-data`, one or more parts named **`images`**.
- Validated on the server: at most **10 files**, **≤ 5 MB** each, and content type
  must be **JPEG, PNG, WebP or GIF** (checked by MIME type, not file extension).
  Violations return `400`.
- The Cloudinary `publicId` is stored per image so it can be deleted later.

## 🗃️ Data Model

- **User** — `id` (UUID), `username` (unique), `email` (unique), `password`
  (BCrypt, never serialized), `firstName`, `lastName`, `phone`, `role`
  (`USER` / `DEALER` / `ADMIN`)
- **Property** — `id` (UUID), `title` (unique), `description`, `address`,
  `locality`, `rental`, `dealer` (`@ManyToOne` → User), `images`
  (`@OneToMany` → PropertyImage)
- **PropertyImage** — `id` (UUID), `imageUrl`, `publicId`, `property`
  (`@ManyToOne` → Property)

## 🧪 Testing

```bash
./mvnw test
```

Most tests are web-layer slices (`@WebMvcTest`) and service unit tests that need
no database; repository tests use an in-memory H2 database. A few
`@SpringBootTest` integration tests (context load, method-security enforcement,
the auth refresh-token flow) run against a real Postgres instance, as
configured in `application.properties`.

## ✉️ Contact

Sumit Sengar · Bangalore, India
