# Environment variable templates (copy locally — do not commit)

Create files on your machine only. **Do not add these files to Git.**

---

## Backend — copy to `.env` at project root

Used by: `./scripts/run-cloud-api.sh`, `./mvnw spring-boot:run`, Railway Docker.

```bash
# Groq (recommended for beta) — OpenAI not required
SPRING_PROFILES_ACTIVE=groq
GROQ_API_KEY=
OPENAI_MODEL=llama-3.3-70b-versatile

# CORS
APP_CORS_ORIGINS=http://localhost:5173,http://127.0.0.1:5173,https://your-app.vercel.app,https://*.vercel.app

# API protection (local: leave empty)
APP_API_KEY=
APP_API_KEY_ENABLED=false
APP_RATE_LIMIT_PER_MINUTE=30
APP_TAILOR_LIMIT_PER_HOUR=10
```

Railway production (Variables UI, not necessarily a file):

```bash
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_...
APP_API_KEY=<openssl rand -hex 32>
APP_CORS_ORIGINS=https://your-app.vercel.app,https://*.vercel.app
```

---

## Frontend — copy to `frontend/.env`

Used by: `npm run dev` / Vercel build.

Local dev: leave empty (Vite proxies to `localhost:8080`).

Production / test against Railway:

```bash
VITE_API_BASE_URL=https://your-service.up.railway.app
VITE_API_KEY=<same as Railway APP_API_KEY>
```

See [ENV-GUIDE.md](ENV-GUIDE.md) and [DEPLOY-RAILWAY.md](DEPLOY-RAILWAY.md).
