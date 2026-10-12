#!/usr/bin/env python3
"""Small, dependency-free regression gate for Veil Reader's Reader Access Dock.

Measures authored opaque background versus normal text and accent/icon color
using WCAG 2.x relative luminance. This is not a screenshot or system-wide UI
accessibility audit; physical device testing remains required.
"""
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
DOCK = ROOT / "app/src/main/java/com/veilreader/app/ui/screens/ReaderAccessDock.kt"
PALETTE = ROOT / "app/src/main/java/com/veilreader/app/ui/theme/ArenaDesignSystem.kt"
MIN_TEXT_CONTRAST = 4.5


def hex_color(source: str, pattern: str) -> tuple[int, int, int]:
    match = re.search(pattern, source)
    if match is None:
        raise AssertionError(f"Color expression missing or changed: {pattern}")
    value = match.group(1)
    return tuple(int(value[index:index + 2], 16) for index in (0, 2, 4))


def luminance(color: tuple[int, int, int]) -> float:
    channels = [component / 255 for component in color]
    linear = [channel / 12.92 if channel <= 0.04045
              else ((channel + 0.055) / 1.055) ** 2.4 for channel in channels]
    return sum(a * b for a, b in zip(linear, (0.2126, 0.7152, 0.0722)))


def contrast(a: tuple[int, int, int], b: tuple[int, int, int]) -> float:
    high, low = sorted((luminance(a), luminance(b)), reverse=True)
    return (high + 0.05) / (low + 0.05)


def verify() -> None:
    assert round(contrast((0, 0, 0), (255, 255, 255)), 2) == 21.0
    assert contrast((17, 34, 51), (17, 34, 51)) == 1.0

    src = DOCK.read_text(encoding="utf-8")
    palette = PALETTE.read_text(encoding="utf-8")
    fn = src.split("internal fun readerAccessColors(theme: ReaderTheme)", 1)[1].split("\n}", 1)[0]
    if ".copy(alpha" in fn:
        raise AssertionError("Reader chrome may not be translucent over unknown book pixels")
    for expr in ("background = if (light)", "foreground = if (light)", "accent = if (light)"):
        if expr not in fn:
            raise AssertionError(f"Reader color contract has changed: {expr}")

    # Actual authored light-background text and icon colors at the call site.
    light_bg = hex_color(fn, r"background = if \(light\) Color\(0xFF([0-9A-Fa-f]{6})\)")
    light_text = hex_color(fn, r"foreground = if \(light\) Color\(0xFF([0-9A-Fa-f]{6})\)")
    light_accent = hex_color(fn, r"accent = if \(light\) Color\(0xFF([0-9A-Fa-f]{6})\)")

    # Dark aliases (Ink, Moon, Brass) resolve to these canonical Arena colors.
    for contract in ("background = if (light) Color(0xFFF0E4CC) else VeilPalette.Ink",
                     "foreground = if (light) Color(0xFF2B241B) else VeilPalette.Moon",
                     "accent = if (light) Color(0xFF795723) else VeilPalette.Brass",
                     "val Ink = VeilMaterials.RealmBackground",
                     "val Moon = VeilMaterials.TextPrimary",
                     "val Brass = VeilMaterials.Brass"):
        theme = (ROOT / "app/src/main/java/com/veilreader/app/ui/theme/Theme.kt").read_text(encoding="utf-8")
        if contract not in (fn if contract.startswith(("background =", "foreground =", "accent =")) else theme):
            raise AssertionError(f"Theme alias or dock token changed: {contract}")

    dark_bg = hex_color(palette, r"val Void = Color\(0xFF([0-9A-Fa-f]{6})\)")
    dark_text = hex_color(palette, r"val Moon = Color\(0xFF([0-9A-Fa-f]{6})\)")
    dark_accent = hex_color(palette, r"val AntiqueGold = Color\(0xFF([0-9A-Fa-f]{6})\)")
    for desc, fg, bg in (
        ("light text", light_text, light_bg),
        ("light accent", light_accent, light_bg),
        ("dark text", dark_text, dark_bg),
        ("dark accent", dark_accent, dark_bg),
    ):
        value = contrast(fg, bg)
        print(f"{desc}: {value:.2f}:1 (minimum {MIN_TEXT_CONTRAST}:1)")
        if value < MIN_TEXT_CONTRAST:
            raise AssertionError(f"Insufficient Reader Access Dock {desc} contrast: {value:.2f}")


if __name__ == "__main__":
    try:
        verify()
    except (AssertionError, OSError, ValueError) as error:
        print(f"Reader chrome contrast check FAILED: {error}", file=sys.stderr)
        sys.exit(1)
