# Eyad Prompt & Skill Registry — Intake Scanner v0.1

Status: Draft pilot specification  
Parent: #73, #74, #75  
Policy: fail-closed, read-only by default, no direct public MCP to production agents.

## Purpose

The intake scanner is the first technical trust boundary between public prompt/skill sources and the Eyad Approved Registry. It performs static analysis only. It MUST NOT execute candidate scripts, install dependencies, contact candidate-defined endpoints, or expose company secrets.

## Inputs

A candidate may be:
- prompt text
- skill folder/archive
- template/workflow
- tool descriptor
- upstream repository path/release

## Required extraction

The scanner MUST emit:

- stable candidate id
- upstream project and canonical source URL
- upstream version/tag/commit
- content hash
- import timestamp
- file inventory
- content type
- license evidence locations
- dependency manifest
- declared and inferred capabilities
- scripts/binaries present
- dynamic includes / remote loaders
- external URLs/domains referenced
- package-install instructions
- shell/code-execution indicators
- secret/credential references
- filesystem read/write indicators
- browser/tool-call requirements
- project relevance tags
- provenance evidence summary

## Capability flags

The manifest MUST explicitly represent:

`shell_exec`
`network_access`
`package_install`
`filesystem_read`
`filesystem_write`
`secret_access`
`credential_access`
`external_tool_call`
`code_execution`
`browser_automation`
`persistent_memory_write`
`outbound_webhook`
`data_export`

Unknown or ambiguous capability declarations MUST be treated as high risk.

## Detection rules

### Immediate quarantine

Quarantine when any of the following is detected:

- shell execution
- arbitrary code execution
- package installation
- secret/credential access
- arbitrary network access
- binary payloads of unknown provenance
- obfuscated/generated executable code
- unresolved license
- incomplete/high-risk provenance
- remote loader/self-updater behavior
- hidden post-install behavior

### Immediate reject candidates

Reject or escalate directly to Security when there is evidence of:

- credential exfiltration
- persistence mechanisms unrelated to stated function
- destructive commands without valid need
- policy bypass instructions
- known compromised/typosquatted dependency
- deliberately hidden network destination
- malicious prompt/tool injection designed to escape Company OS policy

## Static-only guarantees

The scanner MUST NOT:

- execute scripts
- import packages
- run shell commands from candidate content
- dereference remote includes automatically
- fetch arbitrary candidate-defined URLs
- read local secrets
- write outside the registry scan workspace

## Output manifest

Each scan produces a machine-readable manifest and a human review summary.

Required machine fields:

- candidate_id
- content_hash
- upstream
- provenance_status
- license_status
- capability_flags
- risk_class
- findings[]
- project_tags[]
- recommended_action

Recommended actions:

`PASS_TO_PROVENANCE`
`QUARANTINE`
`REJECT`
`MANUAL_REVIEW`

## Exit criteria

The scanner specification is accepted when:

- all capability flags are representable
- malformed/unknown declarations fail closed
- scripts/binaries are detectable
- high-risk patterns deterministically quarantine
- manifest output is stable and versionable
- zero candidate code is executed during scan
