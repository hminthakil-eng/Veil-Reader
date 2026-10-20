# Restore exact base dependency snapshot — PR558

Evidence: dependency review compared zero base snapshots to one head snapshot, classifying existing build-tool dependencies as additions. Source root build.gradle.kts is identical on main811f0d7d and product e182b46c. Read-only audit run39504483776/job118909872156 verified resolved BC family1.85 and actual AGP provider jar bcprov1.85. It does not establish that every graph entry is harmless.

Purpose: recompute the missing base snapshot using the repository's existing, trusted Dependency Baseline workflow, then rerun unchanged dependency review. This restores comparison input, without source/dependency changes, exclusions or advisory waivers.

Mission control: no checkout or application execution. Actions write is needed only to dispatch that existing workflow; contents read verifies main's exact SHA immediately before dispatch. No other secrets, writes, artifacts or external hosts. The existing canonical-main workflow retains its maintained permissions and commands.

Expected source: main811f0d7d521fc11f110638a100c315f698d52fd5. Root must verify the dispatched run's head SHA and resulting snapshot before consuming it. A concurrently moved main invalidates the expected-source result and must be diagnosed, not silently accepted.

Terminal state: inspect dispatch logs, exact baseline run and unchanged review result. Retire this temporary control definition after results are preserved. Keep task branch while its audit/repair evidence has review value.
