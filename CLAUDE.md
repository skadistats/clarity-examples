# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository contains example code for the Clarity replay parser (https://github.com/skadistats/clarity), which parses Dota 2 and CS:GO replays. The project uses Gradle with Kotlin DSL and Java 17.

## Build System

The project uses **Gradle** (not Maven, despite having older Maven files). Build configuration is in `build.gradle.kts`.

### Common Commands

Build a specific example:
```bash
./gradlew <exampleName>Package
```

Run an example directly without packaging:
```bash
./gradlew <exampleName>Run --args "path/to/replay.dem"
```

Build all:
```bash
./gradlew build
```

The built uno-jars (self-contained JARs with all dependencies) are located in `build/libs/<exampleName>.jar` and can be run with:
```bash
java -jar build/libs/<exampleName>.jar replay.dem
```

### Running Single Tests

Gradle tasks are auto-generated per example directory in `src/main/java/skadistats/clarity/examples/`. Each example has:
- `<exampleName>Run` - Run the example
- `<exampleName>Package` - Build the uno-jar

## Project Architecture

### Event-Based Processing Model

Clarity uses an annotation-driven event system. The core workflow:

1. **Create a Source** - `MappedFileSource` (fastest, requires local file) or `InputStreamSource` (works with any InputStream)
2. **Create a Runner** - `SimpleRunner` (single pass) or `ControllableRunner` (allows seeking, runs in separate thread)
3. **Add Processors** - POJOs with annotated methods that handle events
4. **Run** - Call `runner.runWith(processor)`

### Key Annotations

**Event Listeners:**
- `@OnMessage(MessageClass.class)` - Receive protobuf messages from replay
- `@OnEntityCreated`, `@OnEntityUpdated`, `@OnEntityDeleted` - Entity lifecycle events
- `@OnGameEvent` - Game events (provided by clarity's GameEvents processor)
- Custom events can be defined (see lifestate example)

**Event Providers:**
- `@Provides({EventClass.class})` - Declares a processor can raise custom events
- `@Initializer(EventClass.class)` - Initialize event before use
- `@UsesEntities` - Processor needs entity system

### Context Object

First parameter of all event handlers. Provides:
- `ctx.getProcessor(Class)` - Get reference to another processor in the run
- `ctx.getTick()` - Current replay tick
- `ctx.getEngineType()` - Source 1 or Source 2
- `ctx.getBuildNumber()` - Server build number (Source 2 only)
- `ctx.createEvent(EventClass, ParamTypes...)` - Create custom events

### Entity System

Dota 2/CS:GO replays contain networked entities (heroes, creeps, game state, etc.). Entity structure:
- **Send tables** (Source 1) / **Serializers** (Source 2) - Define entity schemas
- Properties accessed via `Entity.getProperty()` or field paths
- Schema varies between game versions, stored in replay for compatibility

### Creating Custom Events

See `src/main/java/skadistats/clarity/examples/lifestate/SpawnsAndDeaths.java` for complete example:
1. Create custom event annotations (`@OnEntitySpawned`, etc.)
2. Add `@Provides` to processor class
3. Use `@Initializer` methods to create Event objects
4. Listen to base events (`@OnEntityCreated`, etc.)
5. Process data and call `event.raise(params)`

## Important Implementation Notes

### Dependencies
- Clarity version: 3.1.3
- Requires `annotationProcessor("org.atteo.classindex:classindex:3.13")` for annotation processing
- Logback for logging (configured in `src/main/resources/logback.xml`)

### Logging
Enable logging for specific packages in `src/main/resources/logback.xml`. Example:
```xml
<logger name="clarity.entities" level="debug" />
```

### Replay Data
- This repo has a symlink `replays/` pointing to `/home/spheenik/projects/replays` for test data
- Examples expect a replay file path as first argument
- Source 2 replays: Dota 2 (modern), CS:GO 2
- Source 1 replays: Dota 2 (old)

### Quick Info Without Iteration
For basic match info (players, picks, bans, winner), use `Clarity.infoForFile(path)` which returns `CDemoFileInfo` without iterating the replay.

## Example Categories

The `src/main/java/skadistats/clarity/examples/` directory contains many examples:
- **allchat** - Parse chat messages (simple @OnMessage example)
- **combatlog** - Combat log replication
- **matchend** - Scoreboard at game end (uses ControllableRunner.seekToTick)
- **lifestate** - Custom event provider for spawn/death tracking
- **info** - Quick metadata extraction without iteration
- **dtinspector** - Interactive send table browser (GUI)
- **position**, **modifiers**, **particles**, **tempentities** - Various entity features
- **seek**, **tick** - Runner control examples

Each example is self-contained with a Main.java entry point.
