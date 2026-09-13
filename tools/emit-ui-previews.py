"""Expose only synthetic Compose-test screenshots for review through text-only CI clients."""
from base64 import b64encode, b64decode
from pathlib import Path
import xml.etree.ElementTree as ET
import re

log = Path("app/build/ui-capture.log")
if log.exists():
    folder = Path("app/build/ui-screenshots")
    folder.mkdir(parents=True, exist_ok=True)
    name, chunks = None, []
    for line in log.read_text(errors="replace").splitlines():
        marker = re.search(r"VEIL_SCREENSHOT_(BEGIN|DATA|END) (.+)", line)
        if not marker:
            continue
        kind, value = marker.groups()
        if kind == "BEGIN":
            name, chunks = value, []
        elif kind == "DATA" and name:
            chunks.append(value)
        elif kind == "END" and value == name:
            if re.fullmatch(r"[a-z-]+", name):
                (folder / f"{name}.png").write_bytes(b64decode("".join(chunks), validate=True))
            name, chunks = None, []

for report in sorted(Path("app/build/outputs/androidTest-results").rglob("*.xml")):
    try:
        root = ET.parse(report).getroot()
        print(f"VEIL_TEST_REPORT {report}: {root.attrib}")
        for case in root.iter("testcase"):
            for failure in list(case.findall("failure")) + list(case.findall("error")):
                print(f"VEIL_TEST_FAILURE {case.attrib}")
                print((failure.get("message", "") + "\n" + (failure.text or ""))[:14000])
    except ET.ParseError:
        print(f"VEIL_TEST_REPORT unreadable: {report}")

for screenshot in sorted(Path("app/build/ui-screenshots").glob("*.png")):
    print(f"VEIL_SCREENSHOT_BEGIN {screenshot.stem}")
    data = b64encode(screenshot.read_bytes()).decode("ascii")
    for offset in range(0, len(data), 4000):
        print(f"VEIL_SCREENSHOT_DATA {data[offset:offset + 4000]}")
    print(f"VEIL_SCREENSHOT_END {screenshot.stem}")
