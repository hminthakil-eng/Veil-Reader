#!/usr/bin/env bash
# Collect synthetic UI captures even when an assertion fails; retain the test result.
set +e
gradle :app:connectedDebugAndroidTest --stacktrace
veil_test_exit_code=$?
adb logcat -d -b crash
adb pull /data/local/tmp/veil-ui-previews app/build/ui-screenshots
exit "$veil_test_exit_code"
