# Deployment

Backend + database run together on one VM via Docker Compose. Frontend
deploys separately as a static site (Vercel or similar). This is a runbook
for account-creation/dashboard steps you do yourself — Claude can prepare
code but can't create accounts or enter payment details on your behalf.

## Why a VM, not a PaaS, for the backend

Most free-tier PaaS web services (Render, etc.) don't give free tiers a
persistent disk — a database living in the same container would lose all
its data on every restart. A real VM's disk is just... a real disk, so
Postgres and the backend can share one host and actually keep their data.
[Oracle Cloud's Always Free tier](https://www.oracle.com/cloud/free/) is a
solid choice: genuinely free forever (not a trial), a real Arm VM (up to 4
OCPUs / 24GB RAM split across instances), full Docker support. A card is
required for identity verification but nothing is charged while you stay
within Always Free limits. Any VM with Docker works identically, though —
these steps aren't Oracle-specific.

## 1. Provision a VM

Whichever provider you use, you need:

- A VM with **Docker and Docker Compose installed** (`docker --version`,
  `docker compose version` should both work over SSH).
- A firewall/security-list rule opening **ports 22 (SSH), 80, and 443**.
  Do **not** open port 5432 (Postgres) or, once Caddy is running, 8081 —
  neither needs to be reachable from the internet; the backend and Postgres
  talk to each other over Docker's internal network.
- A **domain name pointing at the VM's IP** (an A record). TLS is not
  optional here: the app's auth cookies are `Secure` (see
  `AuthController`), so the browser won't send them back over plain HTTP —
  login/refresh/logout simply won't work without HTTPS. A free subdomain
  (e.g. from a dynamic-DNS provider) works fine if you don't own a domain.

## 2. Deploy the backend + database

SSH into the VM:

```bash
git clone https://github.com/sengarsumit/Rives-estate.git
cd Rives-estate
cp .env.example .env
```

Edit `.env`: set `DB_PASSWORD`, `JWT_SECRET_KEY` (`openssl rand -base64 32`),
your Cloudinary credentials, `DOMAIN` (the one you pointed at this VM), and
leave `APP_FRONTEND_ORIGIN` for now — you'll come back to it after step 3.

```bash
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

This starts three containers: `db` (Postgres, data persisted in a named
volume), `backend` (built from the root `Dockerfile`), and `caddy` (reverse
proxy, gets a Let's Encrypt certificate for `DOMAIN` automatically). First
startup takes a minute or two while Caddy issues the certificate.

Verify: `curl https://your-domain/properties/all` should return `[]` (or
your existing properties) over HTTPS.

## 3. Deploy the frontend

On [Vercel](https://vercel.com) (or Netlify — the repo's `vercel.json` is
Vercel-specific, but the equivalent is a one-line redirect rule on any
static host):

1. Import the `rives-frontend` GitHub repo.
2. **Root directory**: `rives-frontend` — the actual project lives one
   level below the repo root (`Rives-frontend/rives-frontend/` locally).
3. Environment variable: `VITE_API_BASE_URL` = `https://your-domain` (the
   backend's HTTPS URL from step 2, no trailing slash).
4. Deploy. Vercel gives you a `https://<project>.vercel.app` URL (or attach
   your own domain).

## 4. Close the loop: point the backend at the real frontend origin

Back on the VM:

```bash
# edit .env: set APP_FRONTEND_ORIGIN to the Vercel URL from step 3
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build backend
```

Without this, CORS and the WebSocket handshake will reject the deployed
frontend (defaults to `http://localhost:5173`, which only matches local dev).

## 5. Verify

- Sign up, log in, browse properties, create a listing, upload an image,
  message a dealer, refresh the page mid-session (access-token refresh).
- Check the browser's Network tab: the `/ws` WebSocket connection should
  show `101 Switching Protocols`, not fail.
- `docker compose logs -f backend` on the VM if anything 500s.

## Updating

```bash
git pull
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build backend
```

Postgres data survives (`docker compose down`, not `down -v`, if you ever
need to stop the stack — `-v` deletes the volume and the data with it).
