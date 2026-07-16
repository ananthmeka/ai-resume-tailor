# Full deploy checklist (every code or config change)

Use this **complete** list so nothing is skipped. Order matters.

---

## A. Local — code ready

| # | Action | Check |
|---|--------|--------|
| A1 | Edit code on branch `main` (or feature branch → merge to `main`) | `git status` shows intended files only |
| A2 | **Do not** commit `.env`, keys, or `frontend/.env` | `git status` has no secret files staged |
| A3 | Optional: compile backend locally | `cd backend && mvn -q -DskipTests compile` exits 0 |
| A4 | Optional: build frontend locally | `cd frontend && npm ci && npm run build` exits 0 |

---

## B. Git — push to GitHub

Repo root (inner folder with `backend/` + `frontend/`):

```bash
cd /Users/anmeka/DEMOS/PERSONAL-PROJECTS/ai-resume-tailor/ai-resume-tailor

git fetch origin
git status -sb
git log -1 --oneline HEAD
git log -1 --oneline origin/main
```

| # | Action | Check |
|---|--------|--------|
| B1 | Stage changes | `git add backend/ frontend/ docs/ Dockerfile scripts/` (adjust paths) |
| B2 | Commit | `git commit -m "your message"` — skip if nothing to commit |
| B3 | Push | `git push origin main` |

**Push checks (all must pass):**

```bash
git log -1 --oneline origin/main    # matches your local HEAD SHA
```

- Browser: https://github.com/ananthmeka/ai-resume-tailor/commits/main — latest commit message and time correct.
- Spot-check changed files on GitHub (e.g. `backend/.../LlmStatusController.java` if you fixed compile).

**If push fails:** use GitHub account that owns `ananthmeka/ai-resume-tailor` (`gh auth status` or SSH key for that account).

---

## C. Railway — backend (Spring Boot)

### C1. Source settings (one-time / verify)

| Setting | Value |
|---------|--------|
| Repository | `ananthmeka/ai-resume-tailor` |
| Branch | `main` |
| Root directory | `backend` |
| Auto deploy on push | Enabled |

**Check:** Settings → Source matches above.

### C2. Required variables (Raw Editor)

```env
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_...
APP_API_KEY=<openssl rand -hex 32>
APP_CORS_ORIGINS=http://localhost:5173,https://ai-resume-tailor-ecru-tau.vercel.app,https://*.vercel.app
PORT=8989
GENERATE_INTERVIEW_QUESTIONS=false
APP_RATE_LIMIT_PER_MINUTE=30
APP_TAILOR_LIMIT_PER_HOUR=10
```

Optional (Groq TPM / long resumes):

```env
LLM_PAUSE_MS=12000
OPENAI_MAX_COMPLETION_TOKENS=4096
```

Optional (OpenRouter when Groq hits limits):

```env
LLM_FALLBACK_ENABLED=true
LLM_FALLBACK_BASE_URL=https://openrouter.ai/api/v1
LLM_FALLBACK_API_KEY=sk-or-...
LLM_FALLBACK_MODEL=meta-llama/llama-3.3-70b-instruct
```

**Check:** Variables tab shows all keys (values masked). `PORT` matches **Settings → Networking → Port**.

### C3. Deploy

| # | Action | Check |
|---|--------|--------|
| C3a | After push, open **Deployments** | New deployment starts within ~1–2 min |
| C3b | Wait for status | **Success / Active** (not Failed / Crashed) |
| C3c | Commit SHA on deployment row | Same as `git log -1 origin/main` |
| C3d | **Deploy logs** | `Started ResumeTailorApplication` |
| C3e | **Deploy logs** | Tomcat port = your `PORT` (e.g. `8989`) |
| C3f | If **Build failed** | Read build log (e.g. Java compile error); fix, push again from section B |

### C4. Runtime API checks

Replace `YOUR_APP_API_KEY` and domain if different.

```bash
# Public — no key
curl -s https://ai-resume-tailor-production-8518.up.railway.app/api/health

# Protected
curl -s https://ai-resume-tailor-production-8518.up.railway.app/api/llm-status \
  -H "X-API-Key: YOUR_APP_API_KEY"
```

**Expected:**

| Endpoint | Expected |
|----------|----------|
| `/api/health` | `{"status":"ok","service":"ai-resume-tailor"}` |
| `/api/llm-status` | `activeProfiles` includes `railway,groq`; `fallbackConfigured` true/false matches `LLM_FALLBACK_ENABLED` |

**HTTP logs:** no `connection refused` on health after deploy.

### C5. Functional check (backend only)

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST https://ai-resume-tailor-production-8518.up.railway.app/api/tailor \
  -H "X-API-Key: YOUR_APP_API_KEY" \
  -F "resume=@/path/to/small.pdf" \
  -F "jobDescription=Test JD" \
  -F "resumeLength=TWO_PAGES"
```

Expect `200` (may take 20–60s with Groq pause/retries). `429`/error body → TPM limits or missing vars.

---

## D. Vercel — frontend (React)

### D1. Project settings (one-time / verify)

| Setting | Value |
|---------|--------|
| Repository | `ananthmeka/ai-resume-tailor` |
| Root directory | `frontend` |
| Framework | Vite |
| Production branch | `main` |

### D2. Environment variables (Production)

| Key | Value |
|-----|--------|
| `VITE_API_BASE_URL` | `https://ai-resume-tailor-production-8518.up.railway.app` (no trailing slash) |
| `VITE_API_KEY` | **Same** as Railway `APP_API_KEY` |

**Check:** Settings → Environment Variables → both set for **Production**.

### D3. Deploy

| # | Action | Check |
|---|--------|--------|
| D3a | Push to `main` triggers deploy **if** `frontend/` changed | Deployments tab shows new build |
| D3b | If **only backend** changed | Vercel redeploy **optional** for UI; API changes still need Railway only |
| D3c | If **`VITE_*` changed** | **Redeploy** required (vars baked at build time): Deployments → ⋯ → Redeploy |
| D3d | Deployment status | **Ready** |

### D4. Access / protection

| # | Action | Check |
|---|--------|--------|
| D4a | Settings → Deployment Protection | **Production = Off / None** for public beta |
| D4b | Open production URL (not dashboard deployment URL) | e.g. `https://ai-resume-tailor-ecru-tau.vercel.app` |
| D4c | Incognito window | Shows **AI Resume Tailor** form, not Vercel authenticator gate |

### D5. UI checks

| # | Check |
|---|--------|
| D5a | Footer shows correct `API: https://…railway.app` |
| D5b | Upload resume + JD → **Generate** (wait up to ~60s on free Groq) |
| D5c | No CORS error in browser DevTools → Network |
| D5d | No `Invalid or missing API key` → `VITE_API_KEY` = `APP_API_KEY` and Vercel redeployed after env change |

---

## E. When something fails — map symptom → fix

| Symptom | Section |
|---------|---------|
| Railway build Failed (Java compile) | B push fix → C3f |
| 502 / connection refused | `PORT` + Tomcat port (C2, C3e) |
| 401 on API | `APP_API_KEY` / `VITE_API_KEY` (C2, D2) |
| Groq TPM / rate limit | `LLM_PAUSE_MS`, wait 60s, `LLM_FALLBACK_*`, shorter JD (C2 optional) |
| Vercel authenticator page | D4 |
| UI old behavior after backend fix | Railway only — D3b; frontend redeploy if UI changed |

---

## F. Quick copy-paste block (after every backend fix)

```bash
cd /Users/anmeka/DEMOS/PERSONAL-PROJECTS/ai-resume-tailor/ai-resume-tailor
git add backend frontend docs
git status
git commit -m "describe change"   # if needed
git push origin main
git log -1 --oneline origin/main
curl -s https://ai-resume-tailor-production-8518.up.railway.app/api/health
curl -s https://ai-resume-tailor-production-8518.up.railway.app/api/llm-status -H "X-API-Key: YOUR_APP_API_KEY"
```

Then Railway: Deployments → Success + SHA match.  
Then Vercel: open production URL → one test **Generate**.

---

## G. Current pending changes (Groq TPM pause + retry)

If not pushed yet:

```bash
cd /Users/anmeka/DEMOS/PERSONAL-PROJECTS/ai-resume-tailor/ai-resume-tailor
git add backend/
git commit -m "fix: Groq TPM pause between LLM calls and rate-limit retry"
git push origin main
```

Railway: confirm deploy **Success**, SHA matches, health + llm-status OK.  
Vercel: **no change required** unless you edited `frontend/` — then redeploy.  
Test: Generate on Vercel URL (expect longer wait ~15–25s extra from `LLM_PAUSE_MS`).
