# Railway build failed — checklist

## 1. Root Directory (most common)

**Service** → **Settings** → **Source** → **Add Root Directory**:

```text
backend
```

If Root Directory stays empty, Railway uses **Railpack at repo root** and fails with:
`Railpack could not determine how to build the app`.

**Alternative:** leave Root Directory empty and push the **repo-root** `Dockerfile` + `railway.toml` (builds `backend/` via Docker). Prefer **Root Directory = backend** when possible.

After changing root, **Redeploy** (old logs may still show Railpack).

---

## 2. Read the real error

**Service** → **Deployments** → click failed deploy → **View logs**.

| Log message | Fix |
|-------------|-----|
| `Dockerfile not found` | Set Root Directory = `backend` |
| `mvnw: not found` / wrapper error | Pull latest `backend/Dockerfile` (uses Maven image, no mvnw) |
| `failed to solve` / network | Retry deploy; Railway transient issue |
| Build OK but crash on start | Check **Variables**: `GROQ_API_KEY`, `SPRING_PROFILES_ACTIVE=railway,groq` |

---

## 3. Push Dockerfile fix

After updating Dockerfile locally:

```bash
cd .../ai-resume-tailor
git add backend/Dockerfile docs/RAILWAY-BUILD-FIX.md
git commit -m "fix: Railway Docker build without mvnw wrapper"
git push origin main
```

Railway auto-redeploys on push.

---

## 4. Verify

```bash
curl -s https://YOUR-RAILWAY-DOMAIN/api/health
```
