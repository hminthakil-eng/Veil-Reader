"""Expose only synthetic Compose-test screenshots for review through text-only CI clients."""
from base64 import b64encode
from pathlib import Path

for screenshot in sorted(Path("app/build/ui-screenshots").glob("*.png")):
    print(f"VEIL_SCREENSHOT_BEGIN {screenshot.stem}")
    data = b64encode(screenshot.read_bytes()).decode("ascii")
    for offset in range(0, len(data), 4000):
        print(f"VEIL_SCREENSHOT_DATA {data[offset:offset + 4000]}")
    print(f"VEIL_SCREENSHOT_END {screenshot.stem}")
