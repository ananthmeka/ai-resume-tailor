#!/usr/bin/env bash
# Install pre-commit and pre-push hooks from .githooks/
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

if [[ ! -d .git ]]; then
  echo "No .git directory. Run from project root after git init."
  exit 1
fi

git config core.hooksPath .githooks
chmod +x .githooks/pre-commit .githooks/pre-push

echo "Installed git hooks (core.hooksPath=.githooks)"
echo "  pre-commit — blocks staging .env / gsk_ keys"
echo "  pre-push   — blocks push if .env in new commits"
