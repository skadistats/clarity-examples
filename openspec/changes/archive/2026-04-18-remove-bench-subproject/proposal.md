## Why

Benchmarking has been migrated to the standalone `clarity-bench` project, making the `bench/` subproject and the `src/jmh/` JMH harness in `clarity-examples` dead weight. Removing them reduces project scope to what it actually is — example and teaching code — and eliminates stale CLAUDE.md documentation that points developers to the wrong place.

## What Changes

- **BREAKING** Remove `bench/` subproject (`entitybaseline`, `eventdispatchbench`, `propertychangebench` examples)
- **BREAKING** Remove `src/jmh/` JMH harness (`EntityStateParseBench`, `Main`, `ReportWriter`, `ContextWriter`)
- Remove JMH plugin (`me.champeau.jmh`) and JMH dependency wiring from root `build.gradle.kts`
- Remove `"bench"` from `verifyExampleNames` task's subproject list in root `build.gradle.kts`
- Remove `bench` include from `settings.gradle.kts`
- Update `clarity-examples/CLAUDE.md`: remove `bench/` table row, remove JMH harness paragraph, replace "Cross-version benchmarking" section with pointer to `clarity-bench`
- Update `clarity/CLAUDE.md`: add `clarity-bench` to "Related repos" section
- Create `clarity-bench/CLAUDE.md`: usage, layout, key flags, replay corpus policy

## Capabilities

### New Capabilities

None — this is a pure removal and documentation update.

### Modified Capabilities

- `project-structure`: the `bench/` subproject is being removed from the project layout

## Impact

- `clarity-examples`: `bench/` subproject deleted, `src/jmh/` deleted, root build files updated, CLAUDE.md updated
- `clarity`: CLAUDE.md updated (no code changes)
- `clarity-bench`: CLAUDE.md created (no code changes)
- No downstream API impact — bench code is not imported by any other subproject
