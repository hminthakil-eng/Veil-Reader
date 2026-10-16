# Sealed investigation ledger domain prototype

This isolated, opt-in-by-integration prototype has no runtime callers, persistence, Room,
Readium, UI, Gradle or dependency changes. Remove the three domain files and matching test
file to reverse the implementation. It adds no ingestion or reading-history claims and
contains no borrowed canon, dialogue, art or trademarks.

## Contract and assumptions

- Callers issue opaque, non-blank theory/link/prediction IDs and must ensure uniqueness
  across their own lifetime/store. LedgerSnapshot rejects duplicate IDs within each record
  kind. IDs are not derived from hashCode, quote text or publication metadata.
- PublicationIdentity accepts only lowercase 64-hex SHA-256 shape. The caller must supply
  a digest of the entire original source, verify its provenance and bind saved Book IDs
  to that exact version. This code neither computes nor proves a digest correct. A changed
  source version must receive a different binding; URI/name/metadata never imply identity.
- Links are scoped to a single exact publication identity (including EPUB/PDF source type).
  They connect two distinct saved Highlight IDs. Theories retain IDs and private authored
  text, never copies of highlights. A resolution snapshot must contain current records and
  current bindings; omissions, deletion markers, stale version bindings and duplicate
  references/records/bindings yield MISSING tombstones, without selecting an arbitrary match.
  Deleted books must have their bindings removed. Returned Highlight records are sensitive
  domain objects, not safe UI state. Existing Book and Highlight types are untouched.
- ResourceIdentity validates a non-blank resource/chapter token and source type; callers
  must validate that the token belongs to that source. AnchorReference checks source and
  publication consistency and preserves a non-blank opaque locator. There is no locator
  parser, resource-membership proof, ordering, PDF page convention or stable decoding claim.
  EPUB reflow, PDF page mapping and locator provenance remain unproven.
- SectionCoverage is an explicit trust assertion supplied by a caller, not something the
  prototype produces from navigation. Exact publication/resource/opaque-target equality is
  required; incompatible certificates return UNVERIFIABLE. No certificate means LOCKED,
  regardless of current locator jumps. Publication mismatch always means UNVERIFIABLE,
  even for a previously revealed record. A current anchor is diagnostic only and grants
  no coverage; navigation to another resource within the same publication is permitted.
- Evaluation returns status only. A deliberate owner RevealAction can create a REVEALED
  private record only when eligible. The caller owns action provenance and authentication;
  owner IDs and public constructors are not security barriers. Previously revealed records
  stay revealed absent a mismatch; no automatic reread reseal policy is introduced.
- TheorySummary, LinkSummary and PredictionSummary omit private titles, bodies, relation
  labels, quotes, prediction text and locator payloads. Even REVEALED summaries carry no
  text. A later UI must deliberately design private text access and accessibility behavior.
  In-memory private domain objects remain sensitive; no heap/log/encryption guarantee exists.

## Corrections to the consultation

Sidecar JSON is not automatically risk-free or fully reversible: atomic writes, backup,
restore, deletion, concurrency, permissions, encryption and sync require independent review.
No sidecar is implemented. Partial header/tail hashes can miss middle-source modifications
and are not exact source identity. Fuzzy tolerance-based Comparable can violate transitivity;
this model has no fuzzy ordering. A 15-second dwell is not proof of reading and is not used.
A caller-trusted certificate permits manual reveal eligibility, not a claim of actual reading.

## Verification still required

Eleven JUnit tests cover version mismatch/reimport, jump-only lock, manual eligibility,
explicit owner reveal, deleted/missing highlight tombstones, duplicate references/records,
distinct link endpoints and unique IDs, invalid fingerprints, safe sealed projections,
incompatible certificates and ambiguous publication bindings. Tests were written first but
were intentionally not executed; compilation, red/green evidence and test results remain
unverified while the separate RebornUX QA run is active. Only git diff --check is permitted
for this slice. No builds, Gradle tasks, tests or device checks were run here.

Future work requires reviewed storage/migrations, digest/resource/certificate provenance,
authenticated owner actions, deletion/restore lifecycle, and real EPUB/PDF locator/device,
RTL, large-text and accessibility evidence before any production integration or safety claim.
