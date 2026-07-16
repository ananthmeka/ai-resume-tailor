# Railway 502 — "Application failed to respond"

## What 502 means

Railway’s proxy is up, but **nothing is answering on the service PORT** (app crashed, still starting, or listening on wrong port).

### Your HTTP logs: `connection refused`

When **Deploy logs** show `Started ResumeTailorApplication` and **Tomcat started on port(s): 8080**, but **HTTP Logs** show `upstreamErrors[].error: connection refused`, the edge is dialing a **different port** than Tomcat (e.g. Networking shows **8989**, app listens on **8080**).

**Fix (pick one after redeploy with `docker-entrypoint.sh`):**

1. **Variables → Raw Editor:** set `PORT` to the same number as **Settings → Networking → Port** (e.g. `PORT=8989`), redeploy.
2. Or change **Networking** target port to match what Tomcat logs (8080) — prefer (1) so you follow Railway’s assigned `PORT`.

Do **not** bake `ENV PORT=8080` in the Dockerfile; it can fight Railway’s runtime port.

---

## Step 1 — Check deployment status

**Deployments** tab:

- Latest row must be **Success / Active** (green), not Failed.
- If **Failed** or **Crashed**, open **Deploy logs** and **HTTP logs**.

---

## Step 2 — Runtime logs (most important)

**Deployments** → latest **Success** → **View logs** (runtime, not build).

Look for:

| Log | Meaning |
|-----|--------|
| `Started ResumeTailorApplication` | App started OK |
| `OutOfMemoryError` | Need more memory (Dockerfile sets `-Xmx768m`) |
| `Exception` / `Error` on startup | Fix config (see Step 3) |
| Nothing after container start | JVM crash or instant exit |

---

## Step 3 — Variables (required for stable run)

**Variables → Raw Editor:**

```env
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_...
APP_API_KEY=your_openssl_hex
APP_CORS_ORIGINS=http://localhost:5173,https://*.vercel.app
APP_RATE_LIMIT_PER_MINUTE=30
APP_TAILOR_LIMIT_PER_HOUR=10
```

Save → wait for **new deploy** → retry curl.

`APP_API_KEY` can be empty for **health** only if `APP_API_KEY` is unset and profile doesn’t force empty enabled key — still set Groq for tailor endpoints.

---

## Step 4 — Port binding

App must listen on **`0.0.0.0:$PORT`**. Railway sets `PORT` (e.g. 8989).  
`application.yml` uses `server.port=${PORT}` and `server.address=0.0.0.0`.

After code fix, push and redeploy:

```bash
git add backend/src/main/resources/application.yml backend/Dockerfile backend/railway.toml
git commit -m "fix: bind 0.0.0.0 and JVM heap for Railway"
git push origin main
```

---

## Step 5 — Healthcheck in UI (optional)

**Settings → Deploy → Healthcheck Path:** `/api/health`  
Increase timeout if app is slow to start (Tika + Spring can take 60–120s first boot).

---

## Step 6 — Test again

```bash
curl -s https://ai-resume-tailor-production-8518.up.railway.app/api/health
```

Expected: `{"status":"ok","service":"ai-resume-tailor"}`

---

## Step 7 — Settings → Deploy → Custom Start Command

Should be **empty** (default `java -jar`). If you added a custom command, remove it.

---

Paste **last 30 lines of runtime logs** if 502 continues after green deploy + variables + push.
