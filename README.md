# AI Resume Tailor (V0.1)

One master resume + job description → job-specific, ATS-friendly resume (no invented facts).

**Secrets:** Never commit `.env` files. Copy templates from [docs/ENV-TEMPLATE.md](docs/ENV-TEMPLATE.md). Full setup: **[docs/SETUP-KEYS-RAILWAY-VERCEL.md](docs/SETUP-KEYS-RAILWAY-VERCEL.md)**. See [docs/SECRETS-POLICY.md](docs/SECRETS-POLICY.md).

Install Git hooks: `./scripts/install-git-hooks.sh`

## LLM backends

| When | Doc |
|------|-----|
| Laptop + Ollama + Vercel | [docs/DEPLOY-LAPTOP-LLM.md](docs/DEPLOY-LAPTOP-LLM.md) |
| **24/7 cloud free/open models** | **[docs/CLOUD-LLM-OPTIONS.md](docs/CLOUD-LLM-OPTIONS.md)** (Groq, OpenRouter, Together) |

Cloud quick start:

```bash
export GROQ_API_KEY=gsk_...
./scripts/run-cloud-api.sh groq
```

Deploy same profile on Railway/Render; set Vercel `VITE_API_BASE_URL` to your Railway URL.

**One-click public beta:** [docs/DEPLOY-RAILWAY.md](docs/DEPLOY-RAILWAY.md) (Dockerfile + API key + rate limits + Groq).

## Laptop LLM + Vercel UI

See **[docs/DEPLOY-LAPTOP-LLM.md](docs/DEPLOY-LAPTOP-LLM.md)** — Vercel frontend + Spring Boot on your laptop + **Ollama** (Cursor/Codex are not HTTP APIs). Use Cloudflare Tunnel for HTTPS.

Quick:

```bash
ollama pull qwen2.5:7b
./scripts/run-laptop-api.sh
cloudflared tunnel --url http://localhost:8080
# Vercel env: VITE_API_BASE_URL=https://….trycloudflare.com
```

## Quick start

### Backend

```bash
cd backend
export OPENAI_API_KEY=your-key
export OPENAI_MODEL=gpt-4o-mini   # optional
./mvnw spring-boot:run
```

API: `http://localhost:8080`

### Frontend

```bash
cd frontend
npm install
npm run dev
```

UI: `http://localhost:5173` (proxies `/api` to backend)

## MVP scope

- Upload PDF/DOCX base resume
- Paste job description
- Optional target length (1 page / 2 pages / executive)
- Structured AI pipeline (parse → optimize → render)
- HTML preview, PDF download, change log
- No login, no database, no payment

## Deploy

- **Public beta (recommended):** [docs/DEPLOY-RAILWAY.md](docs/DEPLOY-RAILWAY.md) — Railway Docker + Groq + Vercel  
- Frontend: Vercel / Cloudflare Pages (`npm run build`)  
- Backend: Railway (`backend/Dockerfile`), Render, or Cloud Run  

Security env: `APP_API_KEY`, `APP_RATE_LIMIT_PER_MINUTE`, `APP_TAILOR_LIMIT_PER_HOUR`

## Cost

Dominated by OpenAI usage (~₹500–2000/month at low traffic with `gpt-4o-mini`).
