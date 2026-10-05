# R2.05–R2.07 — Script Detection Correctness

Date: 2026-10-05

This slice fixes typography correctness before optical size tuning.

## Fixed

- Persian/Arabic-family locale detection now normalizes regional tags such as:
  - fa-IR
  - ar-SA
  - ur_PK
  - ps-AF
  - ckb-IQ
- Arabic Extended-B (U+0870–U+089F) is included in mixed-script detection.

## Non-change

No shell font size, line height, weight, family asset or Reader publication typography is changed.

Optical metric tuning remains gated on current rendered Threshold/Archive evidence so multiple visual variables are not changed blindly at once.
