# Deployment

Backend on Render, database on Neon, frontend on Vercel — all three have
genuinely free tiers with **no credit card required**. This is a runbook for
the account-creation/dashboard steps you do yourself — Claude can prepare
code but can't create accounts or enter payment details on your behalf.

## Why this combination

Render's free web services have no persistent disk, so a database living in
the *same* free container would lose all its data on every restart —
that's why the database is a separate service (Neon), not something baked
into the backend's own container. Neon's free Postgres is a permanent tier
(not a trial) that self-heals: it scales to zero after 5 minutes idle and
wakes itself automatically on the next query, no dashboard visit required.
Render's free web service does the same thing — spins down after 15 minutes
with no traffic, 30–60s cold start on the next request. Neither is
production-grade, both are fine for a personal/portfolio project.

Render gives you HTTPS automatically on its own `*.onrender.com` domain, so
unlike a self-managed VM, there's no reverse proxy or certificate handling
to set up.

## 1. Database: Neon

1. Sign up at [neon.tech](https://neon.tech) — no card required.
2. Create a project (any name; pick a region close to where Render will run).
3. From the project dashboard, copy the **connection string**. It looks like:
   ```
   postgresql://<user>:<password>@<endpoint>.neon.tech/<dbname>?sslmode=require
   ```
4. Translate that into three values you'll need in step 2:
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<endpoint>.neon.tech/<dbname>?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `<user>`
   - `SPRING_DATASOURCE_PASSWORD` = `<password>`

## 2. Backend: Render

1. Sign up at [render.com](https://render.com) — no card required.
2. **New → Web Service**, connect the `Rives-estate` GitHub repo.
3. Render should auto-detect the root `Dockerfile` (environment: **Docker**).
   If asked, leave the Dockerfile path as `./Dockerfile` and the build
   context as the repo root.
4. Instance type: **Free**.
5. Environment tab — add these (see `.env.example` for the full list):
   - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
     `SPRING_DATASOURCE_PASSWORD` — from step 1
   - `JWT_SECRET_KEY` — `openssl rand -base64 32`
   - `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
   - `SPRING_JPA_HIBERNATE_DDL_AUTO` = `update` — **required**. Spring Boot's
     default is `none` for a non-embedded database, which boots cleanly but
     creates zero tables — every request then 500s with no obvious startup
     error. `application.properties` sets this locally, but it's git-ignored
     so it never reaches Render's build; this env var is what makes it apply.
   - `APP_FRONTEND_ORIGIN` — leave as `http://localhost:5173` for now,
     you'll come back and fix this in step 4
   - `SERVER_PORT` = `10000` — **required**. Render always injects its own
     `PORT` env var (default `10000`) and expects the app to bind to it, but
     Spring Boot's environment-variable binding only recognizes the name
     `SERVER_PORT` (which it auto-maps to the `server.port` property), not a
     bare `PORT`. Without this, the app starts fine but Render can never
     reach it, so health checks fail. `application.properties` isn't part of
     this at all — it's git-ignored, so it never reaches Render's build;
     this env var is the only thing that makes the port match.
6. Deploy. Render assigns a URL like `https://rives-estate.onrender.com` —
   that's your backend's public HTTPS URL, no further setup needed.
7. Verify: `curl https://your-service.onrender.com/properties/all` should
   return `[]` (or your existing properties).

## 3. Frontend: Vercel

1. Sign up at [vercel.com](https://vercel.com) — no card required for the
   free tier.
2. Import the `rives-frontend` GitHub repo.
3. **Root directory**: `rives-frontend` — the actual project lives one
   level below the repo root (`Rives-frontend/rives-frontend/` locally).
4. Environment variable: `VITE_API_BASE_URL` = your Render URL from step 2
   (no trailing slash).
5. Deploy. Vercel gives you `https://<project>.vercel.app`.

## 4. Close the loop: point the backend at the real frontend origin

Back in Render's dashboard → Environment: set `APP_FRONTEND_ORIGIN` to the
Vercel URL from step 3, save (Render redeploys automatically on env var
changes). Without this, CORS and the WebSocket handshake reject the deployed
frontend.

## 5. Verify

- Sign up, log in, browse properties, create a listing, upload an image,
  message a dealer, refresh the page mid-session (access-token refresh).
- Check the browser's Network tab: the `/ws` WebSocket connection should
  show `101 Switching Protocols`, not fail.
- First request after either service has been idle will be slow (cold
  start) — that's expected on the free tier, not a bug.
- Render's dashboard has a live log tab if anything 500s.

## Updating

Push to the branch Render/Vercel are watching — both auto-deploy on push, no
manual redeploy step needed.

## Local development

`docker-compose.yml` (Postgres only, or Postgres + backend together) is
unrelated to this deployment path and still works for local dev exactly as
before — see the main `README.md`.
