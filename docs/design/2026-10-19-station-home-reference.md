# Station Home — owner-supplied visual reference

Date: 2026-10-19. Status: visual reference brief, not an implemented screen or visual acceptance receipt.

The owner supplied `1000111342.png` during the rebuild discussion. It depicts Veil Reader as an ornate nocturnal railway station with a castle in the distance, warm lanterns, a dark train, brass framing and a prominent current-book panel. Treat it as a direction for the future native Home surface. The image alone does not approve the pending Library retrieval specification or a new implementation plan.

Reference SHA-256: `4c7488d99e2519778084e3819f865835f191663eca8fa31a8d428da7cb41f9f4`.
Source: the owner's attachment, inspected visually in the conversation; provenance beyond that is not established. This brief records a visual mapping, not a license claim or an external publication of the attachment.

## Faithful visual ingredients

- Tall architectural scene: vaulted station roof, distant blue moonlight/castle, train and platform depth.
- Warm amber lanterns and restrained fog against deep blue-black architecture.
- Engraved, aged brass rails and corners; dark inset panels with strong foreground text contrast.
- A large tilted upright book paired with the current-book details and a clear Continue Reading action.
- Recent book covers as tangible framed objects, followed by distinct archive entry cards.
- A prominent compass-like central navigation ornament, with plain destination labels.

Preserve the atmospheric richness. Scale decorative detail around readable content and reachable actions rather than reducing the design to a generic dark card list.

## Scene and interaction mapping

| Reference region | Native target | Data/action ownership |
|---|---|---|
| Hanging Veil Reader sign | Responsive branded header, scene asset behind live text where appropriate | Decorative identity; not a competing navigation control |
| Train, platform, castle, lanterns and fog | Layered backdrop with foreground/midground depth | Home-only atmosphere, no publication or progress ownership |
| Current book panel | Cover, title, author, confirmed progress and Continue Reading | Existing Threshold hero model and `onOpenBook`; actual stored book/locator |
| Chapter and time remaining | Optional metadata only when reliable for that publication | No fabricated chapter count or reading-time estimate |
| Recent books rail | Stable-ID book items, individual resume/open actions, See All | Existing recent-book model and Library route |
| All/In Progress/Completed/Favorites cards | Counts derived from canonical library state | Existing Library destination; filtered entry requires explicit routing design before being presented as working |
| Bottom ornament/navigation | Native labelled hit targets over decorative framing | Preserve the current navigation owner; scene art does not define routes |

The current route model has READING, LIBRARY, CASTLE, PATH and PROFILE, plus an existing Archive overlay. The reference instead shows Home, Library, Notes, Profile and a central ornament. Resolve that difference in the Home specification: Notes can use the existing Archive overlay, but replacing PATH or turning the ornament into a new route is not silently authorized by a picture. Give no ornament an invented successful action.

## Native rendering constraints

Compose supplies live text, buttons, progress, focus, scrolling and semantics. Exported illustration assets supply scenery/material detail; do not bake book titles, percentages, counts or interactive controls into a full-screen bitmap. The outer phone frame, camera cutout and depicted status bar belong to the mockup, not the Android app content.

Use bounded scene assets, suitable crop/focal regions for narrow and wide screens, and lazy cover loading. Scene layers must allow a tall Persian title or 200% font without moving the only resume action outside the usable layout. Foreground panels need sufficient opacity/contrast against the busy scenery.

Motion is proposed, not present: subtle fog drift, small lantern flicker and train ambience only if measured budgets permit. Respect reduced motion; pause/release animation and scene resources when Home is hidden, the app backgrounds or the reader opens. No scene rendering beneath the reading page. Do not infer a need for a 3D engine solely from the image.

The empty Home retains the station but replaces the current-book panel with an honest library/import entry. Existing books remain usable during import. Missing covers get deliberate placeholders; corrupt/unavailable books use the established recoverable error path.

## Review and evidence

Future Home work needs its own source-grounded written specification, implementation plan and native captures against this reference. Compare architecture/depth, material/light balance, hero hierarchy, cover presentation and navigation legibility in EN/LTR and FA/RTL at 320/390dp and 100%/200% font. Require physical motion, TalkBack, lifecycle and performance evidence separately from screenshot resemblance.

The Library retrieval specification remains a separate pending proposal at `docs/superpowers/specs/2026-10-19-library-retrieval-design.md`. This reference adds the owner's desired Home appearance without claiming its behavior or implementation is complete.
