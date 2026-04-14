## ADDED Requirements

### Requirement: Shared depends on content subprojects at runtime

The `shared/` subproject SHALL declare `runtimeOnly` dependencies on each of the four content subprojects (`examples`, `repro`, `dev`, `bench`) so that `ExampleLauncher` can reflectively enumerate and invoke registered examples. `shared/` SHALL NOT declare compile-time dependencies on these subprojects.

#### Scenario: Runtime arrow exists

- **WHEN** a developer reads `shared/build.gradle.kts`
- **THEN** its `dependencies {}` block includes `runtimeOnly(project(":examples"))`, `runtimeOnly(project(":repro"))`, `runtimeOnly(project(":dev"))`, and `runtimeOnly(project(":bench"))`

#### Scenario: No compile arrow

- **WHEN** a developer tries to `import` a class from `:examples` / `:repro` / `:dev` / `:bench` inside any file under `shared/src/main/java/`
- **THEN** the build fails, because `shared`'s compile classpath does not include those subprojects

### Requirement: Launcher Gradle task

The build SHALL expose a single Gradle task that runs `ExampleLauncher` with `shared`'s full runtime classpath.

#### Scenario: Task exists

- **WHEN** a developer runs `./gradlew :shared:tasks --all`
- **THEN** a task named `launcher` (or equivalent agreed name) is listed, whose effect is to run `skadistats.clarity.examples.shared.ExampleLauncher` with all content subprojects on the runtime classpath

#### Scenario: Task accepts args

- **WHEN** a developer runs `./gradlew launcher --args="allchat"`
- **THEN** the launcher skips the picker and runs the `allchat` example (via `ReplayChooser`'s resolution cascade)
