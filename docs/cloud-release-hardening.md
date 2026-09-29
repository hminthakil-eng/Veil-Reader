# Cloud execution for release blocker #262

Use branch `release/reader-hardening-rc` (PR #263) and Codemagic workflow
`veil-reader-release-hardening` / **Veil Reader Release Hardening — Cloud ADB**.
The separate `ci/codemagic-release-pilot` Firebase workflow is not this route.

The build remains on Codemagic M2; the emulator runs on emulator.wtf and is
attached through its supported ADB session. The existing
`:app:generateBaselineProfile -Pveil.profile.connected=true` task creates the
sources. No Firebase key, local Mac emulator, new Gradle plugin, or Windows
remote-control connection is required.

## Account boundary

In Codemagic's environment-variable group `emulator_wtf`, configure:

| Variable | Value |
| --- | --- |
| `EW_API_TOKEN` | Existing emulator.wtf API token; mark Secret |
| `VEIL_CLOUD_FREE_QUOTA_CONFIRMED` | `yes`, only after checking sufficient unused free quota in both accounts |

Do not put credentials in Git or chat. A confirmation flag is an operator
attestation, not a billing API check or a spending cap. Start this workflow
manually only after checking quotas. No automatic cloud trigger is configured.
The cloud session has a 30-minute maximum; the build has a 60-minute maximum.

## Two executions, one final verified commit

1. Start the workflow on the hardening branch. The first generation produces
   real source profiles and saves copies plus the source SHA under
   `profile-evidence/`. With no profiles committed yet, the committed-source
   gate intentionally fails before release packaging. This is a profile
   refresh result, not release success.
2. Download those profile artifacts and inspect the generation log and rules.
   Commit only the authoritative `app/src/main/generated/baselineProfiles/`
   or `app/src/release/generated/baselineProfiles/` source files to PR #263.
   Retain generation provenance in the PR evidence.
3. Run the workflow again on that new exact SHA. Regenerated sources must match
   HEAD byte-for-byte. Staged-only files, changed sources, empty profiles and
   stale/debug locations are rejected.
4. Clean outputs, execute policy/parser checks, verifier fixtures, unit tests,
   Room schema check, lint, debug/benchmark builds, release APK/AAB builds,
   offline manifest audit, and package inspection. Artifacts and source hashes
   are recorded with the final SHA in `release-evidence.txt`.

Source fixtures are synthetic verifier tests, not device-generation evidence.
The APK must contain `assets/dexopt/baseline.prof` and the AAB must contain
`BUNDLE-METADATA/com.android.tools.build.profiles/baseline.prof`, each nonempty
and below 1,500,000 bytes. Never close #262 or promote #261 using only a refresh
run or fixture results. Existing Windows release requirements remain outstanding
until satisfied or explicitly changed by the release owner; this route does
not silently waive them. Production signing is a separate existing release gate.

## References

- Codemagic: https://docs.codemagic.io/specs-macos/xcode-26-5/
- Cloud ADB sessions: https://docs.emulator.wtf/integrations/cli/
- Baseline Profile support: https://docs.emulator.wtf/integrations/gradle-plugin/
- Android limitations: https://developer.android.com/topic/performance/baselineprofiles/overview
