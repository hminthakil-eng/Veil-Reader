# Veil Reader Technology Radar — 2026 Q3

Status date: 2026-09-20

This radar converts current platform research into explicit engineering decisions.
Statuses: ADOPT, ADOPT-LATER, PILOT, HOLD, REPLACE, REJECT.

## Platform baseline

| Technology | Status | Decision |
|---|---|---|
| Native Android | ADOPT | Veil remains Android-native. Do not introduce KMP until a real second platform exists. |
| Android API 37 / Android 17 | ADOPT | Keep compileSdk/targetSdk 37. Adaptive/resizable behavior is a first-class product requirement. |
| AGP 9.4.0 | ADOPT | Current project baseline; official API 37 support. |
| Gradle 9.6.0 | ADOPT | Required/default line for AGP 9.4. |
| JDK 17 | ADOPT | Stable Android toolchain baseline. |
| Kotlin 2.3.21 | KEEP FOR P7 | Current verified compiler. |
| Kotlin 2.4.20 | PILOT | Current Kotlin release and Readium 3.4 source-toolchain line. Upgrade only behind full CI/regression gate. |

## UI, navigation and design

| Technology | Status | Decision |
|---|---|---|
| Jetpack Compose 1.10.x | ADOPT | Primary UI toolkit. |
| Material 3 1.4.0 | ADOPT | Platform behavior/component foundation, not Veil's visual identity. |
| Material 3 Adaptive 1.3.0 | ADOPT | Stable list-detail/supporting-pane/window-posture behavior for phone/tablet/foldable/ChromeOS. |
| Material 3 Adaptive 1.4 alpha | HOLD | No production dependency until stable unless a blocking bug requires a controlled pilot. |
| Navigation 3 1.1.x | ADOPT-LATER | Replace manual boolean route state after P7 acceptance; integrate with adaptive scene strategies. |
| Veil Design System | ADOPT | Own tokens/components/visual QA. M3 is the primitive layer, not the final look. |
| Official Compose screenshot tests | HOLD/PILOT | Valuable, but current preferred AGP test-suite path is still experimental and wants AGP 9.5 alpha. |
| Roborazzi | PILOT | Candidate for stable-AGP visual regression until official screenshot test suites stabilize. |
| Generic dashboard/glass UI | REJECT | Conflicts with premium reader identity and reading-first design. |

## Reader foundation

| Technology | Status | Decision |
|---|---|---|
| Readium Kotlin Toolkit 3.4.0 | ADOPT | Primary publication toolkit for EPUB/PDF; also TTS/audio/image publication capabilities. |
| Custom EPUB engine | REJECT | No reason to rebuild parsing/navigation/rendering already solved by Readium. |
| Readium EpubNavigatorFragment | ADOPT | EPUB visual navigator behind Veil reader adapter. |
| Readium PdfNavigatorFragment/PDFium | ADOPT WITH CONTAINMENT | Keep PDF implementation behind a PDF adapter. UI must not depend directly on internal PDFView details. |
| Readium ImageNavigator / Divina | PILOT | Useful for CBZ/image publications; validate against Veil Manga Hub requirements. |
| Readium TtsNavigator | ADOPT-LATER | Primary EPUB read-aloud foundation. |
| Readium AudioNavigator | PILOT | Evaluate for audiobooks before adding another playback engine. |
| Custom GPU page-curl overlay | HOLD | Device QA proved input interception risk. No production use until navigation remains authoritative and input-transparent. |
| Visual-only page-turn effects | ADOPT | Effects may decorate a committed navigation transition; they never own reader state. |

## Persistence and files

| Technology | Status | Decision |
|---|---|---|
| Room 2.8.5 | ADOPT | Structured local SSOT for durable entities. |
| Preferences DataStore 1.2.1 | ADOPT | Keep for simple user settings during P7. |
| Proto/typed DataStore | PILOT | Consider for Settings v2 when schema complexity justifies typed/versioned settings. |
| SharedPreferences durable domain state | REPLACE | Game/progression data must move to Room/event-backed state. |
| App-private publication storage | ADOPT | Imported files remain durable without source URI permissions. |
| Content-addressed SHA-256 identities | ADOPT | Deduplication and future file sync integrity. |
| Atomic backup bundle v2 | ADOPT-LATER | Versioned manifest, hashes, staged restore, validation, rollback. |

## Background work and sync

| Technology | Status | Decision |
|---|---|---|
| WorkManager 2.11.2 | ADOPT | Durable indexing, downloads, sync, cover extraction and maintenance tasks. |
| Network as SSOT | REJECT | Core reader remains offline-first. |
| Room local SSOT + sync outbox | ADOPT-LATER | All cloud sync is downstream from local durable state. |
| Provider-specific sync logic in UI | REJECT | Sync uses a provider-neutral SyncTransport boundary. |
| Credential Manager / passkeys | ADOPT-LATER | Use if/when optional accounts/sync ship. |
| Android Keystore | ADOPT-LATER | Protect auth/session/encryption keys; never hardcode secrets. |

## Search and indexing

| Technology | Status | Decision |
|---|---|---|
| Readium publication search | ADOPT | In-book textual search for supported EPUB/FXL. |
| AppSearch LocalStorage | PILOT | Candidate for cross-book/offline index, notes/highlights and future semantic search. Validate Persian tokenization/quality. |
| Room FTS | FALLBACK/PILOT | Retain as fallback if AppSearch language behavior is insufficient. |
| Main-thread text extraction/indexing | REJECT | Extraction/index builds run off-main and are resumable. |

## Images, manga and remote catalogs

| Technology | Status | Decision |
|---|---|---|
| Coil 3.6.3 | ADOPT-LATER | Cover/image pipeline and Manga/Webtoon image loading/caching. |
| Paging 3.5.1 | ADOPT-LATER | Large libraries, manga catalogs, chapter/source lists. |
| MangaSourceProvider SPI | ADOPT | Veil-owned provider boundary; adapters isolated per source. |
| Direct Kotatsu GPL code reuse | REJECT by default | Reference architecture/features only unless GPL implications are explicitly accepted. |
| Compose Webtoon reader | ADOPT-LATER | Continuous vertical image reader, independent of EPUB navigator. |
| CBZ/Divina via Readium | PILOT | Reuse Readium where it fits offline image publications. |
| Unreviewed scraper plugins | REJECT | Every source adapter passes security/license/domain/compatibility review. |

## Audio and accessibility

| Technology | Status | Decision |
|---|---|---|
| Android native TTS via Readium TtsNavigator | ADOPT-LATER | Offline/read-aloud baseline. |
| Media3 1.11.x | PILOT | Use only if needed for audiobook MediaSession/background controls after overlap review with Readium. |
| TalkBack semantics | ADOPT | Release gate. |
| Reduced motion | ADOPT | Release gate. |
| Dyslexia-friendly fonts | ADOPT-LATER | Reader accessibility feature. |
| Line guide / reading ruler | ADOPT-LATER | Reader accessibility feature. |

## Performance

| Technology | Status | Decision |
|---|---|---|
| R8 release optimization | ADOPT | Required for performance/size validation. |
| Baseline Profiles | ADOPT | Project already has module; expand critical journeys. |
| Macrobenchmark | ADOPT | Startup, library scrolling, opening, page turns, PDF zoom, search. |
| Android Vitals | ADOPT-LATER | Field startup/jank/ANR quality once distributed. |
| Debug-build visual performance judgement | REJECT | Performance acceptance must use release/benchmark builds. |
| Per-frame bitmap allocation in reader animation | REJECT | Animation hot paths must be allocation-conscious. |

## Dependency injection and modularity

| Technology | Status | Decision |
|---|---|---|
| Dagger/Hilt + AndroidX Hilt 1.4 | ADOPT-LATER | Introduce during module split; useful for ViewModels, repositories, WorkManager. |
| Service locator from composables | REJECT | UI does not construct repositories/engines. |
| One giant :app module | REPLACE GRADUALLY | Move to bounded core/reader/feature modules without a big-bang rewrite. |
| Gradle version catalog + convention plugins | ADOPT-LATER | Centralize versions/build rules during modularization. |

## Security and privacy

| Technology | Status | Decision |
|---|---|---|
| HTTPS-only production traffic | ADOPT | Cleartext disabled except explicit localhost/debug needs. |
| Network Security Configuration | ADOPT-LATER | Central policy when network features ship. |
| Android Keystore | ADOPT-LATER | Keys/tokens only. |
| Optional account | ADOPT | Core reading never requires login. |
| Consent-based diagnostics | ADOPT-LATER | No hidden telemetry. |
| Secrets in repo/chat | REJECT | Always. |
| Certificate pinning by default | HOLD | Use only with an operational rotation plan; standard TLS/CT is safer than brittle pinning in many cases. |

## AI and intelligence

| Technology | Status | Decision |
|---|---|---|
| AI in core reading path | REJECT | Reader must work fully without AI. |
| ML Kit GenAI / Gemini Nano | PILOT-LATER | On-device privacy/cost benefits; device/language availability must be checked dynamically. |
| LiteRT-LM / custom on-device model | PILOT-LATER | Candidate for broader local AI where ML Kit turnkey APIs do not fit. |
| Cloud LLM provider | OPTIONAL LATER | Explicit opt-in; book content boundaries and privacy controls required. |
| AppFunctions / Android MCP | HOLD | Experimental preview; track for future OS-agent integration. |
| Spoiler-safe book intelligence | ADOPT AS ARCHITECTURAL REQUIREMENT | Any AI feature receives only content up to the user's allowed reading boundary unless explicitly overridden. |

## Explicit non-decisions

- Do not migrate to Kotlin Multiplatform just because libraries support it.
- Do not add cloud sync until local durability, backups and conflict semantics are proven.
- Do not build a custom foundation reader engine.
- Do not ship experimental GPU page-turn code because it looks impressive in isolation.
- Do not adopt alpha AndroidX components in production without a blocking need and rollback path.
- Do not couple Veil's domain model to a specific backend, manga source, AI provider or reader implementation.
