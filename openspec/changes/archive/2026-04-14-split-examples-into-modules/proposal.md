## Why

The `examples/` directory has grown to 41 flat sibling directories that mix four very different kinds of things: teaching examples users learn from, reproducers for specific GitHub issues, maintainer-only diagnostic tools, and throughput benchmarks. New contributors can't tell which is which, and the single flat list makes it impossible to build or package only one category. Separating the categories now — before the accompanying launcher and replay-tree cleanups — gives every later change a stable place to land.

## What Changes

- Introduce four Gradle subprojects under the root of `clarity-examples`, each with its own source set:
  - `examples/` — teaching/showcase code (22 current entries)
  - `repro/` — issue reproducers (2 current entries, more expected over time)
  - `dev/` — maintainer diagnostic tools and dumpers (15 current entries)
  - `bench/` — JMH-style throughput benchmarks (3 current entries)
- Introduce a fifth subproject `launcher/` as an empty-but-scaffolded module. Its source code is delivered by follow-up proposals (`add-example-launcher`, `add-replay-runner`); this change only creates the Gradle module so later proposals have somewhere to land.
- Move every current example directory out of `src/main/java/skadistats/clarity/examples/…` and into the matching subproject's `src/main/java/…` tree. Package name per example is unchanged beyond the category segment (e.g. `skadistats.clarity.examples.combatlog` → `skadistats.clarity.examples.combatlog` stays in the `examples` subproject; `skadistats.clarity.examples.issue350` → `skadistats.clarity.examples.repro.issue350`).
- Move the per-directory Gradle task-generation logic (currently in the root `build.gradle.kts`) into a shared convention plugin under `buildSrc/` so each subproject's build file stays short.
- Task names remain the leaf directory name (`./gradlew allchatRun`, `./gradlew issue350Run`) when unique across subprojects; use qualified paths (`./gradlew :examples:allchatRun`) only when collisions arise. **BREAKING** for any muscle memory or scripts that assumed `./gradlew <name>Run` always resolves unqualified.
- Update `README.md` and `CLAUDE.md` to document the new layout, the five subprojects, and the qualified task-path form.
- The in-tree `replays/` symlink stays at the project root; all subprojects reach it via `$rootDir/replays`.

## Capabilities

### New Capabilities
- `project-structure`: how the clarity-examples Gradle project is organized — which subprojects exist, what goes in each, how tasks are generated, and how contributors decide where a new example belongs.

### Modified Capabilities
<!-- none; specs/ is empty -->

## Impact

- **Build config**: `settings.gradle.kts` grows to include five subprojects; root `build.gradle.kts` shrinks as its per-example logic moves into `buildSrc/`; each subproject gets a minimal `build.gradle.kts` that applies the convention plugin.
- **Source tree**: every current example directory moves from `src/main/java/skadistats/clarity/examples/<name>/` to `<subproject>/src/main/java/skadistats/clarity/examples/<name>/`. No `.java` file logic changes — only paths and, where relevant, `package` declarations.
- **IDE run configs**: `.idea/runConfigurations/*.xml` paths and module references become stale. This proposal does not fix them — the follow-up `normalize-run-configs` proposal handles that once `launcher/` has content.
- **External callers**: any published documentation or scripts invoking `./gradlew <name>Run` continue to work for unique names. Qualified forms (`:examples:<name>Run`) are the canonical syntax going forward.
- **Published artifacts**: per-example uno-jar packaging is unaffected — each subproject continues to produce `<name>.jar` for each of its example directories.
- **Follow-up proposals unblocked**: `add-example-launcher`, `add-replay-runner`, `normalize-run-configs` all depend on `launcher/` existing as a subproject.
