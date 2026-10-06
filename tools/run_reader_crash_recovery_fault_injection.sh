#!/usr/bin/env bash
set -euo pipefail

ADB="${1:-${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}/platform-tools/adb}"
SERIAL="${2:-${ANDROID_SERIAL:-emulator-5554}}"
PACKAGE="com.veilreader.app"
COMPONENT="com.veilreader.app/.debug.ReaderCrashRecoveryFaultActivity"
SEED_FILE="files/reader-crash-recovery-seed.json"
RESULT_FILE="files/reader-crash-recovery-fault.json"
HOST_RESULT="build/reports/reader-crash-recovery-fault.json"

mkdir -p build/reports
rm -f "$HOST_RESULT"

"$ADB" -s "$SERIAL" shell pm clear "$PACKAGE" >/dev/null

# The seed activity intentionally kills itself. am start -W may therefore report the process death
# even though that is the expected fault, so evidence is validated from app-private storage instead.
set +e
"$ADB" -s "$SERIAL" shell am start -W -n "$COMPONENT"   --es "veil.reader.crashRecovery.mode" "seed-kill" >/dev/null 2>&1
set -e

dead=false
for _ in $(seq 1 100); do
  if [[ -z "$("$ADB" -s "$SERIAL" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')" ]]; then
    dead=true
    break
  fi
  sleep 0.1
done
[[ "$dead" == true ]] || {
  echo "Reader crash fault injector did not terminate the app process." >&2
  exit 1
}

"$ADB" -s "$SERIAL" exec-out run-as "$PACKAGE" cat "$SEED_FILE" >/tmp/veil-reader-crash-seed.json
python3 - /tmp/veil-reader-crash-seed.json <<'PY'
import json, sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    payload = json.load(fh)
if payload.get("status") != "checkpointed":
    raise SystemExit(f"invalid seed evidence: {payload}")
if payload.get("sequence") != 32 or payload.get("commitCount") != 32:
    raise SystemExit(f"fault commit series incomplete: {payload}")
latency = payload.get("checkpointWriteLatencyUs") or {}
for key in ("p50", "p95", "max"):
    if not isinstance(latency.get(key), int) or latency[key] < 0:
        raise SystemExit(f"invalid checkpoint latency evidence: {payload}")
PY

"$ADB" -s "$SERIAL" shell am start -W -n "$COMPONENT"   --es "veil.reader.crashRecovery.mode" "verify" >/dev/null

ready=false
for _ in $(seq 1 100); do
  if "$ADB" -s "$SERIAL" exec-out run-as "$PACKAGE" cat "$RESULT_FILE" >"$HOST_RESULT.tmp" 2>/dev/null; then
    mv "$HOST_RESULT.tmp" "$HOST_RESULT"
    ready=true
    break
  fi
  sleep 0.1
done
rm -f "$HOST_RESULT.tmp"
[[ "$ready" == true ]] || {
  echo "Reader crash recovery verification did not produce evidence." >&2
  exit 1
}

python3 - "$HOST_RESULT" <<'PY'
import json, sys
with open(sys.argv[1], "r", encoding="utf-8") as fh:
    payload = json.load(fh)
expected = {
    "roomBeforeWasOrigin": True,
    "checkpointRecovered": True,
    "roomAfterWasDestination": True,
    "checkpointCleared": True,
    "commitCount": 32,
}
if payload.get("status") != "pass":
    raise SystemExit(f"reader crash recovery failed: {payload}")
details = payload.get("details") or {}
for key, value in expected.items():
    if details.get(key) != value:
        raise SystemExit(f"unexpected reader crash recovery evidence: {payload}")
latency = details.get("checkpointWriteLatencyUs") or {}
if not (0 <= latency.get("p50", -1) <= latency.get("p95", -1) <= latency.get("max", -1)):
    raise SystemExit(f"invalid checkpoint latency ordering: {payload}")
print("Reader crash recovery fault injection passed:", json.dumps(payload, sort_keys=True))
PY
