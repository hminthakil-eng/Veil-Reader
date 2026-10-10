#!/usr/bin/env python3
"""Fail-closed syntax and registry checks for the project-specific Grand Forge skill pack.

This checks skill files and discovery links; it does not execute skills or establish app quality.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
SKILLS = (
    "veil-grand-forge-orchestration",
    "veil-paper-engine-physics-qa",
    "veil-reading-state-durability",
    "veil-visual-regression-accessibility",
    "veil-secure-supply-chain",
    "veil-reading-capabilities-verification",
    "veil-competitor-cleanroom-intel",
    "veil-performance-release-lab",
    "veil-official-android-tooling",
)


def check():
    errors = []
    for name in SKILLS:
        path = ROOT / ".agents" / "skills" / name / "SKILL.md"
        if not path.is_file():
            errors.append(f"missing skill: {path.relative_to(ROOT)}")
            continue
        body = path.read_text(encoding="utf-8")
        if not body.startswith("---\n") or "\n---\n" not in body[4:]:
            errors.append(f"{name}: missing valid YAML frontmatter delimiters")
            continue
        frontmatter = body.split("\n---\n", 1)[0]
        if not re.search(rf"(?m)^name:\s*{re.escape(name)}\s*$", frontmatter):
            errors.append(f"{name}: frontmatter name must match skill directory")
        match = re.search(r"(?m)^description:\s*(\S.+)$", frontmatter)
        if not match or len(match.group(1).strip()) < 25:
            errors.append(f"{name}: descriptive frontmatter is missing")
        if len(body.split("\\n---\\n", 1)[-1].strip()) < 500:
            errors.append(f"{name}: skill instructions are too thin to be actionable")
    bridge = ROOT / ".claude" / "skills" / "veil-grand-forge-orchestration" / "SKILL.md"
    if not bridge.is_file():
        errors.append("Claude skill bridge is missing")
    else:
        content = bridge.read_text(encoding="utf-8")
        for name in SKILLS:
            if f".agents/skills/{name}/SKILL.md" not in content:
                errors.append(f"Claude bridge missing reference: {name}")
    for doc in (
        "docs/research/GRAND_FORGE_SKILLS_PLUGINS_RESEARCH_2026-10-10.md",
        "docs/research/GRAND_FORGE_OFFICIAL_ANDROID_SKILLS_2026-10-10.md",
    ):
        if not (ROOT / doc).is_file():
            errors.append(f"missing research document: {doc}")
    workflow = ROOT / ".github" / "workflows" / "dependency-review.yml"
    if not workflow.is_file() or (
        "grand-forge/p0-kindle-reader-quality-20261008"
        not in workflow.read_text(encoding="utf-8")
    ):
        errors.append("Dependency Review is not scoped to canonical Reader PRs")

    for error in errors:
        print(f"ERROR: {error}", file=sys.stderr)
    if errors:
        print(f"Skill pack validation failed: {len(errors)} issue(s)", file=sys.stderr)
        return 1
    print(f"PASS: {len(SKILLS)} Grand Forge skills, Claude bridge, research and security PR trigger")
    return 0


if __name__ == "__main__":
    raise SystemExit(check())
