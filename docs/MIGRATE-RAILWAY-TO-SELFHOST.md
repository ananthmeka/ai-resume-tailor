# Complete migration: Railway → self-hosted API (Coolify or Docker Compose)

Frontend stays on **Vercel**. Only the **Spring Boot API** moves off Railway.

---

## Bookmark URLs (your project)

| What | URL |
|------|-----|
| GitHub repo | https://github.com/ananthmeka/ai-resume-tailor |
| Latest commits | https://github.com/ananthmeka/ai-resume-tailor/commits/main |
| Self-host compose file | https://github.com/ananthmeka/ai-resume-tailor/blob/main/docker-compose.selfhost.yml |
| Groq API keys | https://console.groq.com/keys |
| **Vercel production UI** | https://ai-resume-tailor-ecru-tau.vercel.app |
| Vercel preview (`main`) | https://ai-resume-tailor-git-main-ananthmeka.vercel.app |
| Vercel dashboard | https://vercel.com → project **ai-resume-tailor** |
| Vercel env vars | Vercel → **ai-resume-tailor** → **Settings** → **Environment Variables** |
| **Railway API (replace this)** | https://ai-resume-tailor-production-8518.up.railway.app |
| Railway health (old) | https://ai-resume-tailor-production-8518.up.railway.app/api/health |
| Railway dashboard | https://railway.app/dashboard |
| Coolify docs (install) | https://coolify.io/docs/get-started/installation |
| Docker Engine (Ubuntu) | https://docs.docker.com/engine/install/ubuntu/ |
| Caddy docs | https://caddyserver.com/docs/install |

After migration, **`NEW_API_BASE`** = either:

- Coolify: `https://<your-app>.<coolify-domain>` (from Coolify → Application → **FQDN**), or  
- Compose + Caddy: `https://api.yourdomain.com` (your DNS name)

No trailing slash on `VITE_API_BASE_URL`.

---

## Major step 1 — Choose host and get a VPS

### 1A. Pick a path

| Path | When to use |
|------|-------------|
| **Coolify** | You want a Railway-like UI, Git deploy, automatic HTTPS |
| **Docker Compose + Caddy** | You want minimal software on the server |

Both need a **Linux VPS** (Ubuntu 22.04 or 24.04 recommended).

### 1B. Create a VPS

Examples (any provider works):

- Hetzner: https://www.hetzner.com/cloud  
- DigitalOcean: https://www.digitalocean.com/products/droplets  
- Oracle Cloud free tier: https://www.oracle.com/cloud/free/

| Check | Action |
|-------|--------|
| OS | Ubuntu 22.04/24.04 |
| Size | ≥ 2 GB RAM (Java + Docker) |
| Firewall | Allow **22** (SSH), **80**, **443**; Coolify also needs **8000** during install (see Coolify docs) |
| SSH | `ssh root@YOUR_VPS_IP` or `ssh ubuntu@YOUR_VPS_IP` |

### 1C. Optional custom domain (recommended for Compose path)

1. Buy/use a domain at your registrar (Cloudflare, Namecheap, etc.).  
2. Create **A record**: `api` → `YOUR_VPS_IP` (proxied or DNS-only; both work with Caddy/Coolify).  
3. **`NEW_API_BASE`** = `https://api.yourdomain.com`

If you skip a domain, Coolify can still give you an HTTPS URL on its default domain.

---

## Major step 2 — Install platform on the VPS

### Path A — Coolify

| # | Action | URL / command |
|---|--------|----------------|
| A2.1 | SSH to VPS | `ssh user@YOUR_VPS_IP` |
| A2.2 | Install Coolify (follow current docs) | https://coolify.io/docs/get-started/installation |
| A2.3 | Open Coolify UI | Usually `http://YOUR_VPS_IP:8000` first login (docs may use HTTPS after setup) |
| A2.4 | Create admin account | In Coolify UI |
| A2.5 | Connect **GitHub** | Coolify → **Sources** → GitHub → authorize https://github.com |
| A2.6 | Add **Server** | Coolify → **Servers** → localhost/this VPS → **Validate** |

### Path B — Docker Compose only

| # | Action | URL / command |
|---|--------|----------------|
| B2.1 | SSH to VPS | `ssh user@YOUR_VPS_IP` |
| B2.2 | Install Docker | https://docs.docker.com/engine/install/ubuntu/ |
| B2.3 | Install Docker Compose plugin | Same Docker docs (compose v2) |
| B2.4 | Install Caddy (for public HTTPS) | https://caddyserver.com/docs/install#debian-ubuntu-raspbian |
| B2.5 | Clone repo | `git clone https://github.com/ananthmeka/ai-resume-tailor.git` |
| B2.6 | Use commit with self-host files | `cd ai-resume-tailor && git pull` (includes `611aca9+` with `docker-compose.selfhost.yml`) |

---

## Major step 3 — Set environment variables (backend)

Use the **same secrets** as Railway when possible so Vercel `VITE_API_KEY` does not need to change (only `VITE_API_BASE_URL` must change).

### 3A. Generate or reuse keys

| Variable | How to get | URL |
|----------|------------|-----|
| `GROQ_API_KEY` | Groq console → Create key | https://console.groq.com/keys |
| `APP_API_KEY` | Reuse Railway value **or** `openssl rand -hex 32` on laptop | — |

If you **regenerate** `APP_API_KEY`, you **must** update Vercel `VITE_API_KEY` to match.

### 3B. Required variables (copy into Coolify **Environment** or `backend/.env` on VPS)

```env
PORT=8080
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_PASTE_FROM_GROQ
APP_API_KEY=PASTE_64_CHAR_HEX
APP_CORS_ORIGINS=http://localhost:5173,https://ai-resume-tailor-ecru-tau.vercel.app,https://ai-resume-tailor-git-main-ananthmeka.vercel.app,https://*.vercel.app
GENERATE_INTERVIEW_QUESTIONS=false
APP_RATE_LIMIT_PER_MINUTE=30
APP_TAILOR_LIMIT_PER_HOUR=10
JAVA_TOOL_OPTIONS=-Xmx768m -XX:+UseContainerSupport
```

### 3C. Recommended (Groq TPM / quality)

```env
LLM_PAUSE_MS=65000
OPENAI_EXTRACT_MODEL=llama-3.1-8b-instant
OPENAI_MAX_COMPLETION_TOKENS=3072
```

### 3D. Optional (OpenRouter fallback)

```env
LLM_FALLBACK_ENABLED=true
LLM_FALLBACK_BASE_URL=https://openrouter.ai/api/v1
LLM_FALLBACK_API_KEY=sk-or-...
LLM_FALLBACK_MODEL=meta-llama/llama-3.3-70b-instruct
```

OpenRouter keys: https://openrouter.ai/keys

### 3E. Where to paste

| Path | Location |
|------|----------|
| Coolify | Application → **Environment Variables** → **Production** → Raw editor |
| Compose | File `ai-resume-tailor/backend/.env` on VPS (never commit) |

**Note:** Profile name `railway` only enables API-key + CORS settings; it does **not** require Railway hosting. Container listens on `PORT` (use **8080** for Compose/Coolify unless your proxy maps another port).

---

## Major step 4 — Deploy the API

### Path A — Coolify deploy

| # | Action | Details |
|---|--------|---------|
| A4.1 | New resource | Coolify → **+ New** → **Application** |
| A4.2 | Source | **Public Repository** or **Private** via GitHub → `ananthmeka/ai-resume-tailor` |
| A4.3 | Branch | `main` |
| A4.4 | Build | **Dockerfile** |
| A4.5 | Dockerfile location | `backend/Dockerfile` (base directory **`backend`**) |
| A4.6 | Port | **8080** (container exposes 8080; entrypoint uses `PORT` env) |
| A4.7 | Environment | Paste section 3B–3D |
| A4.8 | Domain | Coolify → **Domains** → e.g. `api.yourdomain.com` or use generated FQDN |
| A4.9 | Deploy | **Deploy** → wait **Running** / green |
| A4.10 | Copy public URL | This is **`NEW_API_BASE`** (no trailing `/`) |

Docs: https://coolify.io/docs/knowledge-base/docker/dockerfile/build-pack

### Path B — Docker Compose deploy

| # | Action | Command |
|---|--------|---------|
| B4.1 | Create env file | `nano ai-resume-tailor/backend/.env` (paste 3B–3D) |
| B4.2 | Build and start | `cd ai-resume-tailor && docker compose -f docker-compose.selfhost.yml up -d --build` |
| B4.3 | Local check on VPS | `curl -s http://127.0.0.1:8080/api/health` |
| B4.4 | Caddyfile | `/etc/caddy/Caddyfile`: `api.yourdomain.com { reverse_proxy localhost:8080 }` |
| B4.5 | Reload Caddy | `sudo systemctl reload caddy` |
| B4.6 | **`NEW_API_BASE`** | `https://api.yourdomain.com` |

---

## Major step 5 — Verify API (before touching Vercel)

Replace `NEW_API_BASE` and `YOUR_APP_API_KEY`.

### 5A. Health (no API key)

```bash
curl -s https://NEW_API_BASE/api/health
```

**Expected:** `{"status":"ok","service":"ai-resume-tailor"}`

Browser: open `https://NEW_API_BASE/` — may show `Invalid or missing API key` (normal).

### 5B. LLM status (API key required)

```bash
curl -s https://NEW_API_BASE/api/llm-status \
  -H "X-API-Key: YOUR_APP_API_KEY"
```

**Expected:** JSON with `activeProfiles` containing `railway` and `groq`.

### 5C. Optional tailor smoke test

```bash
curl -s -o /dev/null -w "%{http_code}\n" \
  -X POST https://NEW_API_BASE/api/tailor \
  -H "X-API-Key: YOUR_APP_API_KEY" \
  -F "resume=@/path/to/small.pdf" \
  -F "jobDescription=Short test JD" \
  -F "resumeLength=TWO_PAGES"
```

**Expected:** `200` (may take 30–90s with `LLM_PAUSE_MS`).

### 5D. Compare old vs new (parallel run)

| Endpoint | Old (Railway) | New (self-host) |
|----------|---------------|-----------------|
| Health | https://ai-resume-tailor-production-8518.up.railway.app/api/health | `https://NEW_API_BASE/api/health` |

Both should return OK before you switch Vercel.

---

## Major step 6 — Vercel (point UI at new API)

| # | Action | URL |
|---|--------|-----|
| 6.1 | Open env settings | https://vercel.com → **ai-resume-tailor** → **Settings** → **Environment Variables** |
| 6.2 | Edit `VITE_API_BASE_URL` | **Production** (and **Preview** if you use preview URLs) → value = `https://NEW_API_BASE` (**no** trailing `/`) |
| 6.3 | Verify `VITE_API_KEY` | Must equal backend `APP_API_KEY` (unchanged if you reused Railway key) |
| 6.4 | Save | Apply to Production + Preview as needed |
| 6.5 | Redeploy | **Deployments** → latest **Production** → **⋯** → **Redeploy** (required after env change) |
| 6.6 | Confirm build used new env | Deployment → **Build Logs** / open site |

**Do not** put `GROQ_API_KEY` in Vercel.

---

## Major step 7 — End-to-end test (production UI)

| # | Action | URL |
|---|--------|-----|
| 7.1 | Open app | https://ai-resume-tailor-ecru-tau.vercel.app |
| 7.2 | DevTools → Network | Upload resume + JD → **Generate** |
| 7.3 | Confirm API host | Requests go to `NEW_API_BASE`, not `*.up.railway.app` |
| 7.4 | Success | Tailored resume / scores load; no CORS errors in console |
| 7.5 | Preview (optional) | https://ai-resume-tailor-git-main-ananthmeka.vercel.app |

**If CORS error:** fix `APP_CORS_ORIGINS` on server, redeploy/restart API.  
**If 401:** `VITE_API_KEY` ≠ `APP_API_KEY` or Vercel not redeployed after env change.

---

## Major step 8 — Decommission Railway

Only after step 7 passes.

| # | Action | URL |
|---|--------|-----|
| 8.1 | Open Railway | https://railway.app/dashboard |
| 8.2 | Find service | Project (e.g. upbeat-happiness / refreshing-unity) with domain `ai-resume-tailor-production-8518.up.railway.app` |
| 8.3 | Stop billing | **Settings** → remove service or **pause** / delete project |
| 8.4 | Confirm old URL dead | https://ai-resume-tailor-production-8518.up.railway.app/api/health (should fail or 404) |
| 8.5 | Confirm prod still works | https://ai-resume-tailor-ecru-tau.vercel.app |

---

## Quick reference after migration

| Role | URL |
|------|-----|
| UI | https://ai-resume-tailor-ecru-tau.vercel.app |
| API | `https://NEW_API_BASE` |
| Git | https://github.com/ananthmeka/ai-resume-tailor |
| Groq | https://console.groq.com/keys |

See also: [SELF-HOST-BACKEND.md](SELF-HOST-BACKEND.md), [DEPLOY-FULL-CHECKLIST.md](DEPLOY-FULL-CHECKLIST.md), [SETUP-KEYS-RAILWAY-VERCEL.md](SETUP-KEYS-RAILWAY-VERCEL.md) (keys pattern unchanged; host name changes).
