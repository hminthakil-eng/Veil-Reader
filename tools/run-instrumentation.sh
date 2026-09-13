#!/usr/bin/env bash
# Collect synthetic UI captures even when an assertion fails; retain the test result.
set +e
gradle :app:connectedDebugAndroidTest --stacktrace
veil_test_exit_code=$?
adb pull /sdcard/Android/data/com.veilreader.app.preview/files/ui-screenshots app/build/ui-screenshots
exit "$veil_test_exit_code"
