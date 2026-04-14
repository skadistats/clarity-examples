# examples-launcher Specification

## Purpose

Defines the visual launcher for clarity-examples: how runnable examples register themselves via annotation, how `ExampleLauncher` enumerates and dispatches them, and where the launcher lives in the subproject layout. Provides a single entry point that lets contributors and reviewers discover and run any registered example without memorizing per-example Gradle task names.

## Requirements

### Requirement: Example registration via annotation

Every runnable example SHALL carry a `@Example(name, description, category)` annotation on its `Main` class. The annotation SHALL be `@IndexAnnotated` so that the classindex annotation processor produces a compile-time index of registered examples.

#### Scenario: Name is unique across the build

- **WHEN** the build compiles all content subprojects
- **THEN** every `@Example` annotation has a `name` value that is unique across `:examples`, `:repro`, `:dev`, and `:bench`; duplicate names fail the build

#### Scenario: Category assigns the example to a group

- **WHEN** a contributor sets `@Example(category = Category.DOCS)` on an example in the `examples/` subproject
- **THEN** the launcher renders that example under the "Docs" group

#### Scenario: Description shows in the launcher

- **WHEN** the launcher displays an example entry
- **THEN** the `description` field of its `@Example` annotation is shown as a one-line summary

### Requirement: ExampleLauncher picks and runs

`skadistats.clarity.examples.shared.ExampleLauncher` SHALL be the single entry point used by the visual launcher GUI and by IDE run configs.

#### Scenario: No-arg launch opens the picker

- **WHEN** `ExampleLauncher.main(String[])` is invoked with no args AND the runtime is not headless
- **THEN** a Swing window opens listing all `@Example`-annotated `Main` classes grouped by `Category`, with descriptions shown

#### Scenario: Run from picker dispatches via the example's own main

- **WHEN** a user selects an example in the picker and clicks "Run"
- **THEN** the launcher reflectively invokes `selectedClass.main(new String[0])` on a background thread
- **AND** the example's own `ReplayChooser.choose(args)` call (inside its main) runs the standard cascade against the empty args, identical to how the example would behave if started directly

#### Scenario: Arg-based launch skips the picker

- **WHEN** `ExampleLauncher.main` is invoked with `args[0]` matching the `name` of a registered example
- **THEN** the picker is not shown and the matching example's `main` is invoked with `args[1..]` forwarded

#### Scenario: Unknown example name

- **WHEN** `ExampleLauncher.main` is invoked with an `args[0]` that does not match any registered `@Example.name`
- **THEN** the launcher exits with a clear error listing all available example names

### Requirement: Shared hosts the launcher

The `shared/` subproject SHALL host `ExampleLauncher` and hold runtime-only dependencies on every content subproject so the launcher can reflectively load example Main classes.

#### Scenario: Runtime-only dep arrows

- **WHEN** a developer inspects `shared/build.gradle.kts`
- **THEN** its `dependencies {}` block contains `runtimeOnly(project(":examples"))`, `runtimeOnly(project(":repro"))`, `runtimeOnly(project(":dev"))`, and `runtimeOnly(project(":bench"))`

#### Scenario: Compile cycles remain forbidden

- **WHEN** any code under `shared/src/main/java/` is compiled
- **THEN** it does not import any class from `:examples`, `:repro`, `:dev`, or `:bench` at compile time; all such references are reflective
