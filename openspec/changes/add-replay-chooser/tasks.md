## 1. Rename launcher/ → shared/

- [x] 1.1 `git mv launcher shared`
- [x] 1.2 Rename the Java package stub: `launcher/src/main/java/skadistats/clarity/examples/launcher/` → `shared/src/main/java/skadistats/clarity/examples/shared/` (the `.gitkeep` moves along)
- [x] 1.3 Update `settings.gradle.kts`: `include(":examples", ":repro", ":dev", ":bench", ":launcher")` → `include(":examples", ":repro", ":dev", ":bench", ":shared")`
- [x] 1.4 Update `README.md` project-structure section: `launcher/` → `shared/` with a one-line description of its new role
- [x] 1.5 Update `CLAUDE.md` "Example Categories" section: `launcher/` → `shared/`

## 2. ReplayChooser component

- [x] 2.1 Create `shared/src/main/java/skadistats/clarity/examples/shared/ReplayChooser.java` with a single public static entry point `choose(String[] args): String` that returns the absolute replay path (or `null` on cancellation); history is global, with no owner parameter and no call-site inspection
- [x] 2.2 Implement the cascade: args[0] → CLARITY_REPLAY env → per-example history → (headless: use-or-error) / (GUI: chooser with preselect)
- [x] 2.3 Headless detection via `java.awt.GraphicsEnvironment.isHeadless()`
- [x] 2.4 GUI chooser: native OS file picker via AWT `FileDialog` rooted at `$rootDir/replays/`, preselecting the history entry when valid, Enter/Open confirms, Cancel returns `null`
- [x] 2.4a Ensure the chooser window surfaces on top of the IDE/console that launched it (`dialog.setAlwaysOnTop(true)`) so the user doesn't have to alt-tab to find it
- [x] 2.5 History reader + writer backed by `java.util.prefs.Preferences`: read via `Preferences.userNodeForPackage(ReplayChooser.class).get("lastReplay", null)`, write via `.put("lastReplay", absolutePath)`; wrap the write in a try/catch so `BackingStoreException` or similar degrades gracefully (log warning, continue)
- [x] 2.6 Record to history only after a successful interactive open; do not write from args/env paths
- [x] 2.7 Clear error message in headless + no-history path explaining the three ways to provide a replay
- [x] 2.8 Log the resolved path to stdout/log in headless + history path

## 3. Wire subprojects

- [x] 3.1 Add `implementation(project(":shared"))` to `examples/build.gradle.kts`
- [x] 3.2 Add `implementation(project(":shared"))` to `repro/build.gradle.kts`
- [x] 3.3 Add `implementation(project(":shared"))` to `dev/build.gradle.kts` (already depends on `:examples`)
- [x] 3.4 Add `implementation(project(":shared"))` to `bench/build.gradle.kts`
- [x] 3.5 Confirm `./gradlew :shared:build` succeeds

## 4. Migrate examples

The migration pattern for each example's `Main.main(String[] args)`:

```java
// BEFORE
try (Source source = new MappedFileSource(args[0])) { ... }

// AFTER
String replay = ReplayChooser.choose(args);
if (replay == null) return;
try (Source source = new MappedFileSource(replay)) { ... }
```

The `new MappedFileSource(...)` line, the surrounding try-with-resources, and all downstream code stay **unchanged**.

- [x] 4.1 Migrate every `examples/` Main that fits the single-replay-path model following the pattern above (22 files, `livesource` excluded — see 4.4)
- [x] 4.2 Migrate every `repro/` Main (both `issue289` and `issue350`)
- [x] 4.3 Migrate every `dev/` Main (12 files, `csgo2test` excluded — see 4.4)
- [x] 4.4 Exceptions (not migrated, by design):
  - `dev/csgo2test` — hardcoded absolute replay paths, multi-replay rig; doesn't go through args[0]
  - `examples/livesource` — takes two positional args (src replay + dst file); chooser resolves only a single replay path
  - `bench/entitybaseline`, `bench/eventdispatchbench`, `bench/propertychangebench` — custom multi-path batch mode with built-in replay defaults; interactive chooser would defeat the batch semantics
- [x] 4.5 (Benches) covered by 4.4 — all three benches stay as-is
- [x] 4.6 Keep the existing `skadistats.clarity.source.MappedFileSource` import; add a `skadistats.clarity.examples.shared.ReplayChooser` import in every migrated file

## 5. Update spec delta reflecting actual migration

- [x] 5.1 Exceptions enumerated in `specs/project-structure/spec.md` under the "Examples with specialized arg handling retain their existing behavior" scenario (csgo2test, livesource, 3 benches)

## 6. Verification

- [x] 6.1 `./gradlew clean build` passes
- [x] 6.2 Spot-check: `./gradlew :examples:allchatRun --args="replays/dota/s2/normal/<any>.dem"` still works unchanged
- [x] 6.3 Spot-check: `./gradlew :examples:allchatRun` (no args) opens the native OS file picker rooted at `replays/`
- [x] 6.4 Spot-check: `CLARITY_REPLAY=replays/dota/s2/normal/<any>.dem ./gradlew :examples:allchatRun` uses the env var and does not open a chooser
- [x] 6.5 Spot-check: run once, pick a replay; run again with no args; chooser preselects the previous replay (sourced from Preferences)
- [ ] 6.6 Spot-check: delete the previous replay file; run the example; chooser opens at `replays/` with no preselection (stale-entry fall-through) — deferred; covered by spec scenario, verified in code path
- [ ] 6.7 Spot-check: `java -Djava.awt.headless=true -cp ... <example-Main>` with a valid `lastReplay` pref → reuses last, logs the path; without the pref → exits with the clear error message — deferred; covered by spec scenario, verified in code path
- [x] 6.8 `openspec validate add-replay-chooser` passes
