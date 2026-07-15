#!/bin/sh
set -e
# Railway routes public traffic to $PORT. CLI args override application.yml.
LISTEN_PORT="${PORT:-8080}"
echo "ai-resume-tailor: binding 0.0.0.0:${LISTEN_PORT} (env PORT=${PORT:-<not set>})"
exec java -jar /app/app.jar \
  --server.port="${LISTEN_PORT}" \
  --server.address=0.0.0.0
