# Mobile Command Layer — Pilot Backlog

## P0 — must ship first

- [ ] Define typed command envelope and result envelope.
- [ ] Add project registry entry for Veil Reader.
- [ ] Add job state machine.
- [ ] Add audit event schema.
- [ ] Add mobile dashboard health endpoint.
- [ ] Register a trusted self-hosted runner.
- [ ] Add phone-triggerable `test.run` for Veil Reader.
- [ ] Return GitHub run URL/status to the phone.
- [ ] Add approval gate for `agent.task`.
- [ ] Add cancellation/retry behavior where supported.
- [ ] Add secrets handling and least-privilege checklist.
- [ ] Document secure remote-access topology.

## P1 — reliability

- [ ] Retry transient failures with bounded backoff.
- [ ] Add provider/runner health checks.
- [ ] Add local-first execution routing.
- [ ] Add fallback policy for unavailable AI providers.
- [ ] Add job deduplication/idempotency key.
- [ ] Add notification channel abstraction.
- [ ] Add structured error codes for mobile UI.

## P2 — company rollout

- [ ] Connect Manga Production Director.
- [ ] Connect Google Video Connector.
- [ ] Connect SID research jobs.
- [ ] Add portfolio health summary.
- [ ] Add command permissions by project/action.
- [ ] Add metrics: queue time, runtime, success rate, cloud fallback rate.
- [ ] Extract Company OS code/docs into a dedicated repository.

## First vertical-slice acceptance test

Given the owner is on a phone,
when they select Veil Reader and press Run Tests,
then a trusted runner accepts the job,
the UI shows queued/running/completed states,
the final status is returned with a GitHub link,
and no desktop interaction is required.
