#!/usr/bin/env bash
# Native local standalone (no Docker): Ollama + Spring Boot + Vite UI in the browser.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if ! curl -sf http://127.0.0.1:11434/api/tags >/dev/null 2>&1; then
  echo "Ollama is not running on http://127.0.0.1:11434"
  echo "  brew install ollama && ollama serve"
  echo "  ollama pull qwen2.5:14b   # recommended for 2-page resumes"
  exit 1
fi

MODEL="${OPENAI_MODEL:-qwen2.5:14b}"
if ! curl -sf http://127.0.0.1:11434/api/tags | grep -q "\"name\":\"${MODEL}"; then
  echo "Model ${MODEL} not found locally. Run: ollama pull ${MODEL}"
  exit 1
fi

export SPRING_PROFILES_ACTIVE=ollama
export OPENAI_BASE_URL="${OPENAI_BASE_URL:-http://127.0.0.1:11434/v1}"
export OPENAI_MODEL="$MODEL"
export OPENAI_API_KEY=ollama
export APP_API_KEY_ENABLED=false
export GENERATE_INTERVIEW_QUESTIONS=false
export APP_CORS_ORIGINS="${APP_CORS_ORIGINS:-http://localhost:5173,http://127.0.0.1:5173}"

echo "=== AI Resume Tailor (local standalone) ==="
echo "LLM:  $OPENAI_BASE_URL  model=$OPENAI_MODEL"
echo "UI:   http://localhost:5173  (after npm run dev)"
echo "API:  http://localhost:8080"
echo ""
echo "Starting backend in background..."
cd "$ROOT/backend"
./mvnw -q spring-boot:run &
API_PID=$!
trap 'kill $API_PID 2>/dev/null || true' EXIT INT TERM

for i in $(seq 1 60); do
  if curl -sf http://127.0.0.1:8080/api/health >/dev/null; then
    break
  fi
  sleep 2
done
curl -sf http://127.0.0.1:8080/api/health || { echo "Backend failed to start"; exit 1; }

cd "$ROOT/frontend"
if [[ ! -d node_modules ]]; then
  npm ci
fi
echo "Starting Vite (Ctrl+C stops UI and backend)..."
npm run dev
