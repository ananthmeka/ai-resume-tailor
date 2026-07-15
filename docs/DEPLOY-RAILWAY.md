# Deploy AI Resume Tailor — Railway (API) + Vercel (UI) + Groq

End state:

```
User → Vercel (React) → Railway (Spring Boot) → Groq (Llama 3.3 70B)
              ↑ X-API-Key (beta)      ↑ APP_API_KEY + rate limits
```

---

## Prerequisites

1. [GitHub](https://github.com) repo with this project pushed  
2. [Railway](https://railway.app) account  
3. [Vercel](https://vercel.com) account  
4. [Groq](https://console.groq.com/keys) API key (`gsk_...`)  
5. Generate a **beta API key** (long random string) for clients, e.g. `openssl rand -hex 32`

---

## Step 1 — Deploy API on Railway

1. **New Project** → **Deploy from GitHub** → select your repo.  
2. **Service settings** → **Root Directory**: `project-1/ai-resume-tailor/backend`  
3. Railway detects `Dockerfile` and `railway.toml` (health check `/api/health`).  
4. **Variables** (required):

| Variable | Example | Purpose |
|----------|---------|---------|
| `SPRING_PROFILES_ACTIVE` | `railway,groq` | Production + Groq LLM |
| `GROQ_API_KEY` | `gsk_...` | LLM (server only) |
| `APP_API_KEY` | `(openssl rand -hex 32)` | Clients must send this |
| `APP_API_KEY_ENABLED` | `true` | Enforce key (set by `railway` profile) |
| `APP_CORS_ORIGINS` | `https://your-app.vercel.app,https://*.vercel.app` | Browser CORS |
| `APP_RATE_LIMIT_PER_MINUTE` | `30` | Global per IP |
| `APP_TAILOR_LIMIT_PER_HOUR` | `10` | Resume generations per IP/hour |

5. Deploy → copy **public URL**, e.g. `https://ai-resume-tailor-production.up.railway.app`

6. Verify:

```bash
curl -s https://YOUR-RAILWAY-URL/api/health
curl -s https://YOUR-RAILWAY-URL/api/llm-status
curl -s -o /dev/null -w "%{http_code}\n" -X POST https://YOUR-RAILWAY-URL/api/tailor
# Expect 401 without key
curl -s -H "X-API-Key: YOUR_APP_API_KEY" ...
```

---

## Step 2 — Deploy UI on Vercel

1. **Import** Git repo → **Root Directory**: `project-1/ai-resume-tailor/frontend`  
2. **Environment variables** (Production):

| Variable | Value |
|----------|--------|
| `VITE_API_BASE_URL` | `https://YOUR-RAILWAY-URL` (no trailing slash) |
| `VITE_API_KEY` | Same value as Railway `APP_API_KEY` |

3. Deploy → open Vercel URL → test upload + JD + Generate.

**Note:** `VITE_API_KEY` is embedded in the JS bundle. Acceptable for **closed beta** with rate limits; for public launch use a **Cloudflare Worker** or **Vercel serverless proxy** so the key stays server-side.

---

## Step 3 — Local dev (unchanged)

```bash
# No API key required locally (APP_API_KEY unset, api-key-enabled false)
./scripts/run-laptop-api.sh          # Ollama
# or
./scripts/run-cloud-api.sh groq      # test Groq from laptop
```

Frontend: leave `VITE_API_BASE_URL` unset; Vite proxies to `:8080`.

---

## Step 4 — Optional: test Docker locally

```bash
cd project-1/ai-resume-tailor/backend
docker build -t ai-resume-tailor .
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=railway,groq \
  -e GROQ_API_KEY=gsk_... \
  -e APP_API_KEY=test-key \
  -e APP_CORS_ORIGINS=http://localhost:5173 \
  ai-resume-tailor
```

---

## Step 5 — Switch LLM quality tier

| Profiles | LLM |
|----------|-----|
| `railway,groq` | Free/fast open model |
| `railway` + default OpenAI env | `OPENAI_API_KEY`, `OPENAI_MODEL=gpt-4o-mini` |

Change `SPRING_PROFILES_ACTIVE` on Railway and redeploy.

---

## Security checklist (public beta)

- [x] `APP_API_KEY` required on Railway (`railway` profile)  
- [x] Per-IP rate limits (`RateLimitFilter`)  
- [x] Groq key only on server  
- [ ] Rotate `APP_API_KEY` if leaked (update Vercel too)  
- [ ] Custom domain + HTTPS (Railway/Vercel default)  
- [ ] Privacy policy URL live (`/privacy.html`)

See also [CLOUD-LLM-OPTIONS.md](CLOUD-LLM-OPTIONS.md), [DEPLOY-LAPTOP-LLM.md](DEPLOY-LAPTOP-LLM.md).
