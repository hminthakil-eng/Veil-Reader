# Verification contract

## Automated gates

Required before promotion:

- `gradle :app:testDebugUnitTest --stacktrace`
- `gradle :app:lintDebug --stacktrace`
- `gradle :app:assembleDebug --stacktrace`
- `gradle :app:assembleDebugAndroidTest --stacktrace`
- Reader regression tests
- `git diff --check`

The repository Android CI already runs unit tests, lint and debug/release builds on pull requests. This branch adds material-specific JVM contracts for profile differentiation, physics, geometry, mirror behavior, reduced motion and Slide identity.

## Material engine contracts

Automated checks cover:

- all canonical material presets exist;
- material profiles are structurally distinct;
- drag response is monotonic and never outruns touch;
- heavy parchment resists early pull more than glossy stock;
- material-specific release thresholds differ;
- geometry remains finite across the turn;
- deep curl contains both front/back regions;
- left-edge geometry mirrors without changing material state;
- extreme diagonal drag stays binding-constrained;
- sensory identities differ by material;
- rollout remains off by default and reversible.

## Existing Reader regressions intentionally reused

Existing suites continue to protect:

- one-hot mode ownership across Paper/Slide/Paged/Scroll;
- PDF exclusion from EPUB page-turn ownership;
- LTR/RTL physical edge mapping;
- deliberate gesture thresholds;
- renderer ownership under selection/accessibility;
- hardware key routing;
- durable-close and locator transaction behavior in Reader suites.

## Production-adjacent review matrix

Material previews:

- idle;
- slight corner lift;
- medium drag;
- deep curl;
- almost complete;
- complete release;
- cancel release;
- fast flick;
- slow heavy drag;
- glossy;
- matte;
- parchment;
- papyrus;
- manuscript;
- aged;
- clean/new;
- LTR;
- RTL;
- Persian;
- dark;
- sepia;
- reduced motion.

Slide previews:

- idle;
- active transition;
- completed transition;
- RTL;
- large text;
- Persian.

## Real-device gates still required

Source-level verification cannot reliably decide:

- whether the mesh feels heavy rather than rubbery;
- whether 26 strip mappings are stable at 60/90/120 Hz;
- whether fold shading is too visible under OLED/very low brightness;
- whether papyrus fibre is perceptible without becoming noisy;
- whether the implemented material-specific sound synthesis is elegant across phone speakers/headphones;
- whether the implemented material-specific system haptic profiles feel distinct across OEM vibration motors;
- whether rapid alternating turns cause thermal or bitmap-pressure issues.

These are legitimate hands-on judgments. They are the reason the production rollout flag remains off.
