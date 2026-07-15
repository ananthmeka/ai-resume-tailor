#!/usr/bin/env bash
# Run Spring Boot on your laptop; LLM via local Ollama. Expose with Cloudflare Tunnel for Vercel UI.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-ollama}"
export OPENAI_BASE_URL="${OPENAI_BASE_URL:-http://127.0.0.1:11434/v1}"
export OPENAI_MODEL="${OPENAI_MODEL:-qwen2.5:7b}"
export OPENAI_API_KEY="${OPENAI_API_KEY:-ollama}"

# Set after you deploy Vercel, e.g.:
# export APP_CORS_ORIGINS="http://localhost:5173,https://your-app.vercel.app,https://*.vercel.app"

echo "Profile: $SPRING_PROFILES_ACTIVE"
echo "LLM: $OPENAI_BASE_URL model=$OPENAI_MODEL"
echo "CORS: ${APP_CORS_ORIGINS:-localhost only}"
echo ""
echo "Next: expose port 8080 — cloudflared tunnel --url http://localhost:8080"
echo ""

exec ./mvnw spring-boot:run
