# Self-host backend (replace Railway) — open-source options

Keep **Vercel** for the React UI (or self-host that too). Replace **only** the Spring Boot API.

Your app already ships as **Docker** (`backend/Dockerfile` + `docker-entrypoint.sh`).

---

## Option comparison

| Option | Open source | Best for | HTTPS | Effort |
|--------|-------------|----------|-------|--------|
| **[Coolify](https://coolify.io)** | Yes (self-host) | Closest to Railway/Vercel for Docker + Git | Built-in (Let’s Encrypt) | Medium |
| **[Dokku](https://dokku.com)** | Yes | Single VPS, `git push` deploy | Plugins | Medium |
| **[CapRover](https://caprover.com)** | Yes | Docker Swarm on one server | Built-in | Medium |
| **Docker Compose** (this repo) | Yes | Your VPS or home server | You (Caddy/nginx) | Low–medium |
| **Laptop + Ollama + tunnel** | N/A | Demos only | Cloudflare tunnel | Low (see `docs/DEPLOY-LAPTOP-LLM.md`) |

**Recommended:** **Coolify** on a small VPS (~$5–6/mo for the **machine**, not Railway) **or** **Docker Compose + Caddy** if you want minimal moving parts.

Groq/OpenRouter keys stay **server env vars** — same as Railway.

---

## A. Docker Compose on a VPS (minimal OSS)

### A1. Server

- Ubuntu 22.04/24.04 VPS (Hetzner, DigitalOcean, Oracle free tier, etc.)
- Install Docker: https://docs.docker.com/engine/install/ubuntu/

### A2. Deploy files on server

Clone repo (or copy `backend/` + compose file):

```bash
git clone https://github.com/ananthmeka/ai-resume-tailor.git
cd ai-resume-tailor/backend
```

Create **`backend/.env`** on the server (never commit):

```env
PORT=8080
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_...
APP_API_KEY=<openssl rand -hex 32>
APP_CORS_ORIGINS=https://ai-resume-tailor-ecru-tau.vercel.app,https://*.vercel.app,http://localhost:5173
GENERATE_INTERVIEW_QUESTIONS=false
LLM_PAUSE_MS=65000
OPENAI_EXTRACT_MODEL=llama-3.1-8b-instant
OPENAI_MAX_COMPLETION_TOKENS=3072
JAVA_TOOL_OPTIONS=-Xmx768m -XX:+UseContainerSupport
```

### A3. Run

From repo root (where `docker-compose.selfhost.yml` lives):

```bash
docker compose -f docker-compose.selfhost.yml up -d --build
curl -s http://127.0.0.1:8080/api/health
```

### A4. Public HTTPS (Caddy example)

Point `api.yourdomain.com` A-record to VPS IP. Caddy reverse proxy to `localhost:8080` with automatic TLS.

### A5. Vercel

**Settings → Environment Variables:**

- `VITE_API_BASE_URL` = `https://api.yourdomain.com`
- `VITE_API_KEY` = same as `APP_API_KEY`

**Redeploy** Vercel.

### Checks

| Check | Command / action |
|-------|------------------|
| Health | `curl -s https://api.yourdomain.com/api/health` |
| LLM | `curl -s .../api/llm-status -H "X-API-Key: ..."` |
| UI | Vercel → Generate |

---

## B. Coolify (Railway-like UI, OSS)

1. Install Coolify on VPS: https://coolify.io/docs/get-started/installation  
2. **New Resource → Application → GitHub** → repo `ai-resume-tailor`  
3. **Build pack:** Dockerfile  
4. **Root / context:** `backend` (or Dockerfile path `backend/Dockerfile`)  
5. **Port:** `8080` (container); map public HTTPS in Coolify  
6. Paste same env vars as Railway (section A2) in Coolify **Environment**  
7. Deploy → copy HTTPS URL → update Vercel `VITE_API_BASE_URL` → redeploy Vercel  

---

## C. Turn off Railway

1. Find project (**upbeat-happiness** or **refreshing-unity**) with domain `ai-resume-tailor-production-8518.up.railway.app`  
2. Remove custom domain or delete service after new host works  
3. Avoid paying Railway if you migrate fully  

---

## D. Groq limits (unchanged)

Self-hosting **does not** fix Groq 12k TPM. Still use:

- `LLM_PAUSE_MS=65000`, 8b extract model, and/or  
- `LLM_FALLBACK_*` OpenRouter vars  

---

## Your URLs after migration

| Role | Was (Railway) | Becomes |
|------|---------------|---------|
| API | `https://ai-resume-tailor-production-8518.up.railway.app` | `https://api.yourdomain.com` or Coolify URL |
| UI | https://ai-resume-tailor-ecru-tau.vercel.app | Same (update env only) |
