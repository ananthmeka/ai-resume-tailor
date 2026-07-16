# Step-by-step: keys, Railway, Vercel, and safe Git pushes

Project root: `ai-resume-tailor/` (folder with `backend/` and `frontend/`).

---

## Part 1 — Generate keys (laptop)

### 1A. Beta API key (your app — not Groq)

Used so only your UI can call your API (`APP_API_KEY` + `VITE_API_KEY` — **same value**).

```bash
openssl rand -hex 32
```

Save the output in a password manager or local note. Example label: `AI Resume Tailor beta API key`.

You will paste this into:

- Railway → `APP_API_KEY`
- Vercel → `VITE_API_KEY`

**Do not** put this in GitHub.

### 1B. Groq LLM key (Groq’s API)

1. Open https://console.groq.com  
2. Sign in  
3. **API Keys** → **Create API Key**  
4. Copy the key (starts with `gsk_`)

Paste only into:

- Railway → `GROQ_API_KEY`  
- Local `.env` for dev (optional, gitignored)

**Do not** put `GROQ_API_KEY` in Vercel (frontend never calls Groq directly).

---

## Part 2 — Railway (backend API + Groq + APP_API_KEY)

### 2A. Connect repo

1. https://railway.app → log in  
2. **New Project** → **Deploy from GitHub repo**  
3. Select **`ananthmeka/ai-resume-tailor`** (after your push succeeds)  
4. Open the service → **Settings**  
5. **Root Directory:** `backend`  
6. Save → wait for build (Dockerfile)

### 2B. Add variables (dashboard only)

Open **Variables** → **RAW Editor** and paste (replace placeholders):

```env
SPRING_PROFILES_ACTIVE=railway,groq
GROQ_API_KEY=gsk_PASTE_FROM_GROQ_CONSOLE
APP_API_KEY=PASTE_OPENSSL_HEX_64_CHARS
APP_CORS_ORIGINS=http://localhost:5173,https://YOUR-VERCEL-APP.vercel.app,https://*.vercel.app
APP_RATE_LIMIT_PER_MINUTE=30
APP_TAILOR_LIMIT_PER_HOUR=10
```

| Variable | Source |
|----------|--------|
| `GROQ_API_KEY` | Part 1B |
| `APP_API_KEY` | Part 1A |
| `APP_CORS_ORIGINS` | Update `YOUR-VERCEL-APP` after Part 3 (can deploy Railway first with `https://*.vercel.app` only) |

Railway saves automatically and redeploys.

### 2C. Public URL (for Vercel)

1. **Settings** → **Networking** → **Generate Domain**  
2. Copy URL, e.g. `https://ai-resume-tailor-production.up.railway.app`  
3. This becomes **`VITE_API_BASE_URL`** (no trailing `/`)

### 2D. Verify Railway

```bash
export API=https://YOUR-RAILWAY-DOMAIN.up.railway.app
curl -s "$API/api/health"
curl -s "$API/api/llm-status"
```

---

## Part 3 — Vercel (frontend + link to Railway)

### 3A. Import project

1. https://vercel.com → log in with GitHub  
2. **Add New…** → **Project** → import **`ai-resume-tailor`**  
3. **Root Directory:** `frontend`  
4. Framework: **Vite** (auto)

### 3B. Environment variables (Production)

**Settings → Environment Variables** (before or after first deploy):

| Key | Value | Environments |
|-----|--------|--------------|
| `VITE_API_BASE_URL` | `https://YOUR-RAILWAY-DOMAIN.up.railway.app` | Production |
| `VITE_API_KEY` | Same as Railway `APP_API_KEY` (Part 1A) | Production |

Important: changing `VITE_*` requires **Redeploy** (vars are baked at build time).

### 3C. Deploy and copy site URL

After deploy, copy e.g. `https://ai-resume-tailor.vercel.app`.

### 3D. Update Railway CORS

Railway → **Variables** → set:

```env
APP_CORS_ORIGINS=http://localhost:5173,https://ai-resume-tailor.vercel.app,https://*.vercel.app
```

Use your real Vercel hostname. Wait for redeploy.

### 3E. Test in browser

1. Open Vercel URL  
2. Upload resume + paste JD → **Generate**  
3. If CORS/401 errors, re-check `APP_CORS_ORIGINS`, `VITE_API_KEY` = `APP_API_KEY`, and redeploy Vercel.

---

## Part 4 — Local `.env` (optional, never GitHub)

Copy from [ENV-TEMPLATE.md](ENV-TEMPLATE.md):

```bash
# project root — gitignored
touch .env
# edit: GROQ_API_KEY, SPRING_PROFILES_ACTIVE=groq, etc.

# frontend — only if testing against Railway from npm run dev
touch frontend/.env
# edit: VITE_API_BASE_URL, VITE_API_KEY
```

Production values for live site stay in **Railway + Vercel dashboards**, not in committed files.

---

## Part 5 — Before every `git push`

### 5A. Manual check (30 seconds)

```bash
cd /path/to/ai-resume-tailor
git status
```

- Must **not** show `.env` or `frontend/.env` under “Changes to be committed”  
- If they appear: `git reset HEAD .env frontend/.env`

```bash
git ls-files --cached | grep -E '^\.env$|^frontend/\.env$|\.env\.example'
```

**No output** = good (examples removed from repo).

### 5B. Install Git hooks (automatic block)

Once per machine / clone:

```bash
cd /path/to/ai-resume-tailor
chmod +x scripts/install-git-hooks.sh
./scripts/install-git-hooks.sh
```

This sets `core.hooksPath=.githooks`:

| Hook | When | What |
|------|------|------|
| **pre-commit** | `git commit` | Rejects if `.env` / `frontend/.env` staged or `gsk_...` in diff |
| **pre-push** | `git push` | Rejects if `.env` paths in commits being pushed |

Test pre-commit (optional):

```bash
git add .env 2>/dev/null; git commit -m "test" || true
git reset HEAD .env 2>/dev/null
# Should fail with ERROR: Refusing to commit forbidden env files
```

---

## Quick reference

| Secret | Generated how | Railway | Vercel | Git |
|--------|----------------|---------|--------|-----|
| `GROQ_API_KEY` | Groq console | Yes | No | Never |
| `APP_API_KEY` | `openssl rand -hex 32` | Yes | No | Never |
| `VITE_API_KEY` | Same as `APP_API_KEY` | No | Yes | Never |
| `VITE_API_BASE_URL` | Railway public domain | No | Yes | Never |

See also [SECRETS-POLICY.md](SECRETS-POLICY.md), [DEPLOY-RAILWAY.md](DEPLOY-RAILWAY.md), [ENV-GUIDE.md](ENV-GUIDE.md).
