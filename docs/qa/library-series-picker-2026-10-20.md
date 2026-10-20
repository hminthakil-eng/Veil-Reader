# Series picker execution ledger

Approved scope: complete searchable series selection in the existing Android Library. Preserve query/sort, collection semantics, reader engine, storage, schema, dependencies and application identity.

Durable base: ec02ec2106590ddf85bc15b802b3ae21a90a0a48 (PR #557).
Plan: verify production UI regressions; replace eight-preview menu with canonical lazy searchable dialog; verify grouping and large lists; run Android CI and accessibility checks; review and retain a draft PR.

Recovery ruling: the sandbox execution environment became unavailable on 2026-10-20. Uncommitted picker files and the final test process cannot be recovered through current capabilities. This branch is a new implementation against the exact durable base, not a byte-identical transport of those files. No previous GREEN result is asserted.

Mission: GitHub's existing pull-request workflows provide bounded remote verification because no usable sandbox execution environment is available. Inputs are this branch's exact commits; workflow permissions and retention remain unchanged. No custom privileged workflow or paid infrastructure is introduced.

Step 1: Four production Compose regressions added before implementation. Expected RED: missing dialog/search/full choices. Existing seven cases retained. Await fresh Actions results.
