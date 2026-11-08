#!/bin/sh
set -e
if command -v gradle >/dev/null 2>&1; then exec gradle "$@"; fi
echo "Gradle is not installed. Install Gradle 8.7+ or use GitHub Actions." >&2
exit 1
