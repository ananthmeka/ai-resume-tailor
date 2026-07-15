# Deploy: Vercel UI + laptop API + local LLM (Ollama)

## Can the deployed web UI use Cursor or Codex on my laptop?

**Not directly.** Cursor IDE models and Codex are **not** exposed as a stable HTTP API that your Spring Boot server can call from Vercel. They are tied to the editor/subscription, not to `POST /v1/chat/completions`.

**What works today for “LLM on my laptop”:**

| Tool | HTTP API? | Use with this project |
|------|-----------|------------------------|
| **Ollama** | Yes (`http://127.0.0.1:11434/v1`) | **Recommended** — set `SPRING_PROFILES_ACTIVE=ollama` |
| **LM Studio** | Yes (OpenAI-compatible, often `:1234/v1`) | Set `OPENAI_BASE_URL` to LM Studio URL |
| **OpenAI cloud** | Yes | Default profile; key on server |
| **Cursor / Codex** | No public API for your app | Use Ollama or cloud instead |

Your **laptop runs the API + LLM**; **Vercel only hosts static UI** that calls your API over HTTPS.

```
[Vercel: React UI]  --HTTPS-->  [Tunnel: api.example.com]  -->  [Laptop: Spring Boot :8080]
                                                                      |
                                                                      v
                                                              [Ollama :11434]
```

**Trade-offs:** API works only while your laptop is on, tunnel is running, and Ollama is up. Fine for demos and friends; not 24/7 for the public. For 24/7, move API + LLM to Railway/Render/GPU cloud later.

---

## Step 1 — Ollama on laptop

```bash
brew install ollama
ollama serve
ollama pull qwen2.5:7b
# Optional stronger (needs RAM): ollama pull qwen2.5:14b
```

Test:

```bash
curl http://127.0.0.1:11434/v1/models
```

---

## Step 2 — Run API locally (Ollama profile)

```bash
cd project-1/ai-resume-tailor
chmod +x scripts/run-laptop-api.sh
./scripts/run-laptop-api.sh
```

Verify:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/api/llm-status
```

---

## Step 3 — Expose laptop API on HTTPS (pick one)

### Option A: Cloudflare Tunnel (free, good for Vercel)

```bash
brew install cloudflared
cloudflared tunnel --url http://localhost:8080
```

Copy the `https://….trycloudflare.com` URL. That is your **API base** (no trailing slash).

**Security:** Quick tunnels are public to anyone with the URL. Use a long random path or Cloudflare Access for demos; rotate URLs often.

### Option B: ngrok

```bash
ngrok http 8080
```

Use the `https://….ngrok-free.app` URL.

### Option C: Tailscale Funnel

For invited users on your tailnet only.

---

## Step 4 — CORS (required for browser calls from Vercel)

Restart API with your Vercel origin(s):

```bash
export APP_CORS_ORIGINS="http://localhost:5173,https://YOUR-PROJECT.vercel.app,https://*.vercel.app"
./scripts/run-laptop-api.sh
```

Wildcards need Spring `allowedOriginPatterns` (already enabled in this repo).

---

## Step 5 — Deploy UI to Vercel

1. Import Git repo; set **Root Directory** to `project-1/ai-resume-tailor/frontend`.
2. **Environment variable** (Production):

   `VITE_API_BASE_URL` = `https://YOUR-TUNNEL-URL.trycloudflare.com`

3. Deploy. Rebuild when you change this variable.

Local dev without Vercel: leave `VITE_API_BASE_URL` unset; Vite proxies `/api` to `localhost:8080`.

---

## Step 6 — End-to-end test

1. Laptop: Ollama + `./scripts/run-laptop-api.sh` + cloudflared.
2. Browser: open Vercel URL → upload resume → paste JD → Generate.
3. If CORS error: fix `APP_CORS_ORIGINS` and exact Vercel hostname.
4. If timeout: local models are slow — use smaller model or increase `timeout-seconds` in `application-ollama.yml`.

---

## LM Studio instead of Ollama

1. Start LM Studio → Local Server → OpenAI compatible.
2. Run API:

```bash
export SPRING_PROFILES_ACTIVE=ollama
export OPENAI_BASE_URL=http://127.0.0.1:1234/v1
export OPENAI_MODEL=your-loaded-model-name
./scripts/run-laptop-api.sh
```

---

## When to move off the laptop

| Signal | Action |
|--------|--------|
| Need 24/7 | Deploy API to Railway/Render/Cloud Run |
| Need reliable speed | Cloud OpenAI or GPU host (RunPod, etc.) |
| Public users | Fixed domain + auth + rate limits |

Keep the same frontend; only change `VITE_API_BASE_URL`.

For **24/7 without running Ollama on your laptop**, use a **cloud LLM API** (Groq, OpenRouter, Together) — see **[CLOUD-LLM-OPTIONS.md](CLOUD-LLM-OPTIONS.md)**.

---

## Privacy policy (Vercel)

Static page: `frontend/public/privacy.html` — link from the app footer. Required before Chrome Web Store listing.
