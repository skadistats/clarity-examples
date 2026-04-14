## 1. Example annotation

- [ ] 1.1 Add `skadistats.clarity.examples.shared.Example` annotation (class-level, retention RUNTIME, `@IndexAnnotated`) with fields `name: String`, `description: String`, `category: Category`
- [ ] 1.2 Add `skadistats.clarity.examples.shared.Category` enum with `DOCS`, `REPRO`, `DEV`, `BENCH`
- [ ] 1.3 Add the classindex annotation processor dep to `shared` if not already present via clarity's transitive deps
- [ ] 1.4 Annotate every example's `Main` class with `@Example(name=..., description=..., category=...)` — 39 annotations total, one per example, `name` equal to the directory name for muscle-memory continuity
- [ ] 1.5 Add a build-time check (Gradle task or test) that fails if any two `@Example.name` values collide

## 2. ExampleLauncher core

- [ ] 2.1 Create `skadistats.clarity.examples.shared.ExampleLauncher` with a static `main(String[] args)`
- [ ] 2.2 On startup, enumerate registered examples via `ClassIndex.getAnnotated(Example.class)` and build a registry keyed by `@Example.name`
- [ ] 2.3 Implement the arg mode: if `args[0]` matches a registered name, reflectively invoke `selectedClass.main(args[1..])` on a background thread (the example's own `ReplayChooser.open(args)` call handles resolution against any remaining args)
- [ ] 2.4 Implement the no-arg mode: open the Swing picker (§3)
- [ ] 2.5 On unknown name, exit with a clear error listing available names

## 3. Swing picker UI

- [ ] 3.1 Single-window JFrame with a JTree or JList grouped by `Category`
- [ ] 3.2 Detail pane showing the selected example's `description`
- [ ] 3.3 "Run" button, disabled until a selection exists; on click: reflectively invoke `selectedClass.main(new String[0])` on a background thread (the example's own `ReplayChooser.open(args)` call handles replay resolution)
- [ ] 3.4 Running state visible to the user (status bar, button label, or similar)
- [ ] 3.5 Console/log pane — optional for v1; can start with stdout going to the host terminal

## 4. Build wiring

- [ ] 4.1 Add `runtimeOnly(project(":examples"))`, `runtimeOnly(project(":repro"))`, `runtimeOnly(project(":dev"))`, `runtimeOnly(project(":bench"))` to `shared/build.gradle.kts`
- [ ] 4.2 Register a `launcher` (or agreed name) Gradle `JavaExec` task in `shared/build.gradle.kts` whose main class is `ExampleLauncher`, whose classpath is `shared`'s runtime classpath, and whose working directory is `rootProject.projectDir`
- [ ] 4.3 Confirm `./gradlew :shared:tasks --all | grep launcher` lists the task
- [ ] 4.4 Confirm name-collision check runs as part of `./gradlew build`

## 5. Spec delta updates

- [ ] 5.1 Record any naming clashes or exceptions encountered during annotation rollout in the spec delta

## 6. Verification

- [ ] 6.1 `./gradlew clean build` passes with the name-collision check enabled
- [ ] 6.2 `./gradlew launcher` opens the GUI; picking an example and clicking Run produces output from that example
- [ ] 6.3 `./gradlew launcher --args="allchat"` runs `allchat` without showing the picker
- [ ] 6.4 `./gradlew launcher --args="nope"` exits with an error listing all valid example names
- [ ] 6.5 Spot-check: running the same example via `./gradlew allchatRun` and via the launcher both read/write the same `~/.config/clarity-examples/history` entry
- [ ] 6.6 `openspec validate add-example-launcher` passes
