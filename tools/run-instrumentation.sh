#!/usr/bin/env bash
# Collect synthetic UI captures even when an assertion fails; retain the test result.
set +e
adb logcat -G 16M
adb logcat -c
gradle :app:connectedDebugAndroidTest --stacktrace
veil_test_exit_code=$?
adb logcat -d -b crash
adb logcat -d -s VeilPreview:I > app/build/ui-capture.log
exit "$veil_test_exit_code"
