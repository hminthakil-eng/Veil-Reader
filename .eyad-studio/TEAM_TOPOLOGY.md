# Veil Reader — Specialist Team Topology

Management owns priorities, cross-team decisions, integration and final acceptance. Specialist teams own depth in their domain and are expected to propose improvements, not merely execute tickets.

## 1. Research & Insight
Studies reader behavior, competitors, manga ecosystems, accessibility, technical risk, performance and product assumptions. It returns evidence and recommendations.

## 2. Product & UX Design
Owns flows, interaction models, information architecture, reader ergonomics, accessibility intent, onboarding and design-system consistency.

## 3. Game Systems
Owns the RPG rules: Origins, Paths/classes, attributes, quests, rituals, progression, rewards, anti-cheese logic and the relationship between genuine reading and game state.

## 4. Settlement & Economy
Owns the wasteland-to-city loop: Water, Coins, materials, farming, construction, building unlocks, upgrade trees, sinks/sources, pacing and anti-grind balance.

## 5. Worldbuilding & Narrative
Owns lore, NPCs, factions, companions, world events, Chronicle, environmental storytelling and consequences of settlement growth. The story must explain why reading restores the world.

## 6. Reader Platform
Owns EPUB, PDF, CBZ/image manga, RTL/LTR manga, webtoon, local library, progress persistence, annotation quality and reader performance. Reading quality always outranks decorative game UI.

## 7. Manga Source Platform
Owns the replaceable ContentProvider boundary, source manager, permissions, updates, health state, downloads and provider failure isolation. Providers may never directly modify RPG state.

## 8. Visual World & Motion
Owns settlement states, environment art direction, building families, map readability, atmospheric transitions and reduced-motion alternatives. Every tier should visibly transform the land.

## 9. Android Engineering & Architecture
Owns Kotlin/Compose implementation, Room/DataStore, migrations, integration boundaries, tests, performance tooling and release-safe code structure.

## 10. QA, Balance & Release
Owns regression, accessibility, economy simulation, exploit resistance, long-session behavior, migration/backup safety and release gates.

## Cross-team pods
For major features Management may form temporary pods. Example: `Wasteland Reclamation Pod` = Game Systems + Settlement Economy + World Narrative + Visual World + Development + QA.

## Operating rule
Teams may create internal proposals whenever they see an opportunity. A proposal must state: user value, connection to the core Reading RPG, cost/risk, dependencies, acceptance criteria and what should be removed or delayed if scope increases. Management may greenlight aligned, reversible proposals within the approved autonomy boundary.
