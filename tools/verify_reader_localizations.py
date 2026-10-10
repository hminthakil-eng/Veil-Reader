#!/usr/bin/env python3
"""Fail-closed Android English/Persian string resource and format-contract check.

No third-party libraries, Gradle plugins, translation service or network calls.
Run before expensive Android CI builds. This is a static contract, not a claim
that actual RTL layout, TalkBack behavior or translation quality is perfect.
"""
from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ENGLISH = ROOT / "app/src/main/res/values/strings.xml"
PERSIAN = ROOT / "app/src/main/res/values-fa/strings.xml"
# Java/Android Formatter tokens: explicit %1$s, implicit %s, %% and %n.
# Preserve argument indexes and conversion types; indexes may be reordered.
FORMAT = re.compile(
    r"%(?:([1-9]\d*)\$)?([-#+ 0,(<]*)(?:\d+)?(?:\.\d+)?([tT]?)([a-zA-Z%])"
)


def read_strings(path: Path) -> tuple[dict[str, tuple[str, bool]], list[str]]:
    errors: list[str] = []
    values: dict[str, tuple[str, bool]] = {}
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        return values, [f"{path}: invalid or unreadable XML: {type(exc).__name__}: {exc}"]
    if root.tag != "resources":
        return values, [f"{path}: root element must be <resources>"]
    for node in root:
        if node.tag != "string":
            continue  # Keep existing plurals/arrays outside this bounded first gate.
        name = node.attrib.get("name", "")
        if not re.fullmatch(r"[A-Za-z][A-Za-z0-9_]*", name):
            errors.append(f"{path}: missing or invalid string resource name {name!r}")
            continue
        if name in values:
            errors.append(f"{path}: duplicate string name {name}")
            continue
        translatable = node.attrib.get("translatable", "true").lower()
        if translatable not in ("true", "false"):
            errors.append(f"{path}: {name} has invalid translatable attribute")
            continue
        # itertext handles Android styled strings with nested <b>/<i> tags.
        text = "".join(node.itertext())
        values[name] = (text, translatable == "true")
    return values, errors


def format_signature(value: str) -> Counter[tuple[int, str]]:
    """Logical argument index+type, order-independent for explicit %n$ tokens.

    Implicit formats remain positional, so accidentally swapping %s and %d is
    caught. Repeated explicit indexes are counted, also catching lost placeholders.
    Literal %% and newline %n are never formatting arguments.
    """
    arguments: Counter[tuple[int, str]] = Counter()
    next_implicit = 1
    previous_index: int | None = None
    for match in FORMAT.finditer(value):
        explicit, flags, date_prefix, conversion = match.groups()
        if not explicit and conversion in ("%", "n") and not date_prefix:
            continue
        if "<" in flags and explicit is None:
            if previous_index is None:
                # Invalid reusable-previous syntax; keep a sentinel mismatch.
                arguments[(0, "invalid-previous")] += 1
                continue
            index = previous_index
        elif explicit:
            index = int(explicit)
        else:
            index = next_implicit
            next_implicit += 1
        previous_index = index
        # Upper/lower-case versions have the same argument compatibility.
        arguments[(index, (date_prefix + conversion).lower())] += 1
    return arguments


def audit(base: Path = ENGLISH, translation: Path = PERSIAN) -> list[str]:
    source, errors = read_strings(base)
    localized, localized_errors = read_strings(translation)
    errors.extend(localized_errors)
    if errors:
        return errors

    for name in sorted(source.keys() - localized.keys()):
        if source[name][1]:
            errors.append(f"{translation}: missing Persian translation for {name}")
    for name in sorted(localized.keys() - source.keys()):
        errors.append(f"{translation}: unknown string {name}, absent in default values")

    for name in sorted(source.keys() & localized.keys()):
        english, en_translate = source[name]
        persian, fa_translate = localized[name]
        if en_translate != fa_translate:
            errors.append(f"{name}: inconsistent translatable=true/false across locales")
        if not en_translate:
            continue
        en_args = format_signature(english)
        fa_args = format_signature(persian)
        if en_args != fa_args:
            errors.append(
                f"{name}: formatting arguments differ: English={sorted(en_args.items())} "
                f"Persian={sorted(fa_args.items())}"
            )
    return errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base", type=Path, default=ENGLISH)
    parser.add_argument("--persian", type=Path, default=PERSIAN)
    args = parser.parse_args(argv)
    problems = audit(args.base, args.persian)
    if problems:
        for problem in problems:
            print(f"ERROR: {problem}", file=sys.stderr)
        print(f"FAIL: {len(problems)} resource contract violation(s)", file=sys.stderr)
        return 1
    print("PASS: Android EN/FA string names and formatting contracts match.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
