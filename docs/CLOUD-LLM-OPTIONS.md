# Cloud & free LLM options (when laptop/Ollama is off)

Your API already speaks **OpenAI-compatible** `POST /v1/chat/completions`. For 24/7 without your laptop, deploy **Spring Boot to Railway/Render/Cloud Run** and point it at a **cloud inference provider** (no GPU on your server).

```
[Vercel UI] → [Railway/Render API 24/7] → [Groq / OpenRouter / Together / OpenAI]
```

Local Ollama remains optional for privacy/dev.

---

## What this app needs from an LLM

| Requirement | Why |
|-------------|-----|
| **Instruction following** | Strict “no hallucination”, reorder/highlight rules |
| **Long context** | Resume (5–15 pages text) + job description |
| **Structured JSON** | Parse + optimize pipeline (`response_format: json_object` preferred) |
| **Stable API** | 24/7, HTTPS, API key on server only |

**Best quality/cost for production:** OpenAI `gpt-4o-mini` or `gpt-4o` (paid, low cost per resume).

**Best free/low-cost cloud path:** **Groq** or **OpenRouter** (free/cheap open models) — good enough for MVP; test JSON reliability.

---

## Recommended alternate paths (no laptop)

### 1. Groq (recommended first cloud-free tier)

- **Site:** https://console.groq.com  
- **API:** `https://api.groq.com/openai/v1`  
- **Cost:** Free tier with rate limits; paid tiers are inexpensive.  
- **Models (check current docs):** e.g. `llama-3.3-70b-versatile`, `llama-3.1-8b-instant`  
- **Fit:** Fast, good for MVP; 70B class models OK for resume rewrite; verify JSON mode on your chosen model.

**Run locally:**

```bash
export SPRING_PROFILES_ACTIVE=groq
export GROQ_API_KEY=gsk_...
cd backend && ./mvnw spring-boot:run
```

**Railway/Render env:**

```bash
SPRING_PROFILES_ACTIVE=groq
GROQ_API_KEY=gsk_...
APP_CORS_ORIGINS=https://your-app.vercel.app,https://*.vercel.app
```

---

### 2. OpenRouter (many models, including free)

- **Site:** https://openrouter.ai  
- **API:** `https://openrouter.ai/api/v1`  
- **Cost:** Per-model pricing; some **`:free`** models (limits change often).  
- **Fit:** One API key to try Llama, Qwen, Mistral, etc.; good for experiments.

```bash
export SPRING_PROFILES_ACTIVE=openrouter
export OPENROUTER_API_KEY=sk-or-...
export OPENAI_MODEL=meta-llama/llama-3.3-70b-instruct:free
```

Pick models with large context (32k+) for long resumes. Re-test when OpenRouter rotates free models.

---

### 3. Together AI

- **API:** `https://api.together.xyz/v1`  
- **Cost:** Free credits for new accounts; then pay-as-you-go.  
- **Models:** Llama, Qwen, Mixtral, etc.

```bash
export SPRING_PROFILES_ACTIVE=together
export TOGETHER_API_KEY=...
export OPENAI_MODEL=meta-llama/Llama-3.3-70B-Instruct-Turbo
```

---

### 4. Mistral (La Plateforme)

- **API:** OpenAI-compatible endpoint on Mistral’s platform.  
- **Cost:** Free tier / experiments tier (check current policy).  
- **Fit:** Strong European option; good instruction following.

Use profile `mistral` or set `OPENAI_BASE_URL` + key from Mistral console.

---

### 5. Google Gemini (free tier via AI Studio)

- **API:** Gemini API (AI Studio free quota).  
- **Note:** Native API is **not** OpenAI-shaped; use Gemini SDK or a gateway.  
- **Fit:** Good quality; requires a small code adapter **or** OpenRouter route to Gemini.

For zero code change today, prefer Groq/OpenRouter/Together.

---

### 6. Self-host open models in the cloud (free *compute*, you operate)

| Provider | Idea |
|----------|------|
| **Oracle Cloud Always Free** | ARM VM, run **Ollama** or vLLM 24/7 (you maintain OS, updates, security) |
| **Fly.io / Hetzner / RunPod** | Cheap GPU/CPU; Ollama + your JAR |

Same as laptop, but VM stays on. Higher ops burden than Groq/OpenRouter.

**Open models often used for writing/tasks:**

| Model family | Typical use |
|--------------|-------------|
| **Llama 3.3 70B** | Strong general + JSON (with prompting) |
| **Qwen 2.5 72B / 32B** | Good multilingual + structured tasks |
| **Mistral Large / Nemo** | Instruction following |
| **DeepSeek V3 / R1** | Reasoning (may be overkill; check API availability) |

Smaller 7B–8B models: faster/cheaper but more JSON errors on long resumes — use for dev only.

---

## Comparison (practical)

| Path | 24/7 | Cost | Quality (resume) | Ops effort |
|------|------|------|------------------|------------|
| Laptop + Ollama + tunnel | No | ₹0 | Medium | Low |
| **Groq API** | Yes | Free tier / low | Medium–Good | **Very low** |
| **OpenRouter** | Yes | Free/pay per model | Varies | Very low |
| OpenAI `gpt-4o-mini` | Yes | ~₹2–15 per heavy user/mo | **Best** | Very low |
| Ollama on Oracle free VM | Yes | ₹0 compute | Medium | **High** |

**Suggested strategy**

1. **Dev:** Ollama on laptop (`profile=ollama`).  
2. **Demo / friends 24/7:** API on **Railway** + **Groq** (`profile=groq`).  
3. **Paying users:** Same API + **OpenAI** or Groq 70B + human QA sampling.

---

## Deploy API 24/7 (Railway example)

1. Connect repo; root: `project-1/ai-resume-tailor/backend`.  
2. Build: `./mvnw -DskipTests package` → `java -jar target/*.jar`.  
3. Env:

   ```bash
   SPRING_PROFILES_ACTIVE=groq
   GROQ_API_KEY=gsk_...
   APP_CORS_ORIGINS=https://your-app.vercel.app,https://*.vercel.app
   ```

4. Vercel: `VITE_API_BASE_URL=https://your-service.up.railway.app` (no trailing slash).

Same pattern on **Render** or **Cloud Run**.

---

## Auth & rate limits (implemented)

When the API is on the public internet:

1. **`X-API-Key` or `Authorization: Bearer`** — set `APP_API_KEY` + `APP_API_KEY_ENABLED=true` (on by default with profile `railway`).  
2. **Rate limits** — `APP_RATE_LIMIT_PER_MINUTE` (default 30/IP/min), `APP_TAILOR_LIMIT_PER_HOUR` (default 10 tailor calls/IP/hour).  
3. **`GET /api/health`** — no key (Railway health check).  
4. Frontend beta: `VITE_API_KEY` (same as `APP_API_KEY`; rotate if leaked).

Full deploy walkthrough: **[DEPLOY-RAILWAY.md](DEPLOY-RAILWAY.md)**.

Optional later: Cloudflare in front of Railway for DDoS + WAF.

---

## Verify which LLM is active

```bash
curl https://YOUR-API/api/llm-status
curl https://YOUR-API/api/health
```

---

## Spring profiles in this repo

| Profile | Provider |
|---------|----------|
| *(default)* | OpenAI |
| `ollama` | Local Ollama |
| `railway` | Production CORS + API key defaults |
| `groq` | Groq cloud |
| `openrouter` | OpenRouter |
| `together` | Together AI |

See `backend/src/main/resources/application-*.yml` and [ENV-TEMPLATE.md](ENV-TEMPLATE.md).
