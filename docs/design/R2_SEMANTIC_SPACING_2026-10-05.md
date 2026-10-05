# R2.01 / R2.02 — Semantic Spacing Foundation

Date: 2026-10-05

Veil already had a valid 4dp/8dp spacing scale. The problem was not the absence of
tokens; it was screen code inventing local values and using scale names without
communicating intent.

This slice keeps the existing canonical scale and adds semantic aliases:

- Micro = 4dp
- Inline = 8dp
- Cluster = 12dp
- Content = 16dp
- Section = 24dp
- Realm = 32dp
- ScreenCompact = 16dp
- ScreenWide = 24dp
- ScreenLarge = 32dp

No second spacing system is introduced.

Migration rule:
- new layout code uses semantic aliases;
- existing xxs..xxxl remain until touched;
- optical exceptions must be local and justified;
- touch targets remain owned by ArenaGeometry, not spacing.

Next consumers: Threshold return-speed pass and Archive density pass.
