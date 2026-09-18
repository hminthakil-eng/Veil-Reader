#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(pwd)"
CACHE_DIR="${HOME}/.cache/veil-render-android"
JDK_DIR="${CACHE_DIR}/jdk17"
ANDROID_HOME="${CACHE_DIR}/android-sdk"
CMDLINE_DIR="${ANDROID_HOME}/cmdline-tools/latest"

mkdir -p "${CACHE_DIR}" "${ANDROID_HOME}"

if [ ! -x "${JDK_DIR}/bin/java" ]; then
  rm -rf "${JDK_DIR}"
  mkdir -p "${JDK_DIR}"
  curl -fsSL --retry 3 --retry-delay 2 \
    "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse" \
    -o "${CACHE_DIR}/jdk17.tar.gz"
  tar -xzf "${CACHE_DIR}/jdk17.tar.gz" -C "${JDK_DIR}" --strip-components=1
fi

export JAVA_HOME="${JDK_DIR}"
export PATH="${JAVA_HOME}/bin:${PATH}"

if [ ! -x "${CMDLINE_DIR}/bin/sdkmanager" ]; then
  rm -rf "${ANDROID_HOME}/cmdline-tools"
  mkdir -p "${ANDROID_HOME}/cmdline-tools"
  curl -fsSL --retry 3 --retry-delay 2 \
    "https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip" \
    -o "${CACHE_DIR}/cmdline-tools.zip"
  rm -rf "${CACHE_DIR}/cmdline-tools-unpacked"
  mkdir -p "${CACHE_DIR}/cmdline-tools-unpacked"
  unzip -q "${CACHE_DIR}/cmdline-tools.zip" -d "${CACHE_DIR}/cmdline-tools-unpacked"
  mv "${CACHE_DIR}/cmdline-tools-unpacked/cmdline-tools" "${CMDLINE_DIR}"
fi

export ANDROID_HOME
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${CMDLINE_DIR}/bin:${ANDROID_HOME}/platform-tools:${PATH}"

yes | sdkmanager --licenses >/dev/null || true
sdkmanager "platform-tools" "platforms;android-37" "build-tools;37.0.0"

chmod +x ./gradlew
./gradlew --no-daemon :app:testDebugUnitTest --stacktrace
./gradlew --no-daemon :app:lintDebug --stacktrace
./gradlew --no-daemon :app:assembleDebug --stacktrace
./gradlew --no-daemon :benchmark:assembleBenchmarkBenchmark --stacktrace

mkdir -p "${ROOT_DIR}/render-ci"
cat > "${ROOT_DIR}/render-ci/index.html" <<'EOF'
<!doctype html>
<html><body><h1>Veil Reader CI: PASS</h1><p>Readium 3.4 verification build completed.</p></body></html>
EOF
