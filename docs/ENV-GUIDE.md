# Environment variables guide

Project root: `/Users/anmeka/DEMOS/PERSONAL-PROJECTS/ai-resume-tailor/ai-resume-tailor`

## Is OpenAI mandatory?

**No.** OpenAI is only used when you run the **default** profile with `OPENAI_API_KEY` set.

For **Groq** (your public beta plan):

| Where | What to set |
|--------|-------------|
| Railway / root `.env` | `SPRING_PROFILES_ACTIVE=railway,groq` (or `groq` locally) |
| Railway / root `.env` | `GROQ_API_KEY=gsk_...` from [Groq Console](https://console.groq.com/keys) |
| Railway / root `.env` | Leave **`OPENAI_API_KEY` empty** |

The code uses an OpenAI-**compatible** HTTP API; Groq provides that endpoint with Llama models.

---

## Two `.env` files (not GitHub)

| File | Who reads it | Purpose |
|------|----------------|---------|
| **`.env`** (project root) | Spring Boot | `GROQ_API_KEY`, `APP_API_KEY`, `SPRING_PROFILES_ACTIVE`, CORS |
| **`frontend/.env`** | Vite | `VITE_API_BASE_URL`, `VITE_API_KEY` |

**GitHub:** commit only `.env.example` files — **never** real `.env` with secrets.

**Railway:** paste root/backend variables in the Railway **Variables** UI.  
**Vercel:** paste `VITE_*` in **Settings → Environment Variables**.

---

## `APP_API_KEY` vs `VITE_API_KEY`

Same secret, two names:

| Name | Where | Role |
|------|--------|------|
| `APP_API_KEY` | Backend (Railway / root `.env`) | Server expects `X-API-Key` header |
| `VITE_API_KEY` | Frontend (Vercel / `frontend/.env`) | Browser sends `X-API-Key` to API |

Create once on your laptop:

```bash
openssl rand -hex 32
```

Paste the output into **both** places (same string).

There is **no** `VITE_API_KEY` in the root `.env.example` on purpose — it belongs in **`frontend/.env`**.

---

## Quick setup

### Local (no API key, Groq from laptop)

Root `.env`:

```bash
SPRING_PROFILES_ACTIVE=groq
GROQ_API_KEY=gsk_your_key
APP_API_KEY_ENABLED=false
```

`frontend/.env` — leave empty (Vite proxy to localhost).

```bash
./scripts/run-cloud-api.sh groq
cd frontend && npm run dev
```

### Local (pretend production: Railway URL + key)

`frontend/.env`:

```bash
VITE_API_BASE_URL=https://your-service.up.railway.app
VITE_API_KEY=<same as Railway APP_API_KEY>
```

---

## Where each key is created

| Variable | How |
|----------|-----|
| `APP_API_KEY` / `VITE_API_KEY` | You generate: `openssl rand -hex 32` |
| `GROQ_API_KEY` | Groq website → API Keys → Create |
| `OPENAI_API_KEY` | Only if you choose OpenAI instead of Groq |

See [DEPLOY-RAILWAY.md](DEPLOY-RAILWAY.md) for deploy order.
