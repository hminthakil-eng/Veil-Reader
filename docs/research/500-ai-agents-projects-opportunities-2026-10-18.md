# Eyad Studio — 500+ AI Agents Projects: Evaluated Opportunity Registry

- **Research date:** 2026-10-18
- **Primary index:** https://github.com/ashishpatel26/500-AI-Agents-Projects
- **Evidence:** primary repository `README.md` and `agents/README.md`; directly read agent source files 01, 02, 03, 07, 14, 15, 16, 19, 20 and their disclosed behavior, plus the `LICENSE`, and GitHub metadata for framework and external example repos.
- **Scope:** select agents and reusable architecture patterns for Eyad Studio, Veil Reader, Novel Factory, video production and QA.
- **Status:** research and opportunity capture **only** — no dependencies installed, no workflows activated, no external project code integrated, no actual agent run or build test performed.

## Key distinction — do not confuse catalog with 500 installable agents

The source calls itself a collection of **500+ use cases**. The `agents/README.md` enumerates **20 first-party, self-contained Python examples**. Most of the remainder are **links to third-party repositories, notebooks and project ideas**, not a single supported or ready-to-install package. These categories are not equivalent. The root project is MIT-licensed; **third-party links carry their own licensing and usage terms**, which must be checked independently. The official `crewAIInc/crewAI-examples` project, referenced for book writing and game building, is **archived** (read-only from April 20, 2026). The agent index contains older hard-pinned dependencies and model names. No claim is made that the examples were recently run or still work.

### First-party examples actually inspected in source

| Agent | Primary source | What was observed | Action |
|---|---|---|---|
| Web Research | [01](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/01-web-research-agent) | LangGraph, Tavily Search, LLM synthesis; makes API requests | **Adapt only**: citations, retrieved-source provenance, injection defense |
| Code Reviewer | [02](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/02-code-review-agent) | A prompt-based review of supplied code, defaults to Python conventions and GPT-4o | **Adapt only**: add Kotlin/Compose/Android rules, static analysis, proof |
| PDF Q&A | [03](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/03-pdf-qa-agent) | LlamaIndex loads PDFs, builds vector index, sends queries to OpenAI | **Experiment privately only**: offline-first, licensing, privacy |
| GitHub Issue Triager | [07](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/07-github-issue-triager) | Read-only fetches issue and prompts for labels/priority; prints recommendations | **P0 pilot**: do not auto-label until evidence validated |
| Social Media Agent | [14](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/14-social-media-agent) | Two CrewAI roles generate text content and hashtags, **not video** | **P1**: script-to-caption publishing assistant |
| Unit Test Generator | [15](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/15-unit-test-generator) | Produces Python `pytest`; generated tests are not automatically verified | **P0 pattern, rebuild for Kotlin/JUnit** |
| Documentation Writer | [16](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/16-documentation-writer) | Parses Python via AST and proposes docs; not Java/Kotlin-ready | **P1 pattern**: build Gradle/Kotlin aware docs |
| Competitive Analysis | [19](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/19-competitive-analysis-agent) | Three-step LLM sequence identifying competitors and creating strengths table. **No live source collection.** | **P0 concept only**, must add real web/forum/GitHub evidence |
| Multi-Agent Debate | [20](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/20-multi-agent-debate) | Two opposing personas and a judge, sequential calls; no independent evidence verification | **P1**: red-team reviews, not a substitute for tests |

## Ranked reusable opportunities

The following are **candidate ideas**, not certified turnkey agents. `Source reviewed` means inspected agent source. `Index link only` means its original implementation was not audited. Priorities: P0 = first pilot, P1 = secondary, P2 = backlog, HOLD = do not integrate until controls/necessity established.

| ID | Candidate / reference | Eyad Studio adaptation | Priority | Evidence level / main caveat |
|---|---|---|---|---|
| EA-01 | [GitHub issue triager](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/07-github-issue-triager) | Cluster Veil Reader defects, prioritize crash/page curl/RTL/performance | P0 | Source reviewed; read-only by default |
| EA-02 | [Web research agent](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/01-web-research-agent) | Evidence-first competitor research for Moon+, Kindle, ReadEra and new libraries | P0 | Source reviewed; add provenance and web safety |
| EA-03 | [Competitive analysis agent](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/19-competitive-analysis-agent) | Feature acceptance benchmark & gap analysis | P0 | Source reviewed; no browser/search implemented in example |
| EA-04 | [Unit test generator](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/15-unit-test-generator) | Design Kotlin JUnit/Compose UI/emulator test scenarios | P0 | Source reviewed; original generates Python tests only |
| EA-05 | [Code review agent](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/02-code-review-agent) | Support existing Android review gates and failure triage | P0 | Source reviewed; shallow prompt-only, not proof of correctness |
| EA-06 | [Write a Book with Flows](https://github.com/crewAIInc/crewAI-examples/tree/main/flows/write_a_book_with_flows) | Novel Factory outlines → arcs → chapters → continuity → editing | P0-pattern | Index link only; upstream archived; migrate to supported runtime |
| EA-07 | [Multi-agent supervisor](https://github.com/langchain-ai/langgraph/blob/main/docs/docs/tutorials/multi_agent/agent_supervisor.ipynb) | Role dispatch for Veil Grand Forge & SID | P1 | Index link only; notebook path may move |
| EA-08 | [Hierarchical agent teams](https://github.com/langchain-ai/langgraph/blob/main/docs/docs/tutorials/multi_agent/hierarchical_agent_teams.ipynb) | Design teams within common governance | P1 | Index link only; check up-to-date API |
| EA-09 | [Reflection agent](https://github.com/langchain-ai/langgraph/blob/main/docs/docs/tutorials/reflection/reflection.ipynb) | Iterative creative/technical draft refinement, with objective stop conditions | P1 | Index link only; avoid self-confirming quality loops |
| EA-10 | [Corrective RAG](https://github.com/langchain-ai/langgraph/blob/main/docs/docs/tutorials/rag/langgraph_crag.ipynb) | Book/novel fact checking across chapters and reliable citations | P1 | Index link only; source and relevance evaluation required |
| EA-11 | [Adaptive RAG local](https://github.com/langchain-ai/langgraph/blob/main/docs/docs/tutorials/rag/langgraph_adaptive_rag_local.ipynb) | Optional local knowledge retrieval without mandatory cloud account | P1 | Index link only; hardware, rights and security assessment |
| EA-12 | [PDF Q&A](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/03-pdf-qa-agent) | Book search/notes assistant, separate from on-device reader | P1 | Source reviewed; cloud transfer not acceptable by default |
| EA-13 | [Book recommendations](https://github.com/agno-agi/agno/blob/main/cookbook/examples/agents/book_recommendation.py) | Library discovery, author/series taste matching | P1 | Index link only; check actual path/current library APIs |
| EA-14 | [Screenplay writer](https://github.com/crewAIInc/crewAI-examples/tree/main/crews/screenplay_writer) | Documentary, comedic pharmacy video and short-form scripts | P1 | Index link only; archived upstream; path must be verified |
| EA-15 | [YouTube research agent](https://github.com/agno-agi/agno/blob/main/cookbook/examples/agents/youtube_agent.py) | Study editing rhythm/structure and collect cited research | P1 | Index link only; copyright/ToS, outdated link risk |
| EA-16 | [Social media agent](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/14-social-media-agent) | Caption, hook, storyboard synopsis, IG description | P1 | Source reviewed; does not render/edit actual videos |
| EA-17 | [Instagram post](https://github.com/crewAIInc/crewAI-examples/tree/main/crews/instagram_post) | Brand-aware social content for Eyad Studio | P2 | Index link only; archived upstream |
| EA-18 | [Game Builder Crew](https://github.com/crewAIInc/crewAI-examples/tree/main/crews/game-builder-crew) | Use as planning pattern for reader gamification/economy | P1-pattern | Archived Python game demo; **not** production Android 3D game engine |
| EA-19 | [Multi-agent debate](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/20-multi-agent-debate) | Challenge/Red Team review for UI/UX and novel story arcs | P1 | Source reviewed; need an outside evidence judge |
| EA-20 | [Document writer](https://github.com/ashishpatel26/500-AI-Agents-Projects/tree/main/agents/16-documentation-writer) | Architecture decision records, design and QA runbooks | P1 | Source reviewed; Python-specific AST |
| EA-21 | [Code assistant with LangGraph](https://github.com/langchain-ai/langgraph/blob/main/docs/docs/tutorials/code_assistant/langgraph_code_assistant.ipynb) | Fixing compile errors with test feedback and patch review | P1 | Index link only; avoid autonomous merge/build claims |
| EA-22 | [AgentEval](https://github.com/microsoft/autogen/blob/0.2/notebook/agenteval_cq_math.ipynb) | Judge prompt-to-code/agent answers against reference tasks | P1 | Index link only; old AutoGen 0.2 path |
| EA-23 | [AgentOps trace example](https://github.com/microsoft/autogen/blob/0.2/notebook/agentchat_agentops.ipynb) | Agent traceability, error budgets, cost accounting | P1 | Index link only; old AutoGen 0.2 path |
| EA-24 | [Agent Memory Guard](https://github.com/OWASP/www-project-agent-memory-guard) | Security guidance for persistent user/project memory | P1 | Linked repository confirmed accessible; not evaluated as dependency |
| EA-25 | [Citadel](https://github.com/SethGammon/Citadel) | Agent campaign lifecycle and postmortems, possible SID inspiration | P2 | Linked repo accessible; check its Claude Code-specific requirements |
| EA-26 | [Study partner](https://github.com/agno-agi/agno/blob/main/cookbook/examples/agents/study_partner.py) | Study aid for pharmacology learning and reading comprehension | P2 | Index link only; educational, not clinical authority |
| EA-27 | [Research scholar](https://github.com/agno-agi/agno/blob/main/cookbook/examples/agents/research_agent_exa.py) | Research curation for medicine and AI technology | P2 | Index link only; evaluate provenance and evidence standards |
| EA-28 | [Healthcare insights](https://github.com/harshhh28/hia) | Evaluate as **research-only** prototype | HOLD | Not vetted for clinical reliability, privacy, legal/regulatory use |
| EA-29 | [Medical diagnostic agent](https://github.com/ahmadvh/AI-Agents-for-Medical-Diagnostics) | No direct integration with pharmacy patient-facing workflows | HOLD | Clinical safety and consent barriers |
| EA-30 | [PII sanitization agent](https://github.com/teodorofodocrispin-cmyk/TrustBoost-PII-Sanitizer) | Explore data minimization techniques, not as a local privacy guarantee | HOLD | Reported third-party API transmission in repo issue #185 |

## Five first pilots

1. **PILOT-1: Reader Bug Intelligence** — read GitHub issues/PR metadata; suggest severity, reproducibility and owner, but make no changes without human approval. Acceptance: grounded citations to original issue, deduplicated groups, manually reviewed precision, no accidental destructive action.
2. **PILOT-2: Evidence-First Competitor Scanner** — fetch live competitor docs/issues/reviews before comparison. Acceptance: date/source for each feature assertion; separate facts, hypothesis and unsupported claims; retain screenshots or test evidence when permitted.
3. **PILOT-3: Android Test Proposal Assistant** — inspect only changed Kotlin/Compose modules and author draft tests. Acceptance: generated tests compile and execute in controlled CI; no P0 regression; user-facing Gate stays RED until verified.
4. **PILOT-4: Novel Factory Book Flow** — adapt chapters-as-state architecture from archived CrewAI example, integrating continuity cards, character memory, style revision, scene consequence, human editorial review. Acceptance: no invented canon or scene drift; authorship/rights rules; checkpoint/restart.
5. **PILOT-5: Eyad Studio Challenge Board** — debate/research agents critique draft features, but independent tests and human editor decide release. Acceptance: traceable objections and measurable acceptance criteria, not just LLM consensus.

## Architecture & QA gate

`Request → Intent/Permission → Source Retrieval → Planner → Specialist(s) → Evidence Validator → Human Review (when consequential) → Reversible PR → CI/Device QA → Promote`

- **Reusable interfaces:** `TaskSpec`, `Evidence[]`, `AgentResult`, `RiskFlags`, `CostBudget`, `QARecord` and `DecisionRecord`.
- **Required controls:** API keys via secure secrets manager, outbound network allowlist, repo-scoped read tokens, prompt injection defenses for untrusted websites/GitHub issues, rate/cost budget, logging with redacted secrets and personal data.
- **Source provenance:** agents must disclose missing evidence; old sample notebooks/paths are not proof of runnable code in 2026.
- **License:** audit each upstream link separately; MIT root license does **not** relicense third-party repositories.
- **No autonomous merge/deploy:** restrict agents to draft PRs/notes until test reports and the human approval gate pass.
- **Reader-specific:** keep paper curl, slide, paged and scroll modes independently testable; no research pilot may change production reader behavior.
- **Novel-specific:** do not confuse fluent language with good writing. Independent continuity and editorial checks remain mandatory.
- **Healthcare-specific:** never use unaudited agents as medical diagnosis or medication decision makers, nor expose real patient data to unapproved endpoints.

## Evidence & known limitations

- Catalog source: https://github.com/ashishpatel26/500-AI-Agents-Projects/blob/main/README.md
- 20 executable sample index: https://github.com/ashishpatel26/500-AI-Agents-Projects/blob/main/agents/README.md
- Sample dependency example: https://github.com/ashishpatel26/500-AI-Agents-Projects/blob/main/agents/01-web-research-agent/requirements.txt (older versions pinned; `gpt-4o-mini` hardcoded in code).
- `crewAIInc/crewAI-examples` is archived as of 2026-04-20: https://github.com/crewAIInc/crewAI-examples
- Catalog issue concerning external PII API: https://github.com/ashishpatel26/500-AI-Agents-Projects/issues/185
- Catalog issue about broken references: https://github.com/ashishpatel26/500-AI-Agents-Projects/issues/149

**Intake outcome:** reference saved, 30 candidate inspirations catalogued, 5 staged pilot proposals, **0** production integrations or installs. This document should be refreshed against latest upstream code before any pilot is launched.