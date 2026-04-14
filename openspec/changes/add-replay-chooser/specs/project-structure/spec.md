## MODIFIED Requirements

### Requirement: Subproject layout

The clarity-examples Gradle build SHALL be organized as five subprojects at the repository root: `examples`, `repro`, `dev`, `bench`, and `shared`. Each subproject SHALL have its own `src/main/java/` source tree and its own `build.gradle.kts`.

#### Scenario: Root settings file lists every subproject

- **WHEN** a developer reads `settings.gradle.kts`
- **THEN** it includes exactly these subprojects: `examples`, `repro`, `dev`, `bench`, `shared`

#### Scenario: Each subproject is independently buildable

- **WHEN** a developer runs `./gradlew :<subproject>:build` for any of the five subprojects
- **THEN** Gradle compiles that subproject's sources without error and without requiring any other subproject to be compiled first (except for transitive dependencies declared in `build.gradle.kts`)

### Requirement: Category assignment

Every example directory SHALL live in exactly one of the four content subprojects (`examples`, `repro`, `dev`, `bench`). The `shared` subproject SHALL host reusable components consumed by the content subprojects; it SHALL NOT itself contain per-example directories.

#### Scenario: Docs examples land in examples/

- **WHEN** a contributor looks up a teaching example such as `allchat`, `combatlog`, `cooldowns`, `dumpmana`, `gameevent`, `header`, `info`, `lifestate`, `livesource`, `matchend`, `metadata`, `modifiers`, `particles`, `position`, `propertychange`, `resources`, `seek`, `s1tempentities`, `s2dotatempentities`, `s2effectdispatch`, `spawngroups`, or `tick`
- **THEN** it is located under `examples/src/main/java/skadistats/clarity/examples/<name>/`

#### Scenario: Issue reproducers land in repro/

- **WHEN** a contributor looks up an issue reproducer such as `issue289` or `issue350`
- **THEN** it is located under `repro/src/main/java/skadistats/clarity/examples/repro/<name>/`

#### Scenario: Diagnostic tools land in dev/

- **WHEN** a contributor looks up a maintainer diagnostic tool such as `csgo2test`, `dtinspector`, `dump`, `dumpbaselines`, `entityrun`, `fullpacketcount`, `ntsemantics`, `packetentitiesmatch`, `packetentitiesprobe`, `serializers`, `stringtabledump`, or `test`
- **THEN** it is located under `dev/src/main/java/skadistats/clarity/examples/dev/<name>/`

#### Scenario: Benchmarks land in bench/

- **WHEN** a contributor looks up a throughput benchmark such as `entitybaseline`, `eventdispatchbench`, or `propertychangebench`
- **THEN** it is located under `bench/src/main/java/skadistats/clarity/examples/bench/<name>/`

#### Scenario: Shared subproject hosts reusable components

- **WHEN** a contributor opens `shared/src/main/java/skadistats/clarity/examples/shared/`
- **THEN** it contains components reused by the content subprojects (for example, `ReplayChooser`) rather than per-example directories

### Requirement: Java package reflects subproject

Java package names for examples SHALL include the subproject name as a segment for `repro`, `dev`, and `bench`. Examples in `examples` SHALL keep their base packages. Code in `shared` SHALL live under `skadistats.clarity.examples.shared`.

#### Scenario: Docs examples keep bare packages

- **WHEN** a reader opens `examples/.../combatlog/Main.java`
- **THEN** the file's `package` declaration is `skadistats.clarity.examples.combatlog`

#### Scenario: Categorized examples carry their category in the package

- **WHEN** a reader opens `repro/.../issue350/Main.java`, `dev/.../dtinspector/Main.java`, or `bench/.../entitybaseline/Main.java`
- **THEN** the file's `package` declaration is respectively `skadistats.clarity.examples.repro.issue350`, `skadistats.clarity.examples.dev.dtinspector`, or `skadistats.clarity.examples.bench.entitybaseline`

#### Scenario: Shared components carry the shared package

- **WHEN** a reader opens a file under `shared/src/main/java/`
- **THEN** its `package` declaration begins with `skadistats.clarity.examples.shared`

## ADDED Requirements

### Requirement: Content subprojects depend on shared

Each content subproject (`examples`, `repro`, `dev`, `bench`) SHALL declare a compile-time dependency on `:shared` so that examples can consume `ReplayChooser` and future shared components.

#### Scenario: Example imports shared component

- **WHEN** a developer inspects any content subproject's `build.gradle.kts`
- **THEN** its `dependencies {}` block contains `implementation(project(":shared"))`

#### Scenario: Example Main imports ReplayChooser

- **WHEN** a developer opens any standard example's `Main.java` after migration
- **THEN** it imports `skadistats.clarity.examples.shared.ReplayChooser` and calls `choose(args)` to obtain the replay filename; the existing `new MappedFileSource(...)` line remains, just taking the chooser's returned path as its argument

#### Scenario: Examples with specialized arg handling retain their existing behavior

- **WHEN** a developer opens an example whose arg model doesn't match the single-replay-path shape — specifically `dev/csgo2test` (hardcoded multi-replay rig), `examples/livesource` (takes two positional args: src and dst), and the three benches in `bench/` (custom multi-path batch mode with defaults)
- **THEN** those examples are not migrated to `ReplayChooser` and retain their existing arg handling; this is explicitly permitted
