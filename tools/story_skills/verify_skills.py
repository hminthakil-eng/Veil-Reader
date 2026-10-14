#!/usr/bin/env python3
"""Validate the versioned common fiction-writing skills for Gemini CLI and coding agents."""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SKILLS = ROOT / ".agents" / "skills"
MANIFEST = ROOT / "tools" / "story_skills" / "writing_skill_manifest.json"

def main() -> int:
    m = json.loads(MANIFEST.read_text(encoding="utf-8"))
    assert m["schemaVersion"] == 1
    names = [n for s in m["sources"] for n in s["skills"]]
    assert len(names) == len(set(names)) == 26, "Missing or duplicate skills in manifest"
    for rel in m["licenseCopies"]:
        assert (SKILLS / rel).is_file(), f"Missing retained MIT license {rel}"
    problems = []
    for name in names:
        path = SKILLS / name / "SKILL.md"
        if not path.is_file():
            problems.append(f"MISSING {name}")
            continue
        content = path.read_text(encoding="utf-8-sig")
        if not content.startswith("---\n"):
            problems.append(f"INVALID YAML HEADER {name}")
            continue
        parts = content.split("---", 2)
        header = parts[1]
        declared = re.search(r"(?m)^name:\s*(.+?)\s*$", header)
        described = re.search(r"(?m)^description:\s*(.+?)\s*$", header)
        if not declared or declared.group(1) != name:
            problems.append(f"NAME MISMATCH {name}")
        if not described or len(described.group(1)) < 20:
            problems.append(f"DESCRIPTION INVALID {name}")
        if len(content) > 150000:
            problems.append(f"OVERLARGE {name}")
    if problems:
        for problem in problems:
            print("FAIL", problem)
        return 1
    print(f"PASS: {len(names)} skill headers; source license copies; no duplicate names.")
    print("Note: this does not prove activation inside Gemini desktop or ChatGPT app.")
    return 0

if __name__ == "__main__":
    sys.exit(main())
