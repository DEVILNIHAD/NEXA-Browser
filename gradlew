#!/bin/sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
GRADLE_VERSION="8.9"
DIST_DIR="${GRADLE_USER_HOME:-$HOME/.gradle}/wrapper/dists/nexa-gradle-${GRADLE_VERSION}"
GRADLE_HOME="$DIST_DIR/gradle-${GRADLE_VERSION}"
GRADLE_BIN="$GRADLE_HOME/bin/gradle"

if [ -x "$GRADLE_BIN" ]; then
  exec "$GRADLE_BIN" "$@"
fi

if command -v gradle >/dev/null 2>&1; then
  exec gradle "$@"
fi

mkdir -p "$DIST_DIR"
ZIP="$DIST_DIR/gradle-${GRADLE_VERSION}-bin.zip"
if [ ! -f "$ZIP" ]; then
  echo "Downloading Gradle ${GRADLE_VERSION}..."
  curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o "$ZIP"
fi

rm -rf "$GRADLE_HOME"
unzip -q "$ZIP" -d "$DIST_DIR"
exec "$GRADLE_BIN" "$@"
