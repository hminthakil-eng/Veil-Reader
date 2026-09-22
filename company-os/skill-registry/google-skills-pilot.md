# Google Agent Skills — Controlled Pilot

Status: P0 controlled pilot
Owner: SID / Veil Reader Engineering / Security & QA
Upstream: https://github.com/google/skills
License: Apache-2.0

## Company decision

Google's official Agent Skills repository is approved as a **trusted upstream source**, not as an automatically trusted execution source.

No bulk install, no automatic skill execution, no production dependency, and no copying of the whole upstream repository into Veil Reader.

## Operating rule

Observe → Predict → Reuse → Integrate → Automate → Build → Verify → Learn.

For every upstream skill:

1. Identify a concrete task that benefits from the skill.
2. Check for duplication with existing Veil/Eyad instructions and tooling.
3. Pin upstream repository + path + reviewed commit SHA before adoption.
4. Review SKILL.md, scripts, shell commands, network access, package installs, credentials, file writes, and destructive operations.
5. Run only in a disposable/reversible environment first.
6. Record evidence from the pilot.
7. Promote to the allowlist only after QA/security approval.
8. Never expose production secrets to an unreviewed skill.

## Initial pilot scope

The first Veil Reader pilot is deliberately narrow:

- Google Skill Finder
- Google Developer Knowledge
- selected reliability/security guidance only when directly relevant

Android-specific skills are evaluated from Google's separate official Android skills upstream rather than duplicated here.

## Acceptance gates

A skill may be promoted only when all are true:

- provenance is official and pinned;
- license is compatible;
- no hidden or unnecessary network/file/credential access;
- no conflicting project architecture or policy;
- no material duplication with existing company skills;
- instructions are deterministic enough to reproduce;
- task outcome is measurably better than the existing workflow;
- rollback is trivial;
- no production merge occurs without explicit approval.

## Current restrictions

- Do not auto-sync upstream changes.
- Do not execute arbitrary scripts from upstream.
- Do not grant cloud credentials merely because a skill requests them.
- Do not install Google Cloud/GKE/Ads skills unless a real project task requires them.
- Do not merge this pilot to main until the pilot evidence is reviewed.

## First validation task

Use the approved developer-knowledge/finder subset on one bounded Veil Reader engineering question, compare the result against the current workflow, and record:
- correctness,
- time saved,
- duplication,
- security footprint,
- reproducibility,
- rollback path.

