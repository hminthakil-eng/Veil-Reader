# Grand Forge UI/UX Audit & Scorecard — 2026-10-05

## Evidence basis

Current executable UI source reviewed from `grand-forge/arena-app-hardening-v1` at executable head `403e77162f7ed19f34df0e71dded854d4f5462c9`. The branch later received documentation-only commits; those do not change the UI score.

Latest current Android evidence reviewed:
- Storage Instrumentation run `37314662665` — SUCCESS.
- Artifact `11347218792`, `veil-reader-grayfog-shell-review`, 49,773,828 bytes.
- Actual Threshold and Archive captures inspected in English/Persian, 100–200% text scales, Standard and High Contrast.

Other surfaces are scored from current production Compose/source plus the existing Grayfog authored-world/native-render evidence. Physical-device tactile/optical acceptance remains open, especially Paper Curl, OLED/LCD contrast, haptics and real frame pacing.

Commercial reference behavior checked against Apple Books, Kindle and Kobo official current reading guidance. Closed-source competitors are behavior references only; no proprietary implementation is assumed.

## Overall strict score

**Current Veil Reader UI/UX: 78/100.**

- Distinctive art-direction potential: **91/100**
- Current visual execution: **80/100**
- Current interaction architecture: **86/100**
- Current retrieval efficiency: **71/100**
- Physical-device proof: **not yet scoreable as world-class**

World-class target for this project: **92+/100** with no critical subsystem below 85.

The app is no longer visually generic. The gap is now composition discipline, density, first-action speed and device proof rather than lack of identity.

## Score matrix

| Area | Score | Verdict |
|---|---:|---|
| Visual identity / authorship | 91 | Signature quality; clear Veil DNA |
| Color / material architecture | 85 | Strong cool-dark + parchment + brass system |
| Latin typography | 83 | Strong editorial hierarchy; some utility rhythm still weak |
| Persian / RTL visual typography | 80 | Structurally excellent, optical scaling needs refinement |
| Navigation / information architecture | 82 | Clear ownership, strong bottom navigation |
| Threshold / Home | 76 | Beautiful but over-authored before the primary action |
| Library / Archive retrieval | 70 | Biggest UI/UX weakness |
| Gallery | 72 | Strong objects, oversized records/placeholder hierarchy |
| Shelves | 78 | Better spatial metaphor, needs real-library density proof |
| Index | 83 | Most efficient Archive mode |
| Book Detail / Artifact Chamber | 81 | Good narrative hierarchy, device proof pending |
| Reader Sanctuary | 87 source | Excellent restraint/ownership; tactile proof pending |
| Reader Appearance | 83 | Powerful, increasingly coherent |
| PDF workspace | 82 | Properly separated from EPUB; real-document proof pending |
| Notes / Hidden Archive | 79 | Strong product idea, density still needs hierarchy polish |
| Settings | 78 | Functionally mature; still slightly too engineered |
| Castle / world UI | 76 | Distinctive but spatial finish still below concept art |
| Observatory | 79 | Good truthful instrument model, still visually system-like |
| Empty/loading/error states | 77 | Coherent foundation, not yet uniformly memorable |
| Accessibility architecture | 87 | Strong 48dp/script/high-contrast discipline |
| Accessibility optical quality | 78 | 200% layouts survive but become compositionally heavy |
| Responsive/adaptive design | 83 | Real policies exist, more extreme-device proof needed |
| One-hand usability | 74 | Reader good; Library/Threshold first-action distance weaker |
| Motion / haptic language | 81 source | Intentional, but device feel remains an open gate |
| Consistency | 84 | Shared materials/type/geometry are real |
| Commercial finish | 76 | Needs density and speed-of-use refinement |

## What improved materially versus the September audit

The old audit found only GRID/LIST and no confirmed Gallery/Shelves/Index model. Current source now has all three explicit modes:
`LibraryViewMode.GALLERY / SHELVES / INDEX`.

The old audit also identified generic temporary typography. Current source ships explicit offline Latin and Persian/Arabic font families with script-aware metrics and zero Arabic tracking.

Reader mode ownership is now explicit:
- Paper
- Slide
- Paged
- Scroll

PDF remains independent.

Current navigation labels are 11sp rather than the older 9–9.5sp concern and navigation targets retain 56dp minimum height.

## Threshold / Home — 76/100

### What works
- Strong authored entrance; instantly recognizable.
- Atmospheric image is product identity, not stock decoration.
- Parchment current-volume object is visually distinct from shell.
- Current volume remains larger than recent books.
- Responsive policy now considers actual window dimensions and text scale.
- 48dp+ reading actions.
- Script-aware metadata.

### Current visual problem
The latest 100% English capture gives a very large portion of the first viewport to:
1. brand,
2. realm label,
3. enormous two-line narrative headline,
4. explanatory paragraph,
5. decorative rule,
before the parchment resume object.

This produces excellent poster art but weaker return-to-reading speed than Apple Books/Kobo-class daily-use flows.

The supplied target board is better balanced here: atmosphere is retained, but the active volume appears earlier and the hero sentence is subordinate.

### Correction
- Reduce active-session Threshold headline visual mass 20–30%.
- Treat long narrative copy as first-entry/empty-state material, not every return.
- Put the current book action into the first visual scan path.
- Preserve art; do not flatten Threshold into a generic dashboard.
- Keep the parchment object singular and dominant.

## Library / Grayfog Archive — 70/100

This is the screen needing the most work.

### What works
- Real search normalization for Persian/Arabic.
- Gallery, Shelves and Index are genuinely different presentation modes.
- Search, status, collection, series and sort are direct.
- No dependency on hidden deep archive paths for basic retrieval.
- No Material card-wall architecture.
- Source uses stable lazy keys and saveable filters.
- 48dp actions and semantics are common.

### Current capture problem
At 100% scale the full Archive search capture spends most of the first viewport on:
- large architectural header,
- Settings/Import utilities,
- a very tall search field,
- a large Reading State selector,
- view-mode rail,
- collection/series/recent/reset controls.

The first actual book object begins very late in the viewport.

That is the opposite of the product's own retrieval-first principle.

### Visual density problem
The search field and reading-state control use heavy borders and large vertical measures. Individually they are readable; collectively they feel like large instrument panels before the content.

### Recommended composition
Normal state:
1. compact Archive identity,
2. search,
3. one condensed retrieval rail,
4. books immediately.

Only expand secondary facets on demand or when active.

Search-active state may condense the header further.

### Gallery
The current gallery object is visually powerful but too tall for rapid scanning. Missing-cover fallback may repeat title information both on the object and below it.

Use physical object weight without turning one record into a near-full screen.

### Index
Current Index is the most commercially efficient mode and should be the density benchmark. Preserve its ruled-document grammar.

## Persian / RTL — 80 visual / 87 architecture

The current Persian 200% capture proves that the interface survives extreme text enlargement without catastrophic clipping, which is valuable.

However the resulting composition is too oversized:
- hero title becomes billboard-sized,
- search and filter instruments consume a very large vertical budget,
- mixed Latin query + Persian chrome feels optically uneven,
- first content object is pushed deep below controls.

This is not a reason to reduce accessibility text sizes. The fix is responsive composition: condense decoration, not user text.

At 150–200%:
- hide optional realm copy sooner,
- reduce decorative vertical gaps,
- move secondary filters behind an accessible disclosure,
- preserve 48dp targets and full text.

## Reader Sanctuary — 87/100 source

Reader is the strongest UX subsystem.

### Strong decisions
- Reading canvas intentionally has the lowest visual density.
- Chrome is edge-rail based rather than floating-card based.
- Center tap owns chrome reveal.
- Auto-hide is retained.
- Paper, Slide, Paged and Scroll have independent ownership.
- PDF cannot inherit EPUB turn effects.
- Appearance is Quick/Advanced.
- Reduced motion has an explicit path.
- previous-location, notes, selection, focus guide and TTS ownership remain protected.

Apple Books uses a similar low-chrome reading philosophy and exposes Curl/Fast Fade/Scroll plus typography and Line Guide. Veil's mode ownership is more explicit architecturally.

### Why it is not 95
No physical-device visual/tactile proof yet for:
- first Paper turn,
- destination flash,
- slow drag,
- reverse/corner turn,
- 60/90/120Hz pacing,
- OLED/LCD paper appearance,
- crease/backside/translucency,
- long-session visual comfort.

Source quality is not equivalent to tactile reading quality.

## Appearance — 83/100

Very strong power-reader surface:
- quick vs advanced,
- live preview,
- mode semantics,
- granular typography,
- script-safe constraints,
- material/patina options.

Risk: it can still become a settings dump.

The default Quick panel should remain brutally curated. Advanced may be rich, but ordinary users should be able to set Theme, Size and Navigation in seconds.

## Book Detail — 81/100

Good direction:
- book as artifact,
- identity and reading action close together,
- progress/history follow,
- maintenance/destructive actions demoted.

Remaining:
- real long Persian titles,
- low-quality/odd-ratio covers,
- missing metadata,
- finished/re-read state,
- large text,
- landscape/tablet,
- focus order.

The key rule: one obvious Continue/Read action. Favorite/Edit/Delete must never visually compete with it.

## Hidden Archive / Notebook — 79/100

Product concept is stronger than most commercial annotation screens:
- Notes,
- Highlights,
- Bookmarks,
- Echoes,
- Capsules,
- exact locator returns,
- memory/revisit context.

Risk: too many record types can create taxonomic complexity.

Use source title → quote/note → memory evidence → maintenance actions. Avoid making every memory type visually unique.

## Settings — 78/100

Functionally mature:
- progressive disclosure,
- 48dp controls,
- localized labels,
- high contrast,
- sensory options,
- reader controls,
- debug-only material review separated from release behavior.

Still slightly too engineering-oriented. General Settings should feel calm and boring relative to Castle/Archive.

Avoid ornamental frames around ordinary switches/sliders. Keep section titles literal.

## Castle — 76/100

Correct product idea: a spatial world, not a dashboard.

Source now includes:
- map,
- floors,
- bridges,
- chambers,
- spatial responsive policy,
- wide/compact differences,
- actual progression-derived room state.

The remaining problem is visual conviction. Repeated chamber structures can still read as sophisticated panels rather than architecture. Castle should be the richest realm but not a list wearing arches.

Next art pass should prioritize:
- stronger floor/portal continuity,
- fewer obvious repeated containers,
- more spatial depth behind room labels,
- tablet opposing wings,
- meaningful environment change from real reading history.

## Navigation — 82/100

Strong:
- stable primary destinations,
- minimum 56dp nav target,
- authored icons,
- selected-state structure,
- Reader remains its own low-chrome environment.

Current 11sp label floor is acceptable but should be optically reviewed in Persian and low-contrast states.

Do not add more permanent tabs for world systems. Primary navigation must stay reading-first.

## Design-system review

### Strong
- No generic Card dependency across major surfaces.
- Minimal radius vocabulary.
- shared spacing tokens.
- separate geometry families.
- explicit realm budgets.
- offline bundled shell typography.
- separate Persian/Arabic metrics.
- content-script typography helper for mixed-language metadata.
- High Contrast and Reduced Motion ownership.
- 48dp control discipline.

### Weak / remaining
- Some direct alpha values are scattered.
- Some surfaces still feel too similarly framed despite realm-density rules.
- responsive adaptation often grows/condenses components correctly but does not always rebalance the whole first viewport.
- decorative negative space sometimes becomes dead space instead of purposeful silence.

## Commercial comparison

### Versus Apple Books
Veil wins:
- identity,
- narrative world,
- local reader customization breadth,
- explicit page-mode ownership.

Apple wins today:
- effortless first-action clarity,
- calmer control density,
- shipped tactile proof,
- visual restraint under accessibility scaling.

### Versus Kobo
Veil wins:
- authored world,
- advanced typography architecture,
- richer local reading-memory concepts.

Kobo wins today:
- predictable retrieval/navigation efficiency,
- long-proven reading-menu ergonomics.

### Versus Kindle
Veil wins:
- visual originality,
- local-first ownership,
- direct reader customization.

Kindle wins today:
- mature progressive disclosure and assistive reading ecosystem.

## P0 UI/UX repair order

1. **Archive density pass**
   - first content appears much earlier;
   - compact normal-state retrieval rail;
   - secondary facets expand on demand;
   - keep Gallery/Shelves/Index.

2. **Threshold return-speed pass**
   - active-volume action earlier;
   - reduce recurring hero headline size/duration;
   - preserve cinematic entry for first/empty/major state changes.

3. **Accessibility composition pass**
   - at 150/200%, condense optional shell architecture rather than shrinking user text;
   - mixed-script baseline review;
   - preserve targets.

4. **Reader physical acceptance**
   - Paper/Slide/device feel;
   - chrome timing;
   - actual publication typography;
   - OLED/LCD.

5. **Castle spatial art pass**
   - fewer repeated panel readings;
   - stronger architecture;
   - tablet/wide proof.

## P1 refinement

6. tighten Gallery record height and fallback-cover hierarchy;
7. make Settings more neutral;
8. simplify Notebook taxonomy presentation;
9. unify loading/empty/error material transitions;
10. cross-realm motion/haptic audit.

## Do not regress

Do not:
- remove the three Library modes,
- merge Paper and Slide,
- put Paper on PDF,
- reintroduce universal rounded cards,
- make Reader atmospheric like Castle,
- shrink Persian text to solve layout,
- move basic retrieval behind mystical names,
- replace literal controls with lore labels,
- redesign stable Reader persistence for aesthetics.

## Final verdict

The UI has crossed from “promising custom skin” into a real authored product language. It is visually more distinctive than mainstream commercial readers.

It is **not yet world-class finished** because the current shell sometimes makes the user pay for that atmosphere with distance, vertical space and control density. The next improvement is not more ornament. It is ruthless composition editing: keep the world, remove the friction.
