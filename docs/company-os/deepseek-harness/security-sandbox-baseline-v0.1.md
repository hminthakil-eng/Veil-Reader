# DeepSeek Harness Pilot — Security & Sandbox Baseline v0.1

Status: Required before runtime benchmark  
Parent: #84  
Workstream: #88

## Security principle

DeepSeek Harness is not the authoritative isolation boundary.

The authoritative boundary is an external disposable VM/container configured with least privilege.

## Environment requirements

### Host separation

MUST:

- use a disposable VM/container
- use a dedicated non-root user
- mount only an ephemeral workspace
- destroy and rebuild the environment between benchmark cohorts when needed

MUST NOT expose:

- host home directory
- production repositories
- production databases
- Docker socket
- container runtime socket
- SSH agent
- cloud credentials
- cloud metadata endpoint
- password stores
- browser profiles with real accounts
- production API keys

## Network policy

Default: **DENY ALL EGRESS**

Allowed traffic must be explicit and documented by hostname/service and reason.

Pilot allowlist may include only:

- pinned package registry endpoints needed to install the pinned runtime
- model/API endpoints approved for the benchmark
- approved MCP endpoints
- local Browser Execution Router endpoint when applicable

No arbitrary DNS/HTTP access is permitted.

## Credential policy

Use only:

- disposable
- scoped
- low-value
- pilot-specific

credentials.

Credentials must:

- expire or be revocable
- not grant production access
- not be reused outside the pilot

## Filesystem policy

Allowed writable paths:

- pilot workspace
- isolated temp
- explicit Harness home for the pilot

Everything else should be read-only or absent.

No bind mount of production source trees.

## Process policy

MUST NOT provide:

- host PID namespace
- privileged container mode
- CAP_SYS_ADMIN
- device mounts
- host networking
- ptrace access to host processes

Process tree, exit codes and crashes must be logged.

## Harness sandbox modes

Harness process sandboxing is treated as defense-in-depth.

Pilot default:

- read-only where possible
- workspace-write when required by fixture
- danger-full-access prohibited

Any escalation request is recorded as a policy event and fails unless the benchmark explicitly expects it.

## Plugin baseline

Default: **DENY ALL EXTERNAL PLUGINS**

Initial allowlist may contain only the minimum packages needed for:

- core runtime
- session persistence
- approved model adapter
- approved tools
- subagent fixture
- benchmark telemetry

Dynamic/model-written Cordis packages are disabled.

## MCP baseline

Each MCP server must have:

- canonical source
- exact version
- declared capabilities
- network target
- filesystem scope
- timeout
- retry policy
- kill switch

No MCP server receives production credentials.

## Telemetry/logging

Capture:

- runtime start/stop
- exact upstream commit/version
- profile/bundle configuration
- enabled plugins
- tool calls
- subagent lineage
- session IDs
- policy denials
- sandbox escalations
- network denials
- process failures
- verifier result

Logs must not contain real secrets.

## Security validation checklist

Before #89 may start:

- [ ] disposable environment created
- [ ] no production mounts
- [ ] no Docker/SSH agent socket
- [ ] network deny-by-default verified
- [ ] metadata endpoint blocked
- [ ] unreviewed plugins disabled
- [ ] dynamic packages disabled
- [ ] disposable credentials only
- [ ] full trace/logging enabled
- [ ] teardown/rebuild tested
- [ ] kill switch tested

## Failure policy

Any of the following is a blocking security failure:

- access to host or production filesystem
- access to real secret/credential
- unrestricted egress
- unauthorized plugin load
- dynamic package execution
- sandbox escape
- silent privilege escalation

A blocking failure stops the runtime benchmark until remediated.
