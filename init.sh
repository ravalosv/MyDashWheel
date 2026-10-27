#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

INSTALL_CMD=(./gradlew dependencies)
VERIFY_CMD=(./gradlew :app:lintGithubDebug :app:testGithubDebugUnitTest :app:assembleGithubDebug)
START_CMD=(./gradlew :app:installGithubDebug)

echo "==> Working directory: $PWD"
echo "==> Syncing dependencies"
"${INSTALL_CMD[@]}"

echo "==> Running baseline verification"
"${VERIFY_CMD[@]}"

echo "==> Startup command"
printf '    %q' "${START_CMD[@]}"
printf '\n'

if [ "${RUN_START_COMMAND:-0}" = "1" ]; then
  echo "==> Starting the app"
  exec "${START_CMD[@]}"
fi

echo "Set RUN_START_COMMAND=1 if you want init.sh to launch the app directly."
