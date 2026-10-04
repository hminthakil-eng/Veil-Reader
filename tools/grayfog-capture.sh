#!/usr/bin/env bash
# Debug-only fictional shell fixtures. Publication/device correctness needs the real Reader.
set -euo pipefail
review_adb=${ADB:-adb}
review_output=${1:-grayfog-device-captures}
mkdir -p "$review_output"
"$review_adb" get-state >/dev/null
"$review_adb" shell pm path com.veilreader.app >/dev/null
review_device=$("$review_adb" shell getprop ro.product.model | tr -d '\r')
printf '%s\n' "$review_device" > "$review_output/device.txt"
review_surfaces=(THRESHOLD_ACTIVE THRESHOLD_PERSIAN_LONG THRESHOLD_EMPTY LIBRARY_GALLERY LIBRARY_SHELVES LIBRARY_INDEX LIBRARY_SEARCH LIBRARY_NO_RESULTS LIBRARY_EMPTY LIBRARY_MANY LIBRARY_MISSING_METADATA BOOK_DETAIL BOOK_DETAIL_PERSIAN BOOK_DETAIL_MISSING APPEARANCE_QUICK APPEARANCE_ADVANCED SETTINGS NOTES NOTES_EMPTY HIGHLIGHTS BOOKMARKS OBSERVATORY_ISOLATED OBSERVATORY_DENSE CASTLE_LOW CASTLE_ADVANCED PATH RITUAL LOADING ERROR SANCTUM_LOCKED SANCTUM_POPULATED PROFILE)
for review_locale in en fa; do
  for review_scale in 1.0 1.3 1.5 2.0; do
    for review_surface in "${review_surfaces[@]}"; do
      review_name="${review_surface,,}-${review_locale}-${review_scale}-standard"
      "$review_adb" shell am start -W --activity-clear-task --activity-new-task -n com.veilreader.app/.ui.review.GrayfogReviewActivity \
        --es surface "$review_surface" --es locale "$review_locale" --ef fontScale "$review_scale" --ez highContrast false > "$review_output/$review_name.launch.txt"
      grep -q '^Status: ok' "$review_output/$review_name.launch.txt" || { printf '%s\n' "Capture activity failed: $review_name" >&2; exit 1; }
      # Let the finite reveal and font frame settle; never asserts screenshot acceptance.
      sleep 1
      "$review_adb" exec-out screencap -p > "$review_output/$review_name.png"
    done
  done
done
# Explicitly separate high-contrast evidence from ordinary frames.
for review_surface in "${review_surfaces[@]}"; do
  review_name="${review_surface,,}-fa-2.0-contrast"
  "$review_adb" shell am start -W --activity-clear-task --activity-new-task -n com.veilreader.app/.ui.review.GrayfogReviewActivity \
    --es surface "$review_surface" --es locale fa --ef fontScale 2.0 --ez highContrast true > "$review_output/$review_name.launch.txt"
  grep -q '^Status: ok' "$review_output/$review_name.launch.txt" || { printf '%s\n' "Capture activity failed: $review_name" >&2; exit 1; }
  sleep 1
  "$review_adb" exec-out screencap -p > "$review_output/$review_name.png"
done
printf '%s\n' 'Fictional shell fixtures only. Inspect every PNG; no automatic visual acceptance.' > "$review_output/README.txt"
