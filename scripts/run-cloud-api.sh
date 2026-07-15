#!/usr/bin/env bash
# Run API against a cloud LLM (24/7 when deployed to Railway/Render; also for local testing).
# Usage:
#   ./scripts/run-cloud-api.sh groq
#   ./scripts/run-cloud-api.sh openrouter
#   ./scripts/run-cloud-api.sh together
set -euo pipefail
PROFILE="${1:-groq}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/backend"

export SPRING_PROFILES_ACTIVE="$PROFILE"

case "$PROFILE" in
  groq)
    : "${GROQ_API_KEY:?Set GROQ_API_KEY from https://console.groq.com/keys}"
    ;;
  openrouter)
    : "${OPENROUTER_API_KEY:?Set OPENROUTER_API_KEY from https://openrouter.ai/keys}"
    ;;
  together)
    : "${TOGETHER_API_KEY:?Set TOGETHER_API_KEY from Together AI}"
    ;;
  *)
    echo "Unknown profile: $PROFILE (use groq | openrouter | together)"
    exit 1
    ;;
esac

echo "Cloud LLM profile: $SPRING_PROFILES_ACTIVE"
echo "Check: curl -s http://localhost:8080/api/llm-status | jq ."
exec ./mvnw spring-boot:run
