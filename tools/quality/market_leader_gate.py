"""Read-only market-leader evidence gate.

Never infers Android, visual or competitor readiness from source code.
stdlib-only; intended to run even when desktop RAM blocks Gradle.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import sys

VALID = {"unmeasured", "measured-pass", "measured-fail", "blocked"}
REQUIRED_MODE_NAMES = ("PAPER_CURL", "SLIDE", "PAGED", "SCROLL")
SOURCE_CHECKS = {
    "four_navigation_modes": (
        "app/src/main/java/com/veilreader/app/domain/Models.kt",
        REQUIRED_MODE_NAMES,
    ),
    "separate_input_owners": (
        "app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt",
        ("PaperCurlInputListener(", "SlideNavigationInputListener(", "StaticPagedNavigationInputListener("),
    ),
    "separate_visual_overlays": (
        "app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt",
        ("PaperCurlOverlay(", "SlidePageOverlay("),
    ),
    "paper_publication_capability": (
        "app/src/main/java/com/veilreader/app/ui/screens/ReaderAppearancePolicy.kt",
        ("shouldMountPaperCurlRuntime(", "!fixedLayout", "ReaderNavigationMode.PAPER_CURL"),
    ),
    "real_epub_ltr_rtl_probe_exists": (
        "app/src/androidTest/java/com/veilreader/app/ui/reader/material/NativePaperRealReaderInstrumentedTest.kt",
        ("realEpub_bendsDuringDrag_cancelRestores_releaseChangesReadiumLocator",
         "persianRtlEpub_bendsFromLeft_cancelRestores_releaseChangesReadiumLocator"),
    ),
}
REQUIRED_GATES = (
    "visual_user_acceptance", "android_device_evidence",
    "current_gradle_unit_tests", "independent_competitor_benchmark",
    "manual_license_review"
)

def evaluate(contract: dict, repo: Path) -> dict:
    errors: list[str] = []
    outcomes = []
    ids = set()
    tests = contract.get("tests", [])
    if not isinstance(tests, list) or not tests:
        errors.append("Missing test matrix")
        tests = []
    for test in tests:
        tid = test.get("id")
        status = test.get("status")
        evidence = test.get("evidence", [])
        if not isinstance(tid, str) or not tid or tid in ids:
            errors.append(f"Invalid or duplicate test id: {tid!r}")
        ids.add(tid)
        if status not in VALID:
            errors.append(f"Unknown status on {tid}: {status!r}")
        if not isinstance(evidence, list):
            errors.append(f"Evidence list invalid on {tid}")
            evidence = []
        if status == "measured-pass" and not evidence:
            errors.append(f"Unproven PASS forbidden for {tid}")
        outcomes.append({"id": tid, "priority": test.get("priority"),
                         "status": status, "evidence_count": len(evidence)})
    gate_map = contract.get("global_gates", {})
    if not isinstance(gate_map, dict):
        errors.append("Global gates invalid")
        gate_map = {}
    for name in REQUIRED_GATES:
        status = gate_map.get(name)
        if status not in VALID:
            errors.append(f"Unknown/missing global gate: {name}")
        if status == "measured-pass" and not contract.get("global_gate_evidence", {}).get(name):
            errors.append(f"Unproven global PASS forbidden: {name}")
    static = []
    for name, (relative, terms) in SOURCE_CHECKS.items():
        path = repo / relative
        text = path.read_text(encoding="utf-8") if path.is_file() else ""
        static.append({"id": name,"source_exists":path.is_file(),
                       "static_match": all(term in text for term in terms),
                       "source_file": relative})
    unresolved = [o["id"] for o in outcomes if o["status"] != "measured-pass"]
    release_ready = (not errors and not unresolved and
                     all(gate_map.get(g) == "measured-pass" for g in REQUIRED_GATES))
    return {
        "gate_version": 1, "release_ready": release_ready,
        "decision": "REVIEW_REQUIRED" if release_ready else "BLOCKED",
        "integrity_errors": errors, "source_checks": static,
        "tests": outcomes, "unresolved": unresolved,
        "global_gates": gate_map,
        "disclaimer": ("Source check matches prove file contents only; "
                       "they are never a substitute for device, visual, "
                       "performance, accessibility or competitor evidence.")
    }

def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    parser.add_argument("--contract", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args(argv)
    contract_path = args.contract or args.repo / "quality/market-leader/benchmark-contract.json"
    data = json.loads(contract_path.read_text(encoding="utf-8"))
    result = evaluate(data, args.repo)
    output = json.dumps(result, indent=2, ensure_ascii=False)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(output + "\n", encoding="utf-8")
    print(output)
    # Missing device proof blocks release but not a report-generation run.
    return 2 if result["integrity_errors"] else 0

if __name__ == "__main__":
    sys.exit(main())
