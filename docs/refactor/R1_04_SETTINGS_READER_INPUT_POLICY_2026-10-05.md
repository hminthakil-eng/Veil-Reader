# R1.04 Slice 1 — Settings Reader Input Policy

Date: 2026-10-05

Extracted pure reader-input decisions from `SettingsScreen.kt`:
- Material Page debug override semantics;
- tap-action cycle order;
- compact tap-action glyph mapping.

No DataStore write, settings callback, Composable layout, accessibility semantics,
or Reader input ownership moved.

This boundary lets the Settings UI be redesigned later without silently changing
the user's tap-grid behavior.
