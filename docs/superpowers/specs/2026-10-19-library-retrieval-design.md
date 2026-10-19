# Library retrieval — next subsystem design

Status: PROPOSED; written-design review required. Product implementation has not started.
Date: 2026-10-19. Owner: Eyad. Workflow: Superpowers architectural path.

## Purpose and scope

Make the next book easy to find and open without disrupting the reader that the owner already enjoys on the phone. Preserve Grayfog's blue-black archive, restrained brass and readable typography. This slice improves the existing Library retrieval flow; the Home visual redesign follows separately. It is not a replacement library, search engine, cloud service or world renderer.

Owner intent: premium native Android quality, offline use, clear navigation, Persian/English support and evidence before acceptance. Assumption: retrieval correctness and recovery should precede additional Home decoration. Correct that assumption during review if the visual Home surface should take priority.

Success: a user searches, understands the actual ordering and active constraints, recovers from no results, opens the intended book through the established navigation owner, and returns to the same retrieval context.

## Evidence and baseline

Inspected product source: `be4df10c28a67f93abd25ca5d42c8c711d157158`, draft #556; main base `811f0d7d521fc11f110638a100c315f698d52fd5`. Foundation candidate has 1,042 unit and 177 Android emulator tests passing, unchanged lint issue counts, and an independent source review. Those receipts do not certify this proposed subsystem or physical phone quality.

| Existing source | Observed behavior | Design consequence |
|---|---|---|
| `LibrarySearch.kt`, `LibrarySearchTest.kt` | Persian/Arabic letter and digit variants, accents, marks and spacing are already normalized; field relevance already exists | Reuse both; do not advertise a new normalization engine |
| `LibraryScreen.kt`, filtered-list and sort-label branches | A nonblank normalized query under stored `Recent` sorts by relevance, while the control still displays Recent | Make the displayed and selected sort match the comparator actually applied |
| `LibraryScreen.kt`, collection cleanup and series selection | Collection disappearance is reconciled; no corresponding series-selection reconciliation is present | Reconcile both from the current book metadata snapshot |
| `LibraryRetrievalPolicy.kt`, grouping; `LibraryScreen.kt`, matching | Group identity trims labels and ignores case; filter matching ignores case without trimming | Use the same canonical label key for grouping and selection matching |
| `LibraryScreen.kt` | Gallery, Shelves and Index, lazy book keys, result count, reset and empty states already exist | Retain these capabilities; refine the shared contract rather than add duplicate controls |
| `ThresholdModel.kt`, `ReadingNowScreen.kt` | Resume hero/recent policy already excludes non-imported placeholders and uses stable IDs | Preserve Home behavior in this slice |
| `VeilApp.kt` | Import/open failure handling and request ownership are above the screen | Keep them there; UI changes must not create another importer or opening coordinator |

The ordering/selection findings are source observations, not newly executed UI reproductions. Implementation must establish failing behavioral tests before fixing them.

## Alternatives

| Approach | Trade-off | Decision |
|---|---|---|
| Focused native retrieval state and control improvements | Small reversible slice, directly addresses observed inconsistencies, reuses storage/search | Recommended |
| Rebuild Home and all Library views together | More visible change, but larger visual/accessibility/regression surface and harder attribution | Subsequent design cycle |
| Make stars/3D the primary library | Strong atmosphere but obscures retrieval and adds graphics/resource requirements | Keep optional under the separate world design |

## User experience contract

Keep the existing screen order: archive heading/import → search → reading-state selector → view/sort/filter controls → results or recovery. Use plain localized labels. No new permanent toolbar or animated navigation gate.

| Situation | Visible behavior and action |
|---|---|
| No search, default sort | Recent books first; control says Recent |
| Meaningful search, default sort | Relevance first; control says Relevance and its menu item is selected |
| Search with explicit Recent/Title/Author/Series/Progress/Archive Depth | The chosen comparator runs and its label/selection is truthful |
| Query contains only ignored marks/spacing | Keep the user's raw input editable; treat it as no effective search, without claiming relevance ordering |
| Active reading state, collection or series | Each constraint remains visible; selected series uses its actual name; accessible removal action names the constraint |
| Clear search | Clear only query; retain shelf/collection/series and explicit sort |
| Reset filters | Clear query, shelf, collection and series; preserve the chosen view and explicit sort, as the current reset does |
| Selected collection/series no longer exists | Clear only that stale metadata constraint; keep other choices; no misleading permanent empty result |
| Empty library | Existing import action and supported-format explanation; no invented books or progress |
| No matches | Describe the active retrieval context and offer reset; distinguish from an empty library |
| Import in progress/failure | Retain the host's actual status/error path; do not invent success or block access to existing books |
| Open a result and return | Dispatch the current canonical book through `onOpenBook`; preserve query, filters, view, sort and appropriate scroll position while the same navigation entry survives |

No new sort persistence to disk is promised. Existing `rememberSaveable` restoration remains the boundary; a fresh task/cold launch is not a cross-device preference store.

### Sort identity and restoration

Represent default contextual ordering explicitly in the retrieval model, separate from an explicit Recent choice. Internally the default may be `AUTO`: empty effective query resolves to Recent, meaningful query resolves to Relevance. AUTO is an implementation identity, not product text.

In the menu, Relevance is available only for a meaningful query and selects AUTO. Recent always selects explicit RECENT. Without a meaningful query, AUTO is represented by the single selected Recent row; do not render duplicate Recent/Automatic rows. Clearing a query leaves an explicit Title/Author/etc. choice intact.

Read saved legacy `Recent` as AUTO to preserve its existing contextual behavior; the old format cannot distinguish an explicit selection from its default. New explicit Recent uses a distinct stable key such as `RECENT`. Map legacy named choices to their matching modes, and unknown values to AUTO. Document and test this limited restoration decision; do not silently change Room/DataStore.

Default query ordering preserves existing relevance bands (title > author > series > collection > language), then existing recent-time and stable ID tie-breaks. Non-default comparators keep their current primary ordering and gain a stable book-ID final tie-break so equal keys do not reorder with input-list order. Series keeps numerical series order and title before that tie-break.

### Filter identity and reconciliation

Use one label identity function equivalent to trimmed, Locale.ROOT case-insensitive grouping for collection and series. Preserve displayed metadata; this is a transient comparison key, not a database rename or new fuzzy grouping rule. Do not use punctuation/spacing-stripping search normalization to merge collection identities.

Compute available identities and reconcile stale selections from the same canonical book snapshot. An empty results list alone must not clear a valid filter: another active filter or query may legitimately remove all matches. Deletion/metadata editing can invalidate a selection; a query change cannot.

Reading-state counts and filtering must share the same valid-data predicates. Keep finished status authoritative; test zero, positive progress and finished-with-zero cases. Do not silently introduce a progress-data repair/migration in this UI slice.

## UI and data ownership

`VeilApp`/repositories continue to own canonical books, mutations, import/open requests and errors. The screen owns transient retrieval selections. One pure derivation produces effective sort, reconciled constraints, ordered results and result state from that selection plus the existing search/memory inputs. It must not read files, start coroutines, write progress or persist metadata.

Cache normalized search documents by the canonical books snapshot as today. Query changes reuse those documents. Keep stable book IDs/content types in lazy results; do not create a second full-size publication/cover model, decode all covers, or perform I/O during composition. Avoid replacing current retrieval with an unmeasured full-text index.

Proposed file responsibilities, finalized by the implementation plan:

| File | Change |
|---|---|
| `ui/screens/LibraryRetrievalState.kt` | New focused pure selection/sort identity, restoration and derivation contract |
| `ui/screens/LibraryRetrievalPolicy.kt` | Reuse existing grouping; centralize label identity only where required |
| `ui/screens/LibraryScreen.kt` | Consume one derived state; wire existing callbacks, labels, reset and stale-selection reconciliation |
| `ui/screens/LibrarySearch.kt` | Reuse existing normalization/relevance unchanged unless a failing case proves a necessary correction |
| `res/values/strings.xml`, `res/values-fa/strings.xml` | Relevance and precise accessible filter/sort/recovery labels; no raw internal keys in product text |
| Existing Library search/composition and Grayfog test files | Behavioral regressions and real Compose interaction assertions |

Keep `ReadingNowScreen`, `ThresholdModel`, reader engine/session, Room v3, backup formats, application ID, signing configuration and dependencies unchanged. Do not merge #542/#545/#546 wholesale.

## Accessibility and visual constraints

Controls wrap at narrow widths; dynamic labels may wrap rather than hide the only action. Keep existing >=48dp interactive targets. The search purpose remains available after text entry; sort exposes its current value; filter removal identifies the selected group. Decorative ornaments do not create focus nodes. Result updates may use one polite announcement, never per-keystroke chatter or repeating progress/live-region timers.

Verify EN/LTR and FA/RTL at 320dp and 390dp, 100% and 200% font, short landscape and tablet width. Long Persian titles, series and collection names must not clip primary actions. Preserve all three views. Reuse current Grayfog theme; this is not a new artwork/font/animation system. Semantic reachability is necessary and does not establish physical clipping or TalkBack quality by itself.

## Acceptance and evidence

| Layer | Required evidence on the exact implementation candidate |
|---|---|
| Pure regression tests | Default query/nonquery sorting; explicit Recent honored; legacy/unknown restoration; stable ties; label trim/case identity; removed versus merely hidden metadata groups; clear-query versus reset behavior |
| Search preservation | Existing Persian/Arabic variants, digits, accents, half-spaces, ranking bands and non-match tests remain green |
| Compose interaction | Search → sort Recent/Title → clear query; select/remove/reconcile series; reset empty results; open exactly the selected book through existing callback; restored selection after recreation |
| Native accessibility/captures | Existing shell fixtures plus long FA/EN labels, 320/390dp, 200% font, all views, empty/importing/no-match states; real same-fixture captures and reviewed visual differences |
| Reader integration | Existing import → open → close → return behavior and saved reader locator stay intact; no progress writes from retrieval |
| Build | Full unit/lint/debug/instrumentation compilation, storage instrumentation, existing policy/Paper guards and emulator performance gates; no relaxed budgets or new lint issues |
| Device/promotion | Physical TalkBack, clipping, keyboard/Back behavior and scroll/return continuity; keep UNVERIFIED where hardware is unavailable |

Exercise a synthetic 1,000-book metadata fixture for deterministic query/order/reconciliation correctness and visible-list interaction. This is a local-library fixture, not proof of million-user backend scale; record timings only when measured, and retain the existing physical/emulator performance distinction.

Every receipt records source SHA, variant, fixture, device/emulator, test results and artifact hash/signer as applicable. CI and a source review cannot replace the owner's visual/device acceptance.

## Delivery and rollback

After written-spec approval, produce a detailed test-first implementation plan with isolated branch ownership, exact existing tests, baseline commands and native execution method selection. Add failing behavior tests for the observed comparator/label and stale-filter issues before production changes. Keep characterization tests for reused search behavior. Obtain one independent whole-branch source review and publish a draft; no automatic main merge, public release, uninstall or app-data reset.

Rollback is the isolated source commit/branch; this slice adds no irreversible data migration. Separate meaningful UI behavior changes from any mechanical extraction in commit history. Finish this retrieval journey before proposing the Home visual redesign.

## Primary platform references

- Compose semantics, state descriptions and announcements: https://developer.android.com/develop/ui/compose/accessibility/semantics
- Lazy lists/grids and stable item identity: https://developer.android.com/develop/ui/compose/lists

These references guide platform use; they are not evidence that Veil has passed the acceptance table.

## Design self-review and next gate

Checked: one existing subsystem; observed versus proposed behavior separated; no cloud/schema/reader changes; legacy sort ambiguity resolved explicitly; canonical filter identity defined; empty state does not erase valid filters; restoration boundary and device limits stated; all success criteria assigned to evidence. No placeholder implementation or unverified pass is claimed.

Review this written retrieval design before implementation planning. The new code and plan do not yet exist.
