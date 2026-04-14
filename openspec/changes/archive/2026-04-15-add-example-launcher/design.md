## Context

After `add-replay-chooser` lands, the remaining run-time friction is "which example do I want to run today?". A developer who remembers the name uses `./gradlew <name>Run`. Everyone else either reads the README, clicks through `src/main/java/...`, or asks someone. `normalize-run-configs` can't happen cleanly until there's a single entry point the IDE can point at regardless of which example runs.

This change introduces that entry point and the registry behind it.

## Goals / Non-Goals

**Goals:**
- One executable, `ExampleLauncher`, that lists all examples and runs the selected one in-process.
- Self-describing examples: `@Example(name, description, category)` on each `Main`.
- Reuse `ReplayChooser` so replay resolution behaves identically whether an example was launched directly or through the launcher.
- Keep each example's `Main.main(String[])` the canonical entry point (the launcher just calls into it reflectively).

**Non-Goals:**
- Not a console TUI, a CLI chooser, or a web UI. Swing only, matching `dtinspector`.
- Not a test runner, benchmark harness, or live-stream tool. It just picks + runs.
- No process isolation — examples run in the launcher's JVM.
- No hot-reload / restart of a running example. Run one, close it, run another.

## Decisions

### D1. Discovery via `@Example` + classindex
**Choice:** Each example's `Main` carries `@Example("allchat", "Parse chat messages", Category.DOCS)`. The annotation is `@IndexAnnotated`, so the classindex annotation processor already on the build classpath emits a compile-time index the launcher enumerates at startup via `ClassIndex.getAnnotated(Example.class)`.

**Alternatives considered:**
- Manual registry: 39 lines to maintain centrally, churns on every add/remove.
- Gradle-generated resource: good for task names but lacks description/category fields; forcing Gradle to scan source for descriptions is over-engineering.
- Classpath scan for `Main.class`: fragile, slow, picks up stray classes.

**Rationale:** `@Example` matches clarity's own idiom (`@OnMessage`, `@OnGameEvent`, etc. all use classindex). The annotation carries the human-readable metadata the GUI needs to display. Zero runtime scan cost — classindex is a compile-time manifest.

### D2. Example identifier = annotation `name`
**Choice:** `ExampleLauncher` resolves `args[0]` against the `@Example` `name` field, not the directory name or class FQN.

**Rationale:** Names already match Gradle task leaf names (that's how `./gradlew allchatRun` works today), so developer muscle memory carries over.

**Implication:** `name` values MUST be unique across all content subprojects. The build validates this (see task list).

### D3. Launcher runs examples in-process, on a background thread
**Choice:** The launcher invokes `<ExampleClass>.main(new String[]{replayPath})` via reflection on a worker thread.

**Rationale:** In-process keeps the feedback loop fast and means stdout/logging/exceptions surface in the launcher's console or optionally its UI. A worker thread prevents the Swing EDT from blocking.

**Trade-off:** An example that `System.exit()`s kills the launcher too. Acceptable — this is dev tooling, not production. If ever an issue, we can trap `System.exit` via `SecurityManager` or a wrapper `ClassLoader`.

### D4. Shared hosts the launcher, with runtime-only deps on content subprojects
**Choice:** `shared/build.gradle.kts` gains `runtimeOnly(project(":examples"))`, `runtimeOnly(project(":repro"))`, `runtimeOnly(project(":dev"))`, `runtimeOnly(project(":bench"))`.

**Rationale:** Compile-time arrows still flow inward (content subprojects import `shared.ReplayChooser`). Runtime-only arrows the other way let `shared`'s `ExampleLauncher` reflectively find classes in the content subprojects without creating compile cycles.

### D5. Launcher never calls ReplayChooser directly
**Choice:** When the user clicks Run in the launcher, the launcher reflectively invokes `selectedClass.main(new String[0])` on a background thread. The example's own `ReplayChooser.choose(args)` call — inside its main — runs the cascade (args → env → history → GUI) with the empty args it was handed.

**Rationale:** The launcher stays stateless about replays. Replay resolution semantics are defined in exactly one place — the example's own call site — regardless of how the example was started. `./gradlew allchatRun`, an IDE run config pointing at `allchat.Main`, and the launcher picking `allchat` all reach the same `ReplayChooser.choose(args)` with the same empty args, so behavior is identical by construction. History is already global by spec (`examples-replay-chooser` §"ReplayChooser API shape"), so both paths read and write the same `lastReplay` entry without needing any owner/call-site mechanism.

**Implication:** The launcher does not need, and does not get, a compile-time dependency on `ReplayChooser`. Its only compile-time dependency is the `@Example` annotation + classindex.

### D6. Category values
**Choice:** `Category` is a simple enum in `skadistats.clarity.examples.shared` with values `DOCS`, `REPRO`, `DEV`, `BENCH`. The GUI groups by this.

**Alternative:** derive category from the package segment. Rejected — harder to evolve if we ever want sub-categories, and the annotation field is one line of code.

### D7. Consolidate `logback.xml` under `shared/`
**Choice:** Delete `logback.xml` from `examples/`, `repro/`, `dev/`, `bench/` and keep a single copy in `shared/src/main/resources/`. Every content subproject already compile-depends on `:shared`, so `shared`'s runtime jar (carrying `logback.xml`) is on the classpath for both `./gradlew <name>Run` and `./gradlew launcher`.

**Rationale:** The four files are byte-identical today, so this is a hygiene change, not a behavioral one. The launcher's unified classpath would otherwise contain four identical configs and logback would pick one arbitrarily (with a "multiple bindings"-style warning). Collapsing to one source of truth eliminates that noise and removes a future drift trap where someone tweaks `dev`'s config and silently gets a different config under the launcher.

**Alternatives considered:**
- *Leave all four, add a fifth under `shared`*: five near-identical files, same drift risk, nothing gained.
- *Leave all four as-is*: harmless today, fragile the moment anyone diverges one of them.
- *Launcher-specific override via `-Dlogback.configurationFile`*: good lever if we ever need launcher-only logging, but orthogonal — not a reason to keep four duplicate resource files around.

**Trade-off:** Loses the (hypothetical, unused) ability for a subproject to carry its own logback config. If per-subproject log tuning is ever wanted, the right knob is a per-logger level in the shared config (MDC or `<logger name="…">` entries), not four parallel files.

### D8. UI shape (first cut)
**Choice:** A single-window Swing UI:
- Left pane: tree/list of examples grouped by category, selected entry shows its description.
- Bottom toolbar: `Run` button (disabled until selection) + maybe a "clear history" action.
- On `Run`, a background thread invokes the example. Subsequent `Run` clicks while one is running are disabled (or queue? — open question).

We intentionally leave the precise layout to implementation — this design documents the functional shape, not pixel-level specifics.

## Risks / Trade-offs

- **[R1] Example `main()` that reads from stdin** would deadlock the launcher. None of the current 39 do that, but worth a lint-style check before the migration closes.
- **[R2] Classpath bloat when running the launcher**: all 39 examples plus transitive deps are loaded. That's fine for a dev tool; launcher startup cost is still seconds, not minutes.
- **[R3] Name collisions across subprojects**: enforced by a build-time check (fail the build if two `@Example` annotations share a `name`).
- **[R4] An example that calls `System.exit`** kills the launcher. Accept; fix later with a sandboxing wrapper if it becomes a real irritant.
- **[R5] Headless environments can't use the launcher GUI** — acceptable, since the launcher is explicitly a GUI tool. `args[0]` mode still works.

## Open Questions

- Should the launcher also render an example's Javadoc preamble as extended description? (Probably nice-to-have; out of scope for v1.)
- Does the launcher need a "re-run with same replay" button distinct from "run"? Or is history + Enter sufficient? (Lean: skip; history covers it.)
- What happens if a bench example expects custom args (e.g. a replay list)? Pass through a text field? Or keep the launcher replay-only and leave such benches invokable only via CLI? (Lean: keep the launcher replay-only.)
