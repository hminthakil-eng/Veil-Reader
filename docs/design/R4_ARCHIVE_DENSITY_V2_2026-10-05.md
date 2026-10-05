# R4 — Archive Density / Retrieval-First Pass v2

Date: 2026-10-05
Base: current `grand-forge/arena-app-hardening-v1`

## Product problem

Archive had strong retrieval capability but the first viewport was dominated by
architectural header and permanently visible controls before books appeared.

## Implemented

- populated Archive header condenses by default;
- empty Archive keeps the full authored entrance;
- Search remains direct;
- Reading State becomes compact;
- Gallery / Shelves / Index stay directly reachable;
- Collection / Series / Sort move behind one explicit Filters disclosure;
- active secondary filter count remains visible when collapsed;
- Reset remains direct;
- Gallery physical book object is capped independently from readable metadata;
- fallback/generated cover can suppress duplicate title/author caption when the
  same identity is immediately below;
- Gallery details action becomes a semantic 48dp icon instead of a verbose row;
- 200% text grows metadata without inflating the physical cover.

## Preserved

No retrieval capability is removed:
Search, Reading State, Collection, Series, Sort, Gallery, Shelves, Index, Reset,
Settings and Import remain available.

## Acceptance

- Android CI
- Storage Instrumentation + Grayfog capture artifact
- Persian/English
- 100%/200% text
- no clipping
- all essential controls >=48dp
- first real book materially earlier in populated Archive
