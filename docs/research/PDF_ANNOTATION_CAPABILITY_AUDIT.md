# PDF annotation capability audit

Audited 2026-10-03 against Veil commit `7d3d7e289e7e2dd895a8bf8772587ee78dd8ce0d` (W36–W39). This is a capability investigation, not an annotation implementation. Readium/publication state, locator persistence and the existing Pdfium renderer remain authoritative.

## W41 follow-through

The baseline findings below describe W39 exactly. W41 now opts into existing annotation rendering
using the public provider listener, routes internal PDF links through Readium document locators,
defers app chrome until native link hit testing finishes, and labels bookmarks with document pages.
No generic annotation list, authoring, text-selection, mutation or writer capability was added.
See [W41 verification](../handoffs/W41_PDF_RELIABILITY.md) for executed checks and pending device QA.

## Decision

The current stack can render annotations already embedded in a PDF through a public configuration hook. It cannot currently expose generic annotation enumeration, authoring, text selection or saving through its public Java/Kotlin Reader API. The bundled native engine has relevant entry points, so an owned JNI/adapter extension is a plausible next investigation; replacing the engine is not justified by this audit.

| Category | Capability | Exact boundary |
| --- | --- | --- |
| A — supported immediately | Existing embedded annotation rendering | `PdfiumEngineProvider.Listener.onConfigurePdfView` can call `PDFView.Configurator.enableAnnotationRendering(true)` before loading. This hook is public, with Readium's experimental API opt-in. Veil currently supplies no listener; AndroidPdfViewer defaults this flag to false. Availability is proven by source/ABI, not a runtime visual pass. No annotation editing or comment list is implied. |
| A | PDF links, metadata and outline | Pdfium exposes link records, metadata and TOC; AndroidPdfViewer handles links. Link annotations are a specific supported subtype, not a generic annotation API. Existing navigation still owns taps. |
| A | Durable app bookmarks and reading positions | Existing Readium locators, Veil PDF locator-version migration and app bookmark records already provide persistence. These are app records, not embedded PDF annotations. |
| B — adapter/engine extension required | Enumerate embedded annotations, subtype, bounds, contents and colors | Native PDFium exports annotation APIs, but PdfiumAndroid's Java/JNI wrapper and Readium's document interface do not expose them. Requires owned handle/lifetime bridge and subtype-specific decoding. |
| B | Text extraction/selection and anchored highlights, underline or strikeout | Native text and attachment-point APIs exist. Current PDF navigator does not implement `SelectableNavigator` or `DecorableNavigator`. Requires text geometry, real quad coordinates, selection ownership and accessibility integration. Scanned PDFs do not gain text from these APIs. |
| B | Ink, note creation, editing and removal | Native creation/removal/ink/string APIs exist. Requires owned JNI bindings, supported-subtype policy, mutation serialization and renderer invalidation. Export presence does not prove every annotation subtype works. |
| B | PDF-native save/export and durable annotation identity | `FPDF_SaveAsCopy` is exported. No current Java/Readium writer exists. Requires atomic save-copy, reopen/verification, rollback, identity/fingerprint policy and app metadata reconciliation. |
| B | Reliable page geometry for annotation tools | Public low-level coordinate conversion exists, but current Readium API does not expose its live PDFView/document handles or complete renderer transform. Needs adapter-owned document-page/view-page mapping, rotation/crop/zoom/scroll geometry and managed lifetime. |
| C — unsafe/fake; do not implement | Viewport rectangles advertised as PDF highlights; EPUB decoration fallback advertised as PDF support | Screen coordinates are not durable PDF quads. An app overlay does not edit or export a PDF. Capability casts must remain honest. |
| C | Reflection/private native-pointer borrowing or reusing navigator documents | Readium's Pdfium handles are internal API; its reset path explicitly opens a new document because reusing a PdfDocument can crash PDFium. No cached shared handle shortcuts. |
| C | Silent in-place writes while publication/rendering is open; claims of saved annotations backed only by Room/UI state | Current imported documents are opened read-only. Concurrent save/mutation/close and unverified persistence would violate ownership and durable state. |
| C | Fake selectable text on scans, blanket subtype/form/signature support or native-symbol presence presented as end-to-end support | OCR is a separate capability; signatures/forms/security-sensitive edits and subtype fidelity need independent evaluation. Native ABI evidence is not functional verification. |

## Exact resolved stack and evidence

All artifacts were inspected from the dependencies resolved by the verified W39 build, rather than newer upstream documentation.

| Artifact | Version | AAR SHA-256 |
| --- | --- | --- |
| Readium Pdfium document adapter | 3.4.0 | `e4e33db9b7771995e5651016f571b8e213fbcba37239ebd5ca1525d576405073` |
| Readium Pdfium navigator adapter | 3.4.0 | `7b8dcf75bbb9b27df9a87e98352f56c17ac54022b467fbf2c9a5a86cefc4915d` |
| `com.github.marain87:AndroidPdfViewer` | 3.2.8 | `b9c7aaaf46119c47fdef2660d5f384f5f3443a8569f84f1917d885447b476833` |
| `com.github.marain87:PdfiumAndroid` | 1.9.8 | `8897d2ad45d59b233a44c5493f8c1055f3c843839d86e3e73aa7c8e3fc3371c2` |

Readium shared/navigator/streamer resolve to 3.4.0. Matching Maven Central/JitPack source JARs and `javap -public` of the resolved AAR classes agree:

- `PdfDocument` exposes page count, metadata, outline, cover and close, with no annotation/text/writer surface.
- `PdfNavigatorFragment` implements visual/overflow/configuration navigation, not selection/decorations. `PdfiumNavigatorFragment` is a typealias to that navigator.
- `PdfiumCore` exposes page open/render, metadata, links, outline and coordinate transforms. Its public ABI has no generic annotation CRUD, text-query or save API. JNI has no corresponding annotation/text-query/save bindings.
- AndroidPdfViewer's configurator passes `annotationRendering` through its rendering handler to the native render flag. PdfiumDocument cover generation passes false separately, so enabling Reader annotation rendering does not automatically change cover rendering.
- `PdfiumDocumentFactory` uses a read-only file descriptor; document identifiers are derived from file/byte MD5. Veil also tracks imported content fingerprints. A save changes content identity and must have an explicit reconciliation policy.

Source locations in matching source JARs: `org/readium/adapter/pdfium/navigator/{PdfiumEngineProvider,PdfiumDocumentFragment,PdfiumNavigatorFragment}.kt`; `org/readium/adapter/pdfium/document/{PdfiumDocument,PdfiumDocumentFactory}.kt`; shared `org/readium/r2/shared/util/pdf/PdfDocument.kt`; `com/github/barteksc/pdfviewer/{PDFView,RenderingHandler,PdfFile}.java`; `com/shockwave/pdfium/PdfiumCore.java`.

Veil integration: [ReadiumEngine.kt](../../app/src/main/java/com/veilreader/app/data/ReadiumEngine.kt), [ReaderScreen.kt](../../app/src/main/java/com/veilreader/app/ui/screens/ReaderScreen.kt), [PdfiumLocatorCompat.kt](../../app/src/main/java/com/veilreader/app/data/PdfiumLocatorCompat.kt), [LibraryExport.kt](../../app/src/main/java/com/veilreader/app/data/LibraryExport.kt). Existing backup serialization includes bookmarks/highlights and book files; it is not a native annotation writer.

## Native ABI probe

`readelf --dyn-syms --wide` verified defined symbols in `libpdfium.cr.so` from the pinned PdfiumAndroid AAR for arm64-v8a, armeabi-v7a, x86 and x86_64. All four contain all 18 probed annotation/text/coordinate/save symbols. [Machine-readable results](pdf-annotation-evidence/native-symbols.json) include each library SHA-256 and exact symbol names. This establishes a potential extension substrate, not stable public wrapper support or successful annotation round-tripping.

To reproduce: extract `jni/<abi>/libpdfium.cr.so` from the AAR, compute `sha256sum`, and examine defined dynamic symbols with `readelf --dyn-syms --wide`. Compare the names in the evidence file. Extract `classes.jar` and run `javap -public com.shockwave.pdfium.PdfiumCore` to independently verify the wrapper boundary.

## Ownership and persistence contract for a later B slice

1. Add an explicit adapter capability surface; do not make all VisualNavigators appear selectable/decorable. Keep Readium's locator/current-page and session ownership authoritative.
2. Own document/page/annotation handles inside the adapter. Serialize render, text query, mutation, save and close. Invalidate handles across reset, configuration change and session disposal; never cache the navigator's current PdfDocument in app state.
3. Convert through actual document coordinates and the renderer's complete transform, including crop box, rotation, zoom and scroll. Native conversions require the live page, exact render origin/size and rotation. Keep document-page indices distinct from displayed indices: Readium reverses the displayed list for RTL.
4. Save to a new private temporary file, check return values, reopen and verify annotation/content round-trip before atomic replacement or explicit export. Retain the original on failure. Decide whether edited copies become new books; preserve locator migration and final flush, and reconcile fingerprint/document identifier and backup metadata.
5. Model durable native annotation identity and quads separately from current quote/locator-only highlight records. Never display a saved state until the selected persistence contract succeeds. Bound malformed geometry and subtype data.
6. Preserve pan/zoom, links and selection authority. Suspend authoring during blocking UI, accessibility ownership or session changes. Avoid touch interceptors for passive annotations and Focus Guide.

An integration risk to validate before annotation interaction work: AndroidPdfViewer's default internal-link handler sends a native destination page directly to `PDFView.jumpTo`, while Readium reverses displayed pages for RTL. Source inspection identifies a potential index mismatch; this audit does not claim a runtime regression or alter link handling without a reproducible RTL PDF fixture.

## Verification limits and next concrete work

The W39 unit/build/lint evidence is recorded in [W39 cloud verification](../handoffs/W39_CLOUD_VERIFICATION.md). Instrumentation targets compile; this cloud has no KVM and the software emulator could not sustain package-manager services, so annotation rendering, gestures and save/reopen were not run on a device. No runtime annotation support is claimed here.

A small next implementation can opt into embedded annotation rendering through the existing provider listener, with real annotated/unencrypted/encrypted and rotated/cropped fixtures, all palettes/high contrast, links, zoom/pan, RTL destinations, reset/process recreation and cover expectations verified on a usable device. Native authoring remains a separately scoped adapter extension. Before that extension ships, verify text versus scanned documents, malformed annotations, all packaged ABIs, save failure/rollback, independent-viewer round-trip, configuration/session disposal and preservation of existing signatures/security constraints.
