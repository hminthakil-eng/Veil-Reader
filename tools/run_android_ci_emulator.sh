#!/usr/bin/env bash
set -euo pipefail

MODE="${1:-}"
VEIL_TEST_APPLICATION_ID="${VEIL_TEST_APPLICATION_ID:-com.veilreader.app}"
case "$MODE" in
  storage|performance|durability) ;;
  *) echo "Usage: $0 {storage|performance|durability}" >&2; exit 2 ;;
esac

SDK_ROOT="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
[[ -n "$SDK_ROOT" ]] || { echo "ANDROID_HOME or ANDROID_SDK_ROOT is required." >&2; exit 1; }
SDKMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager"
AVDMANAGER="$SDK_ROOT/cmdline-tools/latest/bin/avdmanager"
EMULATOR="$SDK_ROOT/emulator/emulator"
ADB="$SDK_ROOT/platform-tools/adb"
for tool in "$SDKMANAGER" "$AVDMANAGER"; do
  [[ -x "$tool" ]] || { echo "Missing Android SDK tool: $tool" >&2; exit 1; }
done

API_LEVEL="${VEIL_EMULATOR_API_LEVEL:-35}"
IMAGE="system-images;android-${API_LEVEL};default;x86_64"
"$SDKMANAGER" --install platform-tools emulator "$IMAGE" </dev/null >/dev/null
[[ -x "$EMULATOR" && -x "$ADB" ]] || { echo "Android emulator toolchain was not installed." >&2; exit 1; }

TEMP_ROOT="$(mktemp -d)"
export ANDROID_AVD_HOME="$TEMP_ROOT/avd"
export ANDROID_USER_HOME="$TEMP_ROOT/android-user"
mkdir -p "$ANDROID_AVD_HOME" "$ANDROID_USER_HOME" build/reports

SERIAL="emulator-5554"
AVD_NAME="veil_ci_${MODE}"
EMULATOR_PID=""
EMULATOR_LOG="build/reports/emulator-${MODE}.log"
LOGCAT_LOG="build/reports/logcat-${MODE}.log"
LOCK_FILE="${RUNNER_TEMP:-/tmp}/veil-emulator-5554.lock"
exec 9>"$LOCK_FILE"
flock -n 9 || { echo "$SERIAL is already owned by another process." >&2; exit 1; }

cleanup() {
  status=$?
  set +e
  if [[ "$MODE" == "storage" ]]; then
    "$ADB" -s "$SERIAL" pull /data/local/tmp/veil-grayfog-review build/reports/ >/dev/null 2>&1
  fi
  # Only the inert Compose review directory; never export publication/history files.
  # Preserve the original test exit code even when optional capture collection fails.
  if [[ "$MODE" == "storage" ]] &&
     "$ADB" -s "$SERIAL" shell run-as "$VEIL_TEST_APPLICATION_ID" test -d files/grayfog-review >/dev/null 2>&1; then
    if "$ADB" -s "$SERIAL" exec-out run-as "$VEIL_TEST_APPLICATION_ID" \
        tar -C files -cf - grayfog-review >build/reports/grayfog-review.tar; then
      echo "Saved Compose review captures: build/reports/grayfog-review.tar"
    else
      rm -f build/reports/grayfog-review.tar
      echo "Optional Grayfog capture collection failed; test status remains $status." >&2
    fi
  fi
  if (( status != 0 )); then
    "$ADB" -s "$SERIAL" logcat -d >"$LOGCAT_LOG" 2>&1
  fi
  if [[ -n "$EMULATOR_PID" ]] && kill -0 "$EMULATOR_PID" 2>/dev/null; then
    "$ADB" -s "$SERIAL" emu kill >/dev/null 2>&1
    sleep 1
    kill "$EMULATOR_PID" >/dev/null 2>&1
  fi
  [[ -n "$EMULATOR_PID" ]] && wait "$EMULATOR_PID" 2>/dev/null
  rm -rf "$TEMP_ROOT"
  return "$status"
}
trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

"$ADB" start-server >/dev/null
if "$ADB" devices | awk 'NR > 1 { print $1 }' | grep -Fxq "$SERIAL"; then
  echo "Refusing to reuse an existing $SERIAL." >&2
  exit 1
fi

printf 'no\n' | "$AVDMANAGER" create avd   --force --name "$AVD_NAME" --package "$IMAGE" --device pixel_6 >/dev/null

"$EMULATOR"   -avd "$AVD_NAME" -port 5554   -no-window -no-audio -no-boot-anim -no-metrics -no-snapshot -wipe-data   -gpu lavapipe -accel on -memory 2048 -partition-size 6144   >"$EMULATOR_LOG" 2>&1 &
EMULATOR_PID="$!"

assert_emulator_alive() {
  if [[ -z "$EMULATOR_PID" ]] || ! kill -0 "$EMULATOR_PID" 2>/dev/null; then
    echo "Android emulator process exited unexpectedly." >&2
    tail -200 "$EMULATOR_LOG" >&2 || true
    return 1
  fi
}

timeout 180 "$ADB" -s "$SERIAL" wait-for-device
assert_emulator_alive

booted=false
for _ in $(seq 1 120); do
  assert_emulator_alive
  if [[ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" == "1" ]]; then
    booted=true
    break
  fi
  sleep 2
done
[[ "$booted" == true ]] || { echo "Emulator did not finish booting." >&2; exit 1; }

for _ in $(seq 1 20); do
  if "$ADB" -s "$SERIAL" shell setprop debug.sf.luma_sampling 0 >/dev/null 2>&1; then break; fi
  assert_emulator_alive
  sleep 1
done

stable_samples=0
stable_system_server=""
for _ in $(seq 1 60); do
  assert_emulator_alive
  system_server="$("$ADB" -s "$SERIAL" shell pidof system_server 2>/dev/null | tr -d '\r' || true)"
  available_kb="$("$ADB" -s "$SERIAL" shell df -k /data 2>/dev/null | tr -d '\r' | awk 'NR == 2 { print $4 }' || true)"
  healthy=false
  if [[ "$available_kb" =~ ^[0-9]+$ ]] &&
     (( available_kb >= 524288 )) &&
     [[ -n "$system_server" ]] &&
     "$ADB" -s "$SERIAL" shell cmd package list packages android >/dev/null 2>&1 &&
     "$ADB" -s "$SERIAL" shell cmd activity get-config >/dev/null 2>&1 &&
     "$ADB" -s "$SERIAL" shell cmd settings get global window_animation_scale >/dev/null 2>&1; then
    healthy=true
  fi

  if [[ "$healthy" == true && "$system_server" == "$stable_system_server" ]]; then
    stable_samples=$((stable_samples + 1))
  else
    stable_samples=0
    stable_system_server="$system_server"
  fi
  (( stable_samples >= 6 )) && break
  sleep 5
done
(( stable_samples >= 6 )) || {
  echo "Android services failed to stabilize before tests." >&2
  "$ADB" -s "$SERIAL" shell df -h /data >&2 || true
  tail -200 "$EMULATOR_LOG" >&2 || true
  exit 1
}

"$ADB" -s "$SERIAL" shell settings put global window_animation_scale 0
"$ADB" -s "$SERIAL" shell settings put global transition_animation_scale 0
"$ADB" -s "$SERIAL" shell settings put global animator_duration_scale 0
export ANDROID_SERIAL="$SERIAL"

run_gradle() {
  gradle --no-daemon --max-workers=2 --no-parallel "$@"
}

case "$MODE" in
  storage)
    "$ADB" -s "$SERIAL" shell rm -rf /data/local/tmp/veil-grayfog-review
    run_gradle :app:connectedDebugAndroidTest --stacktrace
    ;;
  performance)
    run_gradle :app:generateBaselineProfile --stacktrace       -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile       -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
    assert_emulator_alive

    PERF_RESULTS_DIR="benchmark/build/perf-results"
    rm -rf "$PERF_RESULTS_DIR"
    mkdir -p "$PERF_RESULTS_DIR"

    preserve_benchmark_result() {
      local label="$1"
      local source_file
      source_file="$(
        find benchmark/build/outputs/connected_android_test_additional_output/benchmarkBenchmark \
          -type f -name '*-benchmarkData.json' -printf '%T@ %p\n' 2>/dev/null \
          | sort -nr \
          | head -1 \
          | cut -d' ' -f2-
      )"
      [[ -n "$source_file" && -f "$source_file" ]] || {
        echo "No AndroidX benchmarkData JSON was produced for $label." >&2
        exit 1
      }
      cp "$source_file" "$PERF_RESULTS_DIR/${label}-benchmarkData.json"
    }

    run_benchmark_class() {
      local class_name="$1"
      local label="$2"
      run_gradle :benchmark:connectedBenchmarkBenchmarkAndroidTest --stacktrace \
        "-Pandroid.testInstrumentationRunnerArguments.class=${class_name}"
      preserve_benchmark_result "$label"
      assert_emulator_alive
    }

    # Run independently: this self-instrumenting benchmark module only executed the first entry
    # when a comma-separated class list was supplied. Independent runs also preserve auditable JSON
    # for both Startup and Reader frame-smoke budget groups.
    run_benchmark_class com.veilreader.benchmark.StartupBenchmark startup
    run_benchmark_class com.veilreader.benchmark.ReaderFrameSmokeBenchmark reader-frame-smoke
    ;;

  durability)
    run_gradle :app:installDebug --stacktrace
    assert_emulator_alive

    PACKAGE="$VEIL_TEST_APPLICATION_ID"
    COMPONENT="${PACKAGE}/com.veilreader.app.debug.ReaderDurabilityProbeActivity"
    RESULT_FILE="files/reader-durability-probe-result.txt"
    REPLAY_GUARD_FILE="files/reader-durability-probe-replay-guard.txt"
    REPORT="build/reports/reader-durability-fault-injection.txt"
    MARKER_LOG="build/reports/reader-durability-marker.log"
    CYCLES="${VEIL_DURABILITY_CYCLES:-18}"
    [[ "$CYCLES" =~ ^[1-9][0-9]*$ ]] || {
      echo "VEIL_DURABILITY_CYCLES must be a positive integer." >&2
      exit 2
    }

    "$ADB" -s "$SERIAL" shell pm clear "$PACKAGE" >/dev/null
    # The APK's process-kill entry point is inert without this private, non-backed-up
    # capability. Shell cannot toggle components on API 35; adb run-as can create it.
    "$ADB" -s "$SERIAL" shell run-as "$PACKAGE" mkdir -p no_backup
    "$ADB" -s "$SERIAL" shell run-as "$PACKAGE" touch no_backup/reader-durability-probe-enabled
    : >"$REPORT"
    : >"$MARKER_LOG"
    {
      echo "Veil Reader semantic locator process-death fault injection"
      echo "sha=$(git rev-parse HEAD)"
      echo "api=$("$ADB" -s "$SERIAL" shell getprop ro.build.version.sdk | tr -d '\r')"
      echo "model=$("$ADB" -s "$SERIAL" shell getprop ro.product.model | tr -d '\r')"
      echo "cycles_per_scenario=$CYCLES"
      echo "started_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    } >>"$REPORT"

    probe_clear_result() {
      "$ADB" -s "$SERIAL" shell run-as "$PACKAGE" rm -f "$RESULT_FILE" >/dev/null 2>&1 || true
    }

    probe_clear_replay_guard() {
      "$ADB" -s "$SERIAL" shell run-as "$PACKAGE" rm -f "$REPLAY_GUARD_FILE" >/dev/null 2>&1 || true
    }

    probe_read_result() {
      "$ADB" -s "$SERIAL" exec-out run-as "$PACKAGE" cat "$RESULT_FILE" 2>/dev/null | tr -d '\r' || true
    }

    probe_wait_result() {
      local phase="$1"
      local result=""
      for _ in $(seq 1 150); do
        result="$(probe_read_result)"
        if [[ "$result" == PASS* ]]; then
          echo "$result" >>"$REPORT"
          return 0
        fi
        if [[ "$result" == FAIL* ]]; then
          echo "$result" | tee -a "$REPORT" >&2
          return 1
        fi
        sleep 0.1
      done
      echo "FAIL phase=$phase error=result_timeout" | tee -a "$REPORT" >&2
      return 1
    }

    probe_start() {
      timeout 20 "$ADB" -s "$SERIAL" shell am start -W -n "$COMPONENT" "$@" >/dev/null
    }

    probe_commit_then_self_kill() {
      local token="$1"
      shift
      local marker_file="$TEMP_ROOT/reader-durability-marker-$token.log"
      local logcat_pid=""
      local result=""
      local marker="COMMIT_READY token=${token}-destination"

      rm -f "$marker_file"
      "$ADB" -s "$SERIAL" logcat -c
      "$ADB" -s "$SERIAL" logcat -v brief -s VeilDurabilityProbe:I VeilReaderTrace:D '*:S' >"$marker_file" 2>&1 &
      logcat_pid="$!"

      # The debug probe kills its own process immediately after onLocatorUpdate() returns.
      # adb/logcat only observes the boundary; it no longer controls when the crash happens.
      "$ADB" -s "$SERIAL" shell am start -n "$COMPONENT" "$@" >/dev/null 2>&1 || true

      for _ in $(seq 1 250); do
        if grep -Fq "$marker" "$marker_file" 2>/dev/null; then
          grep -F "$marker" "$marker_file" | tail -1 >>"$MARKER_LOG"

          # Cleanup only: the semantic crash has already been requested inside the app process.
          # force-stop prevents ActivityManager from restoring the no-history debug probe.
          "$ADB" -s "$SERIAL" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
          kill "$logcat_pid" >/dev/null 2>&1 || true
          wait "$logcat_pid" 2>/dev/null || true
          # Keep measured journal latency before the next case clears logcat. These are
          # emulator observations, not a physical-device performance acceptance result.
          grep -E 'event=locator_crash_checkpoint_(durable|failed)( |$)' "$marker_file" >>"$MARKER_LOG" || true

          for _ in $(seq 1 100); do
            if [[ -z "$("$ADB" -s "$SERIAL" shell pidof "$PACKAGE" 2>/dev/null | tr -d '\r')" ]]; then
              return 0
            fi
            sleep 0.01
          done
          echo "FAIL phase=commit:$token error=process_survived_self_kill_cleanup" | tee -a "$REPORT" >&2
          return 1
        fi

        result="$(probe_read_result)"
        if [[ "$result" == FAIL* ]]; then
          kill "$logcat_pid" >/dev/null 2>&1 || true
          wait "$logcat_pid" 2>/dev/null || true
          echo "$result" | tee -a "$REPORT" >&2
          return 1
        fi
        sleep 0.01
      done

      kill "$logcat_pid" >/dev/null 2>&1 || true
      wait "$logcat_pid" 2>/dev/null || true
      [[ -f "$marker_file" ]] && cat "$marker_file" >>"$MARKER_LOG"
      echo "FAIL phase=commit:$token error=self_kill_marker_timeout" | tee -a "$REPORT" >&2
      return 1
    }

    run_durability_case() {
      local scenario="$1"
      local event="$2"
      local origin_progress="$3"
      local destination_progress="$4"
      local expected="$5"
      local cycle="$6"

      local token="${scenario}-${cycle}"
      local origin_key="${token}-origin"
      local destination_key="${token}-destination"
      local expected_progress="$destination_progress"
      local expected_key="$destination_key"
      if [[ "$expected" == "origin" ]]; then
        expected_progress="$origin_progress"
        expected_key="$origin_key"
      fi

      "$ADB" -s "$SERIAL" shell am force-stop "$PACKAGE" >/dev/null 2>&1 || true
      probe_clear_result
      probe_start \
        --es probe_action seed \
        --es probe_origin_progress "$origin_progress" \
        --es probe_origin_key "$origin_key"
      probe_wait_result "seed:$token"
      "$ADB" -s "$SERIAL" shell am force-stop "$PACKAGE" >/dev/null
      sleep 0.05

      probe_clear_result
      probe_clear_replay_guard
      probe_commit_then_self_kill "$token" \
        --es probe_action commit_and_kill \
        --es probe_event "$event" \
        --es probe_session_id "session-$token" \
        --es probe_destination_progress "$destination_progress" \
        --es probe_destination_key "$destination_key"

      probe_clear_result
      probe_start \
        --es probe_action verify \
        --es probe_expected_progress "$expected_progress" \
        --es probe_expected_key "$expected_key"
      probe_wait_result "verify:$token"

      echo "PASS scenario=$scenario cycle=$cycle event=$event expected=$expected" >>"$REPORT"
    }

    # 6 scenarios x 18 default cycles = 108 self-SIGKILL samples on each exact SHA.
    for cycle in $(seq 1 "$CYCLES"); do
      run_durability_case page-forward page 0.20 0.21 destination "$cycle"
      run_durability_case page-backward page 0.60 0.59 destination "$cycle"
      run_durability_case paper-commit paper 0.30 0.31 destination "$cycle"
      run_durability_case paper-preview-cancel preview 0.40 0.41 origin "$cycle"
      run_durability_case jump-commit jump 0.50 0.75 destination "$cycle"
      run_durability_case final-snapshot final 0.80 0.81 destination "$cycle"
    done

    {
      echo "completed_utc=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
      echo "result=PASS"
      echo "samples=$((CYCLES * 6))"
    } >>"$REPORT"
    ;;

esac

assert_emulator_alive
echo "Veil Android emulator gate passed: mode=$MODE api=$API_LEVEL renderer=lavapipe"
