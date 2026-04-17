#!/usr/bin/env sh
##
## Gradle bootstrap wrapper — downloads Gradle 8.7 on first run.
## No gradle-wrapper.jar required.
##

GRADLE_VERSION=8.7
GRADLE_DIST_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
GRADLE_INSTALL_DIR="${HOME}/.gradle-bootstrap/${GRADLE_VERSION}"
GRADLE_BIN="${GRADLE_INSTALL_DIR}/gradle-${GRADLE_VERSION}/bin/gradle"

if [ ! -f "${GRADLE_BIN}" ]; then
  echo "Bootstrapping Gradle ${GRADLE_VERSION}..."
  mkdir -p "${GRADLE_INSTALL_DIR}"
  if command -v curl > /dev/null 2>&1; then
    curl -fsSL "${GRADLE_DIST_URL}" -o "${GRADLE_INSTALL_DIR}/gradle.zip"
  else
    wget -q "${GRADLE_DIST_URL}" -O "${GRADLE_INSTALL_DIR}/gradle.zip"
  fi
  unzip -q "${GRADLE_INSTALL_DIR}/gradle.zip" -d "${GRADLE_INSTALL_DIR}"
  rm "${GRADLE_INSTALL_DIR}/gradle.zip"
fi

exec "${GRADLE_BIN}" "$@"
