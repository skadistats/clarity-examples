## 1. Remove bench/ subproject

- [x] 1.1 Delete `bench/src/` tree entirely.
- [x] 1.2 Delete `bench/build.gradle.kts`.
- [x] 1.3 Delete the `bench/` directory (should now be empty).

## 2. Remove src/jmh/ JMH harness

- [x] 2.1 Delete `src/jmh/java/skadistats/clarity/bench/EntityStateParseBench.java`.
- [x] 2.2 Delete `src/jmh/java/skadistats/clarity/bench/Main.java`.
- [x] 2.3 Delete `src/jmh/java/skadistats/clarity/bench/ReportWriter.java`.
- [x] 2.4 Delete `src/jmh/java/skadistats/clarity/bench/ContextWriter.java`.
- [x] 2.5 Delete the now-empty `src/jmh/` directory tree (`src/jmh/java/skadistats/clarity/bench/`, `src/jmh/resources/` if present, `src/jmh/` itself). If `src/` becomes empty, delete it too.

## 3. Clean up root build.gradle.kts

- [x] 3.1 Remove the `id("me.champeau.jmh") version "0.7.3"` plugin line from the `plugins {}` block.
- [x] 3.2 Remove the `jmhRuntimeOnly(...)` dependency line from `dependencies {}`.
- [x] 3.3 Remove `"bench"` from the `subprojects` list inside the `verifyExampleNames` task.
- [x] 3.4 Verify the root `build.gradle.kts` still compiles (`./gradlew help` is sufficient).

## 4. Clean up settings.gradle.kts

- [x] 4.1 Remove `include("bench")` from `settings.gradle.kts`.
- [x] 4.2 Verify `./gradlew projects` no longer lists `:bench`.

## 5. Update clarity-examples/CLAUDE.md

- [x] 5.1 Remove the `bench/` row from the Subprojects table.
- [x] 5.2 Remove the sentence about the JMH harness at `src/jmh/java/skadistats/clarity/bench/` (currently after the Subprojects table).
- [x] 5.3 Replace the entire "Cross-version benchmarking" section with a pointer: benchmarking lives in `clarity-bench` at `/home/spheenik/projects/clarity/clarity-bench`; see that project's README for usage.

## 6. Update clarity/CLAUDE.md

- [x] 6.1 Add `clarity-bench` to the "Related repos" section as a standalone perf tool (not a composite-build downstream). Include: path `/home/spheenik/projects/clarity/clarity-bench`, purpose (cross-version JMH harness), and the note that it is NOT wired as a composite build — it resolves clarity via `mavenLocal()` for snapshot candidates.

## 7. Create clarity-bench/CLAUDE.md

- [x] 7.1 Create `/home/spheenik/projects/clarity/clarity-bench/CLAUDE.md` covering:
  - How to run: `./gradlew :vX.Y.Z:run --args="--replays-root /path/to/replays"`
  - For snapshot candidates: `cd ../clarity && ./gradlew publishToMavenLocal && cd -` first
  - `--record` flag to persist results into `results/`
  - Key filters: `--workload`, `--impl`, `--variant` (repeatable, validated)
  - `--list-replays` for corpus inspection
  - Replay corpus: manifest-pinned (`replays/MANIFEST.sha256`), adding a replay obligates backfill across all versions
  - Layout orientation: `harness/` (shared bench code), `vX.Y.Z/` (pinned release subprojects), `results/` (tracked baselines)

## 8. Verify build

- [x] 8.1 Run `./gradlew build` in `clarity-examples`; confirm clean compile with no `:bench` tasks.
- [x] 8.2 Run a quick smoke test on any example (e.g., `./gradlew :examples:headerRun --args "replays/dota/s2/normal/1560289528.dem"`).
