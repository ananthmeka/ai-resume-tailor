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

# Small public beta authentication (one random token per invited user)
APP_BETA_ACCESS_ENABLED=true
APP_BETA_USERS=ananth:replace-with-random-token,user2:replace-with-another-token
APP_TAILOR_LIMIT_PER_HOUR=3
APP_TAILOR_LIMIT_PER_MONTH=20
APP_DATABASE_URL=jdbc:h2:file:/app/data/resume-tailor

# Legacy shared API-key protection (local: leave disabled)
APP_API_KEY=
APP_API_KEY_ENABLED=false
APP_RATE_LIMIT_PER_MINUTE=30
APP_TAILOR_LIMIT_PER_HOUR=10
```

Railway production (Variables UI, not necessarily a file):

```bash
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_...
APP_BETA_ACCESS_ENABLED=true
APP_BETA_USERS=ananth:<random-token>,user2:<random-token>
APP_DATABASE_URL=jdbc:h2:file:/app/data/resume-tailor
APP_CORS_ORIGINS=https://your-app.vercel.app,https://*.vercel.app
```

---

## Frontend — copy to `frontend/.env`

Used by: `npm run dev` / Vercel build.

Local dev: leave empty (Vite proxies to `localhost:8080`).

Production / test against Railway:

```bash
VITE_API_BASE_URL=https://your-service.up.railway.app
# No API secret is compiled into the frontend. Users enter their beta code in the app.
```

See [ENV-GUIDE.md](ENV-GUIDE.md) and [DEPLOY-RAILWAY.md](DEPLOY-RAILWAY.md).
