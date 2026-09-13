# 0.12 Preview — A Reader That Feels Like You

This candidate extends the tested 0.11 preview. The castle campaign, residents, lore, construction and EPUB reading modes remain available.

## What changes

- Optional welcome guide with reading-and-world or quiet-reading choices, a direct library action, and backup restore. Existing populated libraries skip setup; importing a book from Android Files still opens it directly.
- Profile → Settings & reading comfort: General, Reading, and Backups & data. Each section survives activity recreation and Android Back returns to the previous tab.
- Quiet mode hides Castle and Path navigation, home quests, XP, sigils and profile lore. It preserves the estate and continues accumulating engaged reading. Library, notes and backup remain available.
- App light/dark/system appearance is independent from book paper color. A real Compose text sample previews EPUB size, font, line spacing, margins and alignment. Publisher styling respects original alignment; the Book preset resets the font.
- Reduced motion reaches navigation, cover fades, world reveals, construction crossfades and reader chrome. Android’s own animation setting still applies. Native PDF page gestures retain their renderer’s behavior.
- PDF’s Appearance action now opens comfort options, with EPUB-only controls clearly omitted.
- Bottom navigation takes its measured height, wraps labels, and exposes selection to accessibility services. Compact landscape uses a scrolling rail.
- App choices join the existing ZIP backup. New optional keys preserve compatibility with schemas 1 and 2; older archives retain current app choices. Migration flags are not exported. Appearance and app choices restore in one DataStore edit. No Room schema or permissions change.

## Verification

Run policy/unit/lint/APK and emulator workflows for the exact commit. New coverage includes settings route recreation, quiet-mode navigation without losing a reader locator, preference reload, estate accrual while quiet, complete preference/estate ZIP round trip and older-backup compatibility.

Compose interaction tests exercise setup selection, quiet/theme switches, scroll/curl selection, restore confirmation and reachable controls at 200% text. Synthetic native screenshots of welcome, settings, typography and large text are attached to the instrumentation report. These are component rendering/interaction checks, not a physical-device reading session or full TalkBack audit. Test setup follows the [Android Compose testing guide](https://developer.android.com/develop/ui/compose/testing).

Initial candidate: local 39-check reading-policy suite and whitespace checks passed; Android CI pending. Exact CI and screenshot results are recorded on PR #39 after the run.

## Install and remaining gates

The debug artifact installs **Veil Reader Preview**, separate from the original app. Export a ZIP backup from the installed version before replacing or reinstalling any preview, then restore it from Settings → Backups & data. CI debug signing keys may change between builds; a persistent, user-controlled release signing pipeline remains a production gate. Keep the installation that holds your only copy of the library until a backup is saved.

3D curl remains beta. Test actual EPUB/PDF books, gestures, RTL, selection, resumed positions and motion on a physical phone before promoting this preview. Full performance measurements and signed-update, accessibility and release gates in `WORLD_CLASS_RELEASE_GATES.md` remain open.
