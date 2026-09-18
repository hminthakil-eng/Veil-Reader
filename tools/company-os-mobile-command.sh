#!/usr/bin/env bash
set -euo pipefail

COMMAND="${1:-status}"

echo "Eyad Studio Company OS — Veil Reader mobile command"
echo "command=${COMMAND}"
echo "repo=${GITHUB_REPOSITORY:-local}"
echo "ref=${GITHUB_REF_NAME:-local}"
echo "sha=${GITHUB_SHA:-local}"

case "${COMMAND}" in
  status)
    echo "java:"
    java -version
    echo "gradle:"
    gradle --version
    ;;

  unit-tests)
    sh tools/test-policy.sh
    python3 tools/test_performance_budget.py
    python3 tools/test_performance_delta.py
    python3 tools/test_performance_dashboard.py
    gradle :app:testDebugUnitTest --stacktrace
    ;;

  full-ci)
    sh tools/test-policy.sh
    python3 tools/test_performance_budget.py
    python3 tools/test_performance_delta.py
    python3 tools/test_performance_dashboard.py
    gradle :app:testDebugUnitTest --stacktrace
    git diff --exit-code -- app/schemas/
    gradle :app:lintDebug --stacktrace
    gradle :app:assembleDebug --stacktrace
    gradle :benchmark:assembleBenchmarkBenchmark --stacktrace
    ;;

  *)
    echo "Unsupported command: ${COMMAND}" >&2
    exit 64
    ;;
esac
