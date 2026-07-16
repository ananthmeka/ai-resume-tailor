# Secrets policy (this repo)

## Do not upload to GitHub

Never commit, push, or include in PRs:

| File / pattern | Contains |
|----------------|----------|
| `.env` (project root) | `GROQ_API_KEY`, `APP_API_KEY`, etc. |
| `frontend/.env` | `VITE_API_KEY`, `VITE_API_BASE_URL` (when used locally) |
| Any `.env.*` copy with real values | Same |
| API keys, tokens, passwords in source | — |

`.gitignore` blocks these patterns. **There is no `.env.example` in the repo** — use [ENV-TEMPLATE.md](ENV-TEMPLATE.md) to create local files.

## Where secrets belong

| Environment | Backend | Frontend |
|-------------|---------|----------|
| Local laptop | Create `.env` from ENV-TEMPLATE (gitignored) | Create `frontend/.env` if needed (gitignored) |
| Railway | Service **Variables** UI | — |
| Vercel | — | **Environment Variables** UI |

## Before every push

```bash
git status
git ls-files --cached | grep -E '^\.env$|^frontend/\.env$' || true
```

Install hooks: `./scripts/install-git-hooks.sh` — see [SETUP-KEYS-RAILWAY-VERCEL.md](SETUP-KEYS-RAILWAY-VERCEL.md).

## If a secret was pushed

1. Rotate the key immediately (Groq, APP_API_KEY, etc.).
2. Remove from Git history (`git filter-repo` or GitHub secret scanning follow-up).
3. Update Railway/Vercel with new values.
