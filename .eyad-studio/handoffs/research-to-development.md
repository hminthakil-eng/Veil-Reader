# Research → Development Handoff

Use this contract whenever Research hands an idea, system, or risky change to Development.

## Handoff packet
1. Problem — what user or engineering problem are we solving?
2. Decision — what exactly is recommended?
3. Evidence — strongest findings, benchmarks, references, prototype results, and important counter-evidence.
4. Scope — what is included now and what is explicitly excluded?
5. Acceptance criteria — observable conditions Development must satisfy.
6. Guardrails — privacy, accessibility, performance, architecture, licensing, content, or UX limits.
7. Risks — known failure modes and remaining uncertainty.
8. Validation plan — how we will know the implementation succeeded.

## Development response
Development returns one of:
- ACCEPT — implementation can begin.
- REQUEST SPIKE — a bounded technical experiment is needed.
- RETURN FOR RESEARCH — evidence or requirements are not sufficient.
- SPLIT — separate the proposal into smaller independently testable slices.

## No silent assumptions
If implementation requires changing the approved behavior, dependency model, privacy surface, architecture boundary, or success metric, Development must record the change and route the decision back through Research/Product before treating it as settled.

## Fast lane
Low-risk implementation details do not need a formal research cycle. The handoff is mandatory only when the decision could materially affect reading UX, accessibility, data durability, performance, architecture, privacy, monetization, or the product's core identity.
