# Phase 1: Small public beta (fewer than 10 users)

This phase keeps the Spring Boot backend and React/Vite frontend. It adds installable-PWA support, individual beta access codes, persistent quotas, and provider-independent OpenAI-compatible inference.

## Architecture

Vercel PWA → Railway Spring API → OpenAI-compatible open-model endpoint. The endpoint may be Ollama, vLLM, OpenRouter, Together, or another compatible provider. H2 stores only user quota buckets on a mounted volume; resume content is not persisted.

## 1. Create beta access codes

Generate a different token for every invited user:

```bash
openssl rand -hex 24
```

Set Railway variables, using user names that contain no commas or colons:

```text
APP_BETA_ACCESS_ENABLED=true
APP_BETA_USERS=ananth:TOKEN_1,user2:TOKEN_2,user3:TOKEN_3
APP_TAILOR_LIMIT_PER_HOUR=3
APP_TAILOR_LIMIT_PER_MONTH=20
```

Removing an entry revokes that user's access after the service restarts. Never put these tokens in Vercel variables or source control.

## 2. Configure open-model inference

Use any OpenAI-compatible endpoint:

```text
OPENAI_BASE_URL=https://your-open-model-endpoint/v1
OPENAI_API_KEY=server-side-provider-key
OPENAI_MODEL=provider-model-name
OPENAI_MAX_COMPLETION_TOKENS=8192
```

For the initial beta, a paid serverless open-model endpoint is usually less expensive than keeping a GPU continuously active. To self-host later, point the same variables at vLLM without changing application code. Local development continues to use the `ollama` Spring profile.

## 3. Persist quotas on Railway

Create a Railway volume and mount it at `/app/data`, then set:

```text
APP_DATABASE_URL=jdbc:h2:file:/app/data/resume-tailor
```

Without a volume the service works, but counters reset when Railway replaces the container.

## 4. Deploy the backend

Use `backend` as the Railway root directory and set:

```text
SPRING_PROFILES_ACTIVE=railway
APP_CORS_ORIGINS=https://ai-resume-tailor-ecru-tau.vercel.app
GENERATE_INTERVIEW_QUESTIONS=false
```

Keep interview generation disabled during the beta to save one LLM call per resume. Verify `/api/health` before deploying the frontend.

## 5. Deploy the installable PWA

Set only the public backend location in Vercel:

```text
VITE_API_BASE_URL=https://YOUR-RAILWAY-SERVICE.up.railway.app
```

Remove `VITE_API_KEY`; every `VITE_*` value is visible in browser code. Users enter their code in the application. Android users can use **Install app**; iOS users use Safari → Share → **Add to Home Screen**.

## 6. Release checks

1. `/api/health` works without a token.
2. `/api/account` returns 401 without a token.
3. A valid `X-Beta-Token` returns the user and quota.
4. An invalid token cannot upload a resume.
5. Monthly quota remains after a redeploy.
6. The PWA manifest and icons load over HTTPS.
7. Resume text does not appear in application logs or the quota database.

## Phase 2 trigger

Move from H2/access codes to PostgreSQL plus email/OAuth when there are more than 10–20 users, self-registration is needed, or multiple backend instances are required. Then add asynchronous jobs, Redis rate limits, and provider routing/circuit breakers.
