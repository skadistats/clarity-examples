## Why

Even after `add-replay-chooser` lands, running an example still requires invoking a specific `<name>Run` Gradle task (CLI) or a specific per-example `.idea/runConfigurations/*.xml` (IDE). Neither helps a human who wants to *browse* what examples exist, read a one-line description of each, and just click one.

We also want `normalize-run-configs` (later proposal) to replace the 40 brittle per-example run configs with one — an IDE run config that points at a single `ExampleLauncher` main class, with the example name passed as an arg (or chosen in the GUI when no arg is given).

## What Changes

- Introduce `skadistats.clarity.examples.shared.ExampleLauncher` in the `shared/` subproject — a Swing application that:
  - Shows a browsable list of all registered examples, grouped by category (docs / repro / dev / bench), with each entry's one-line description.
  - On selecting an example, reflectively invokes its `Main.main(new String[0])` on a background thread.
  - Replay resolution is delegated to the example's own `ReplayChooser.choose(args)` call (which, on empty args, runs the full cascade: env → history → GUI). The launcher itself never calls the chooser directly — so the cascade behaves identically whether the example was started by the launcher, by `./gradlew <name>Run`, or by an IDE run config.
  - Can be launched with no args (opens the picker) or with `args[0]` = example identifier (skips the picker and dispatches directly).
- Introduce a `@Example(name, description, category)` annotation on each example's `Main`, backed by `org.atteo.classindex.IndexAnnotated` so the annotation processor already in use by clarity produces a compile-time index the launcher can read at runtime.
- Add runtime-only dependencies from `:shared` to each content subproject (`:examples`, `:repro`, `:dev`, `:bench`) so the launcher can see and reflectively invoke every example's `Main` class.
- Add one Gradle run task (e.g. `./gradlew launcher` or `./gradlew :shared:launcher`) to run the launcher GUI.

## Capabilities

### Modified Capabilities
- `project-structure`: adds runtime-only deps from `:shared` to the content subprojects; adds the `launcher` Gradle task; consolidates the four identical `logback.xml` files under the content subprojects into a single copy in `shared/src/main/resources/`.
- `examples-replay-chooser` (from `add-replay-chooser`): unchanged in API but now has a second caller (the launcher) in addition to each example's `main`.

### New Capabilities
- `examples-launcher`: how a user (or IDE run config) enumerates, picks, and runs any example via a single entry point.

## Impact

- **Source tree**: 39 `Main.java` files each gain one `@Example(...)` annotation on the class declaration. A new `ExampleLauncher.java` + small registry + Swing UI code lands in `shared/`.
- **Resources**: the four byte-identical `logback.xml` files under `examples/`, `repro/`, `dev/`, `bench/` are deleted; a single copy lives in `shared/src/main/resources/`. Every subproject already compile-depends on `:shared`, so the same config governs both `./gradlew <name>Run` and `./gradlew launcher`. Drift risk vanishes; the launcher no longer loads multiple configs from one classpath.
- **Build config**: `shared/build.gradle.kts` gains `runtimeOnly(project(":examples"))` et al. plus the `launcher` run task.
- **Classpath**: running the launcher costs loading all content subprojects at runtime. This is acceptable — the launcher is an end-user tool, not a library.
- **Dependency graph**: compile-time arrows still flow inward (`examples → shared`); runtime-only arrows flow the other way (`shared →runtime→ examples/...`) for reflective dispatch. No compile cycles.
- **Follow-up `normalize-run-configs`**: will replace the 40 per-example `.idea/runConfigurations/*.xml` with one run config that invokes `ExampleLauncher`, optionally with the example identifier as an arg.
