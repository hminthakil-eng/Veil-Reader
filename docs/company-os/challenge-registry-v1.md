# Eyad Studio Company OS — Challenge Registry v1

Status: ACTIVE COMPANY POLICY  
Parent: #96  
Scope: All active and future Eyad Studio projects

## Purpose

Every project must have a visible, durable place to report challenges.

Every material challenge is:

1. registered,
2. triaged,
3. researched when needed,
4. decided,
5. executed,
6. independently verified,
7. resolved,
8. converted into reusable knowledge.

This system extends the existing Risk Radar, SID, RIU, Decision Log, and Work Reuse Gate. It does not replace them.

## Lifecycle

REPORTED  
→ TRIAGED  
→ RESEARCHING  
→ DECIDED  
→ IN_EXECUTION  
→ VERIFYING  
→ RESOLVED  
→ LEARNED

Allowed exceptional states:

- DUPLICATE
- BLOCKED
- HOLD
- REJECTED
- REOPENED

## Canonical Challenge ID

Each material challenge gets one canonical ID:

`EYAD-CH-<project>-<issue-number>`

Examples:

- `EYAD-CH-VEIL-102`
- `EYAD-CH-MANGA-21`
- `EYAD-CH-VIDEO-8`

The canonical issue lives in the project repository when the challenge is project-specific.

Cross-project challenges may use the Company OS repository as their canonical home.

## Required fields

Every material challenge record must contain:

- Challenge ID
- Project
- Category
- Severity
- Status
- Observed impact
- Evidence
- Reproduction / frequency
- Current workaround
- Suspected cause
- Research question
- Owner
- Research owner when applicable
- Related Work ID(s)
- Related Challenge ID(s)
- Risk Radar link when applicable
- Research findings
- Decision
- Resolution
- Verification evidence
- Recurrence prevention
- Reusable lesson / asset / regression fixture

## Categories

- TECHNICAL
- PRODUCT
- UX
- SECURITY
- QUALITY
- PROCESS
- TOOLING
- DEPENDENCY
- COST
- DELIVERY
- COMPLIANCE
- UNKNOWN

## Severity

### CH0 — Critical

Examples:

- data loss
- credential/security compromise
- company-wide blocker
- production outage
- destructive failure

Immediate escalation:
Project Owner + SID/Risk + Board as appropriate.

### CH1 — High

Examples:

- core capability blocked
- major milestone blocked
- severe reliability issue
- serious architecture/security uncertainty

Must be actively owned and reviewed.

### CH2 — Medium

Meaningful degradation but a safe workaround exists.

### CH3 — Low

Minor friction, local inefficiency, polish issue, or low-impact process problem.

## Triage decisions

Every challenge receives one primary route.

### ROUTE_A — Known solution

Use when root cause and fix are already validated.

Action:

- run Work Reuse Gate,
- link or create canonical Work Item,
- execute only the unresolved delta.

Do not repeat research.

### ROUTE_B — Research required

Use when:

- root cause is uncertain,
- no validated solution exists,
- external solutions may exist,
- architecture/security/cost implications are material,
- standards/current technology matter,
- challenge is recurring,
- multiple projects may be affected.

Action:

- assign SID/RIU research,
- define explicit research question,
- produce evidence and recommendation,
- convert accepted result into canonical Work Item.

### ROUTE_C — Risk Radar

Mandatory when:

- CH0,
- material CH1,
- cross-project,
- systemic dependency,
- security/supply-chain,
- major schedule/cost exposure.

Risk Radar tracks exposure; Challenge Registry tracks investigation and resolution.

### ROUTE_D — Duplicate / existing challenge

Apply Work Reuse Gate.

Link to canonical Challenge ID and close the duplicate.

## Research contract

Research must answer the minimum necessary question.

Required output:

- what is actually happening,
- root-cause confidence,
- existing internal work,
- existing external solutions,
- evidence,
- alternatives,
- tradeoffs,
- security/license/cost implications where relevant,
- recommendation,
- unknowns,
- what must be verified next.

Avoid full re-research when only one assumption has become stale.

## Resolution contract

A challenge cannot move to RESOLVED only because work was completed.

Required:

- solution/change linked,
- verifier defined,
- expected outcome observed,
- regression/recurrence check completed,
- remaining known limitations documented.

For CH0/CH1, verification evidence is mandatory.

## Learned state

After RESOLVED, classify reusable output:

- regression test
- runbook
- architecture rule
- design-system rule
- reusable module
- checklist
- benchmark fixture
- prompt/skill
- SID Solution Registry entry
- Decision Log entry
- no reusable artifact

A recurring challenge with no learned artifact requires justification.

## Recurrence policy

If a RESOLVED challenge returns:

1. REOPEN it or link a new recurrence to the original Challenge ID.
2. Increase recurrence count.
3. Require root-cause analysis.
4. Review whether prior verification was insufficient.
5. Add or strengthen a regression fixture/control.

## Project review

Every project review includes:

- open CH0/CH1 challenges,
- oldest unresolved challenge,
- challenges waiting for research,
- recurring challenges,
- challenges blocked by another unit,
- newly learned/reusable outputs.

## Portfolio review

Company OS aggregates:

- challenges by project and severity,
- cross-project patterns,
- systemic dependencies,
- recurring root causes,
- research backlog,
- mean age,
- time to triage,
- time to researched decision,
- time to verified resolution.

## Metrics

- Open Challenges
- Open CH0 / CH1
- Challenge Age
- Time to Triage
- Time to Research Decision
- Time to Verified Resolution
- Recurrence Rate
- Duplicate Challenge Rate
- Research Reuse Rate
- Challenges Converted to Reusable Knowledge
- Cross-project Pattern Count
- Challenge-to-Risk Escalation Count

## Integration map

Project Challenge Intake
→ Challenge Registry
→ Work Reuse Gate
→ SID / RIU Research (when needed)
→ Risk Radar (when material)
→ Decision Log
→ Canonical Work Item
→ Verification
→ Learned Artifact

## Rule

**A project is not considered healthy merely because delivery is moving. Significant unresolved challenges must be visible.**
