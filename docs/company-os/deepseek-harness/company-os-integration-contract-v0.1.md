# Eyad Company OS ↔ DeepSeek Harness Integration Contract v0.1

Parent: #84  
Workstream: #92

## Foundational rule

**Company OS is the control plane. Harness is an execution substrate.**

## Company OS responsibilities

- authorize tasks
- apply policy
- select project context
- select/limit models
- select/limit tools and plugins
- route browser execution
- define budgets
- require approvals
- verify outcomes
- retain portfolio/project audit linkage
- enable kill switch

## Harness responsibilities

- execute approved task plan
- maintain runtime/session state
- invoke allowlisted tools
- invoke approved subagents
- emit traces/telemetry
- stop on policy denial
- expose recoverable failure state

## Dispatch envelope

Company OS should provide:

- company_task_id
- project_id
- user_intent_digest
- policy_version
- runtime_profile
- allowed_plugins
- allowed_tools
- allowed_models
- budget
- data_classification
- browser_policy_ref
- verifier_ref
- deadline
- trace_parent

## Result envelope

Harness should return:

- company_task_id
- runtime_session_id
- terminal_state
- output
- tool_summary
- subagent_summary
- usage
- policy_denials
- errors
- trace_ref
- checkpoint_ref
- verifier_handoff

## Policy injection

Company OS policy must be mounted/configured above default runtime behavior and be observable in the dumped/effective configuration.

Policy cannot be silently replaced by a plugin.

## Session identity mapping

Maintain:

Company task ID ↔ Harness session ID ↔ subagent child IDs

This mapping must survive restart and be available to audit tooling.

## Plugin control

Company OS owns the plugin allowlist.

Runtime must fail closed when a requested plugin is not approved.

## Cost/usage telemetry

At minimum capture:

- model/provider
- input/output tokens
- request count
- tool count
- wall time
- retries
- child-agent usage

## Kill switch

Company OS needs a supported path to:

- stop a session
- deny new tool calls
- deny new child agents
- disable a plugin/profile
- destroy the pilot environment

## Version pinning

Every execution trace records:
- Harness version
- Harness commit
- profile/bundle config version
- Company OS policy version
- plugin allowlist version
- benchmark fixture version

## Fork decision

Adapter/configuration is preferred.

Forking upstream is not permitted during Pilot v0.1.

Fork review may reopen only if an essential control-plane requirement cannot be implemented through supported profile/bundle/plugin/adapter seams.
