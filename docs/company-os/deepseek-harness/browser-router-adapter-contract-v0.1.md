# DeepSeek Harness Pilot — Browser Execution Router Adapter Contract v0.1

Parent: #84  
Related: #81, #91

## Ownership

Browser policy is owned by **Eyad Company OS**.

DeepSeek Harness MUST NOT decide which browser backend is globally preferred.

## Logical interface

Harness sends a browser task envelope to the external router:

- task_id
- session_id
- objective
- allowed_origins
- data_classification
- requested_capabilities
- timeout
- verifier_spec
- trace_context

Router returns:

- status
- selected_backend
- fallback_chain
- actions_summary
- verifier_result
- artifacts
- error_class
- trace_reference

## Router backends

Possible backends:

1. Jev DOM/ARIA fast path
2. Playwright / structured automation
3. Browser agent
4. Vision / Computer Use fallback

Backend choice is external policy.

## Hard rules

- Jev is optional.
- Unsupported iframe/shadow/canvas/upload/popup/keyboard-heavy cases must be eligible for fallback.
- Harness core is not patched for any backend.
- Browser credentials, if ever allowed later, remain managed outside Harness core.
- DONE is not success; verifier_result decides.

## Failure behavior

Adapter must distinguish:

- UNSUPPORTED
- TIMEOUT
- NAVIGATION_ERROR
- POLICY_DENIED
- VERIFICATION_FAILED
- BACKEND_CRASHED

UNSUPPORTED should trigger policy-controlled fallback, not silent failure.

## Pilot acceptance

PASS if:
- a structured task completes through one backend,
- an unsupported task cleanly falls back,
- browser failure is visible to Harness,
- verifier result is preserved in trace,
- backend can be changed without modifying Harness core.
