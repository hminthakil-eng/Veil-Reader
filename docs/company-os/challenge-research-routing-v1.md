# Challenge Research Routing v1

Parent: #96

## Objective

Route challenge investigation to the minimum capable unit while preserving evidence, avoiding repeated research, and producing decisions that execution teams can use.

## Triage questions

Ask in order:

1. Is this already a known challenge?
2. Is there an active canonical Challenge ID?
3. Is there an existing verified internal solution?
4. Is the root cause known with sufficient evidence?
5. Does an external solution/tool/standard likely exist?
6. Is the evidence stale?
7. Is this cross-project or systemic?
8. Is security, compliance, architecture, or material cost involved?

## Routing

### Project team

Owns:

- reproduction
- impact description
- logs/screenshots/test evidence
- local technical facts
- known workaround

May resolve directly only when a validated solution already exists.

### SID

Owns solution intelligence when:

- existing tools/workflows/repositories may solve it,
- build-vs-integrate decision is required,
- architecture/tooling choice is involved,
- duplication/reuse must be evaluated.

Operating rule:

Reuse → Integrate → Automate → Build.

### RIU / Research

Owns deeper evidence gathering when:

- the underlying phenomenon is unclear,
- current external evidence is needed,
- standards/market/technical research is needed,
- competing claims require validation.

### Security

Required for:

- credentials/secrets
- privilege boundaries
- sandbox/isolation
- supply-chain risk
- untrusted plugins
- external data handling
- security incidents

### QA / Verification

Defines independent success evidence.

No research recommendation is considered proven merely because an agent/tool reports success.

### Board / Portfolio Command

Required when:

- cross-project priorities conflict,
- architectural authority changes,
- significant cost/risk is accepted,
- project scope must change materially.

## Research output template

### Question

What exact uncertainty must be resolved?

### Existing internal evidence

What have we already learned/built/tested?

### Freshness

Which evidence is still valid?
Which assumptions need targeted refresh?

### External evidence

Relevant existing solutions, standards, repositories, tools, benchmarks, documentation.

### Findings

What is supported by evidence?

### Alternatives

List material alternatives and tradeoffs.

### Recommendation

ADOPT / INTEGRATE / EXTEND / BUILD / WATCH / HOLD / REJECT

### Verification

What test/evidence would falsify the recommendation?

## Stop conditions

Stop research and escalate if:

- a CH0 security/data-loss condition exists,
- production containment is required,
- evidence requires real secrets or unsafe access,
- the research question changes materially,
- a canonical prior answer is discovered and remains valid.

## Anti-rework rule

Research must cite the prior Challenge ID / Work ID when it extends previous findings.

Do not recreate the same market scan, architecture review, benchmark, or tool evaluation unless the old evidence is materially stale.
