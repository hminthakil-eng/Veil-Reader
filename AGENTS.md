# Veil Reader Agent Instructions

## Default development workflow

For implementation, bug fixes, refactors, tests, and repository changes, start with:

`.agents/skills/veil-efficient-task-runner/SKILL.md`

It should route to specialist skills only when the task requires them. Do not load every skill for every task.

## Specialist routing

- Substantial Android/Kotlin feature or refactor: `.agents/skills/veil-world-class-app-development/SKILL.md`
- Android implementation, lifecycle, Room/DataStore, Readium, persistence: `.agents/skills/veil-android-engineer/SKILL.md`
- Bug/crash/regression/CI failure: `.agents/skills/veil-systematic-debugger/SKILL.md`
- Pull request or final change review: `.agents/skills/veil-code-reviewer/SKILL.md`
- Compose UI/architecture/performance/accessibility quality: `.agents/skills/veil-compose-quality-reviewer/SKILL.md`
- Security/privacy/import/backup/URI/permission/dependency risk: `.agents/skills/veil-security-privacy-reviewer/SKILL.md`
- Product/UX direction: `.agents/skills/veil-reader-product-designer/SKILL.md`
- Release readiness: `.agents/skills/veil-release-guardian/SKILL.md`

For deep generic Kotlin/Compose reference material when the project-specific skills are insufficient, consult the retained reference pack at:

`.codex/skills/compose-kotlin-agent-skills/SKILL.md`

Project-specific skills and the checked-in codebase take precedence over generic prescriptions.

## Project priorities

- Reading experience first; keep it calm and distraction-free.
- Preserve offline-first behavior and user data durability.
- Avoid regressions in EPUB/PDF reading, position restoration, annotations, bookmarks, backup/restore, and migrations.
- Keep gamification ethical and non-punitive.
- Prefer incremental, testable changes over broad rewrites.
- Reuse existing dependencies and architecture unless a demonstrated problem requires change.
- Treat imported files, backups, URIs, and user reading data as security/privacy-sensitive inputs.
- Respect existing CI and release gates before calling work complete.

## Efficiency rules

- Inspect the smallest relevant code path first.
- Do not repeat repository discovery that has already established the needed context.
- Do not ask for routine confirmation when the task is clear and reversible.
- Run focused checks before expensive full checks, then broaden verification according to risk.
- Never claim a test/build/review passed unless there is evidence it was run.
- Keep final task reports concise: what changed, what was verified, and any real remaining risk.
