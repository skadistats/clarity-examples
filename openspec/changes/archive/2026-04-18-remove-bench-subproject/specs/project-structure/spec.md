## MODIFIED Requirements

### Requirement: Subproject layout

The clarity-examples Gradle build SHALL be organized as four subprojects at the repository root: `examples`, `repro`, `dev`, and `shared`. Each subproject SHALL have its own `src/main/java/` source tree and its own `build.gradle.kts`. The `bench/` subproject and the `src/jmh/` JMH harness SHALL NOT be present.

#### Scenario: Root settings file lists every subproject

- **WHEN** a developer reads `settings.gradle.kts`
- **THEN** it includes exactly these subprojects: `examples`, `repro`, `dev`, `shared`

#### Scenario: Each subproject is independently buildable

- **WHEN** a developer runs `./gradlew :<subproject>:build` for any of the four subprojects
- **THEN** Gradle compiles that subproject's sources without error and without requiring any other subproject to be compiled first (except for transitive dependencies declared in `build.gradle.kts`)

### Requirement: Category assignment

Every example directory SHALL live in exactly one of the three content subprojects (`examples`, `repro`, `dev`). The `shared` subproject SHALL host reusable components consumed by the content subprojects; it SHALL NOT itself contain per-example directories. Throughput benchmarks belong in the separate `clarity-bench` repository, not in `clarity-examples`.

#### Scenario: Docs examples land in examples/

- **WHEN** a contributor looks up a teaching example such as `allchat`, `combatlog`, `cooldowns`, `dumpmana`, `gameevent`, `header`, `info`, `lifestate`, `livesource`, `matchend`, `metadata`, `modifiers`, `particles`, `position`, `propertychange`, `resources`, `seek`, `s1tempentities`, `s2dotatempentities`, `s2effectdispatch`, `spawngroups`, or `tick`
- **THEN** it is located under `examples/src/main/java/skadistats/clarity/examples/<name>/`

#### Scenario: Issue reproducers land in repro/

- **WHEN** a contributor looks up an issue reproducer such as `issue289` or `issue350`
- **THEN** it is located under `repro/src/main/java/skadistats/clarity/examples/repro/<name>/`

#### Scenario: Diagnostic tools land in dev/

- **WHEN** a contributor looks up a maintainer diagnostic tool such as `csgo2test`, `dtinspector`, `dump`, `dumpbaselines`, `entityrun`, `fullpacketcount`, `ntsemantics`, `packetentitiesmatch`, `packetentitiesprobe`, `serializers`, `stringtabledump`, or `test`
- **THEN** it is located under `dev/src/main/java/skadistats/clarity/examples/dev/<name>/`

#### Scenario: Shared subproject hosts reusable components

- **WHEN** a contributor opens `shared/src/main/java/skadistats/clarity/examples/shared/`
- **THEN** it contains components reused by the content subprojects (for example, `ReplayChooser`) rather than per-example directories

### Requirement: Content subprojects depend on shared

Each content subproject (`examples`, `repro`, `dev`) SHALL declare a compile-time dependency on `:shared` so that examples can consume `ReplayChooser` and future shared components.

#### Scenario: Example imports shared component

- **WHEN** a developer inspects any content subproject's `build.gradle.kts`
- **THEN** its `dependencies {}` block contains `implementation(project(":shared"))`

#### Scenario: Examples with specialized arg handling retain their existing behavior

- **WHEN** a developer opens an example whose arg model doesn't match the single-replay-path shape — specifically `dev/csgo2test` (hardcoded multi-replay rig) and `examples/livesource` (takes two positional args: src and dst)
- **THEN** those examples are not migrated to `ReplayChooser` and retain their existing arg handling; this is explicitly permitted

### Requirement: Shared depends on content subprojects at runtime

The `shared/` subproject SHALL declare `runtimeOnly` dependencies on each of the three content subprojects (`examples`, `repro`, `dev`) so that `ExampleLauncher` can reflectively enumerate and invoke registered examples. `shared/` SHALL NOT declare compile-time dependencies on these subprojects.

#### Scenario: Runtime arrow exists

- **WHEN** a developer reads `shared/build.gradle.kts`
- **THEN** its `dependencies {}` block includes `runtimeOnly(project(":examples"))`, `runtimeOnly(project(":repro"))`, and `runtimeOnly(project(":dev"))`

#### Scenario: No compile arrow

- **WHEN** a developer tries to `import` a class from `:examples` / `:repro` / `:dev` inside any file under `shared/src/main/java/`
- **THEN** the build fails, because `shared`'s compile classpath does not include those subprojects

## REMOVED Requirements

### Requirement: Category assignment — bench scenario

**Reason**: The `bench/` subproject has been migrated to the standalone `clarity-bench` repository. Throughput benchmarks no longer live in `clarity-examples`.

**Migration**: Use `clarity-bench` at `/home/spheenik/projects/clarity/clarity-bench`. Run `./gradlew :v5.0.0:run --args="--replays-root /home/spheenik/projects/replays"` (publish to mavenLocal first for snapshot candidates).
