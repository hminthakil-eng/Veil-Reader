---
name: veil-security-privacy-reviewer
description: Security and privacy review for Veil Reader. Use for file imports, EPUB/PDF parsing, ZIP backup/restore, URI and storage handling, exported Android components, permissions, secrets, networking, logging, dependency changes, or any feature that could expose or corrupt user reading data.
---

# Veil Security & Privacy Reviewer

Veil Reader is local-first and handles user-owned documents. Preserve that trust boundary deliberately.

## Threat model first

For the changed surface identify:
- untrusted input source (document, archive, URI, metadata, intent, deep link, network response);
- assets at risk (books, annotations, reading history, settings, progression, filesystem, credentials);
- trust boundary crossed;
- worst realistic failure: read/write outside app storage, arbitrary overwrite, data loss, privacy leak, denial of service, or code/dependency compromise.

## File and URI handling

Check for:
- path traversal / `../` and absolute-path escapes;
- canonical-path validation before extraction or copy;
- archive entry count and decompressed-size limits;
- duplicate/conflicting archive entries;
- malformed filenames and Unicode edge cases;
- symlink or special-file handling where relevant;
- bounded reads instead of trusting declared sizes;
- MIME/extension mismatches and parser failure handling;
- persisted URI permissions only when needed;
- no accidental exposure through external/shared storage.

## Backup and restore

- Treat backup ZIPs as hostile input until validated.
- Validate structure, paths, counts, sizes, schema/version, and required metadata before replacing current state.
- Restore transactionally: a failed restore must leave the previous library usable.
- Never use partial extraction as authoritative state.
- Preserve compatibility with supported older backup versions.
- Do not silently upload backups or books anywhere.

## Android surface

Review:
- exported activities/services/receivers/providers;
- intent/deep-link input validation;
- FileProvider configuration and URI grants;
- runtime permissions and whether each is actually required;
- clipboard/share behavior for private reading data;
- WebView or external browser usage if introduced;
- pending intents / mutability flags where applicable.

Prefer no permission over a broad permission when Storage Access Framework or app-private storage is sufficient.

## Privacy

- Do not add analytics, telemetry, ads, accounts, cloud sync, or external AI processing without an explicit product decision.
- Never log book contents, annotations, filesystem paths containing sensitive names, tokens, backup payloads, or personally identifying metadata unnecessarily.
- Keep debug logs out of release-sensitive data paths.
- New network access must be justified, scoped, documented, and visible in product behavior/privacy documentation.

## Secrets and supply chain

- No API keys, signing credentials, tokens, or private endpoints in source control.
- Pin or constrain dependencies according to existing Gradle/version-catalog practice.
- Prefer established libraries already in the project over adding a second solution for the same job.
- Treat dependency additions as security surface: check maintenance, license, transitive weight, permissions/manifest contributions, and whether the feature truly needs it.

## Denial-of-service resilience

For imported files and archives, consider adversarial sizes, deeply nested structures, huge metadata, expensive parsing, decompression bombs, repeated malformed inputs, and main-thread work. Fail with bounded resource use and a recoverable user-facing error.

## Severity

- **Blocker** — path traversal/arbitrary overwrite, secret exposure, private document leakage, destructive restore/data corruption, unsafe exported surface, or high-confidence exploitable issue.
- **Should fix** — missing limits/validation, excessive permission, sensitive logging, unnecessary network/privacy expansion, or risky dependency choice.
- **Hardening** — defense-in-depth improvement with a clear threat model.

## Completion rule

A successful build is not security evidence. For high-risk changes, require a concrete adversarial test or regression test for the failure class being prevented.
