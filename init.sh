#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

START_CMD=(./gradlew :app:installGithubDebug)

echo "==> Working directory: $PWD"
echo "==> Local repository state (no Gradle checks)"
git rev-parse --abbrev-ref HEAD
git --no-pager status --short
if [ ! -f ./gradlew ]; then
  echo "Gradle wrapper missing" >&2
  exit 1
fi
echo "Validation policy: docs/harness/quality-gates.md"

echo "==> Startup command"
printf '    %q' "${START_CMD[@]}"
printf '\n'

if [ "${RUN_START_COMMAND:-0}" = "1" ]; then
  echo "==> Starting the app"
  exec "${START_CMD[@]}"
fi

echo "Set RUN_START_COMMAND=1 if you want init.sh to launch the app directly."
