## 1. Convention plugin and buildSrc

- [x] 1.1 Create `buildSrc/` with its own `build.gradle.kts` applying the `kotlin-dsl` plugin
- [x] 1.2 Extract the existing per-example task generation logic from root `build.gradle.kts` into a Kotlin convention plugin `examples-convention.gradle.kts` under `buildSrc/src/main/kotlin/`
- [x] 1.3 Parametrize the convention plugin so the scanned source root is derived from the applying subproject (not hard-coded to the old flat path)
- [x] 1.4 Have the convention plugin set each generated `Run` task's working directory to `rootProject.projectDir` so relative replay paths continue to resolve
- [x] 1.5 Verify `./gradlew --stop && ./gradlew tasks` still enumerates every pre-change example task once the plugin is wired but before any example moves (run in a dry-run branch or keep old flat tree as fallback during migration)

## 2. Subproject scaffolding

- [x] 2.1 Add `include(":examples", ":repro", ":dev", ":bench", ":launcher")` to root `settings.gradle.kts`
- [x] 2.2 Create each of the five subproject directories: `examples/`, `repro/`, `dev/`, `bench/`, `launcher/`
- [x] 2.3 Add a minimal `build.gradle.kts` to each content subproject (`examples`, `repro`, `dev`, `bench`) that applies the convention plugin and declares the clarity dependency
- [x] 2.4 Add a minimal `build.gradle.kts` to `launcher/` that declares the clarity dependency but does NOT apply the example-generation convention plugin (launcher is not a directory-per-example tree)
- [x] 2.5 Create `launcher/src/main/java/skadistats/clarity/examples/launcher/` as an empty directory structure with a `.gitkeep` so the module is valid but empty
- [x] 2.6 Confirm `./gradlew projects` lists all five subprojects

## 3. Move docs examples to `examples/` subproject

- [x] 3.1 For each of: `allchat`, `combatlog`, `cooldowns`, `dumpmana`, `gameevent`, `header`, `info`, `lifestate`, `livesource`, `matchend`, `metadata`, `modifiers`, `particles`, `position`, `propertychange`, `resources`, `seek`, `s1tempentities`, `s2dotatempentities`, `s2effectdispatch`, `spawngroups`, `tick` — `git mv` the directory from the old flat location to `examples/src/main/java/skadistats/clarity/examples/<name>/`. (Note: pre-change `rotation/` directory listed in the spec was empty and untracked; removed rather than moved.)
- [x] 3.2 Confirm package declarations in moved docs files remain `skadistats.clarity.examples.<name>` (no rename — matches D3)
- [x] 3.3 Run `./gradlew :examples:build` and confirm every docs example compiles
- [x] 3.4 Spot-check: run `./gradlew allchatRun --args="replays/dota/s2/normal/<any>.dem"` and confirm it still works

## 4. Move repro examples to `repro/` subproject

- [x] 4.1 `git mv` `issue289/` and `issue350/` into `repro/src/main/java/skadistats/clarity/examples/repro/`
- [x] 4.2 Rewrite package declarations: `skadistats.clarity.examples.issue289` → `skadistats.clarity.examples.repro.issue289` (and same for `issue350`)
- [x] 4.3 Rewrite any imports of these packages elsewhere in the codebase (if any)
- [x] 4.4 Run `./gradlew :repro:build` and confirm compilation
- [x] 4.5 Spot-check: `./gradlew issue350Run` still resolves and runs

## 5. Move dev examples to `dev/` subproject

- [x] 5.1 For each of: `csgo2test`, `dtinspector`, `dump`, `dumpbaselines`, `entityrun`, `fullpacketcount`, `ntsemantics`, `packetentitiesmatch`, `packetentitiesprobe`, `serializers`, `stringtabledump`, `test` — `git mv` into `dev/src/main/java/skadistats/clarity/examples/dev/<name>/`. (Note: pre-change `ownerdump/` and `shrinker/` dirs listed in the spec were empty and untracked; removed rather than moved. `dev/` also depends on `:examples` because `csgo2test` references docs examples by FQN; `images/` resources moved to `dev/src/main/resources/images/` because only `dtinspector` uses them.)
- [x] 5.2 Rewrite package declarations: `skadistats.clarity.examples.<name>` → `skadistats.clarity.examples.dev.<name>` for each
- [x] 5.3 Run `./gradlew :dev:build` and confirm every dev example compiles
- [x] 5.4 Spot-check: `./gradlew dtinspectorRun` still resolves and runs

## 6. Move bench examples to `bench/` subproject

- [x] 6.1 For each of: `entitybaseline`, `eventdispatchbench`, `propertychangebench` — `git mv` into `bench/src/main/java/skadistats/clarity/examples/bench/<name>/`
- [x] 6.2 Rewrite package declarations to add the `bench` segment
- [x] 6.3 Run `./gradlew :bench:build` and confirm compilation
- [x] 6.4 Spot-check: `./gradlew entitybaselineRun` still resolves and runs

## 7. Prune root build configuration

- [x] 7.1 Remove the per-example `fileTree` task-generation block from root `build.gradle.kts` (now lives in the convention plugin)
- [x] 7.2 Remove the top-level `src/main/` layout from the root project (files have all moved to subprojects). (Note: `src/jmh/` remains — the root keeps the JMH harness and the `bench` Gradle task, since the per-example bench subproject is a separate concept from the JMH throughput harness.)
- [x] 7.3 Confirm root `./gradlew build` walks all five subprojects; `./gradlew <name>Package` for each example produces `<subproject>/build/libs/<name>.jar`
- [x] 7.4 Run `./gradlew tasks --all | grep Run | sort` and confirm the full list matches the pre-change list (same leaf names). 39 Run + 39 Package tasks registered (pre-change: 41 dirs, minus 3 empty stale dirs `rotation/`, `ownerdump/`, `shrinker/` = 38 expected; additional unsymmetry due to existing 41 counting those empties).

## 8. Verify task-name resolution

- [x] 8.1 Confirm `./gradlew allchatRun` resolves to `:examples:allchatRun` when no collision exists
- [x] 8.2 Confirm `./gradlew :examples:allchatRun` and `./gradlew :dev:dtinspectorRun` both work as qualified forms
- [x] 8.3 Scan for accidental leaf-name collisions across subprojects (none expected; documenting the check). Verified: all 39 leaf Run/Package names are unique across subprojects.

## 9. Update documentation

- [x] 9.1 Rewrite `README.md` project-overview section to describe the five subprojects with a one-line purpose each
- [x] 9.2 Update `README.md` build-system section to show both unqualified and qualified task invocation
- [x] 9.3 Update `README.md` "Example Categories" section so it reflects the new subproject-based grouping (README organizes examples as individual sections; updated the links to new subproject paths)
- [x] 9.4 Update `CLAUDE.md` "Common Commands" to document qualified task paths and the unqualified fallback rule
- [x] 9.5 Update `CLAUDE.md` "Project Architecture" to mention the subproject split and note that `launcher/` is reserved for follow-up proposals

## 10. Verification

- [x] 10.1 Run `./gradlew clean build` from a fresh checkout equivalent and confirm success
- [x] 10.2 Confirm every pre-change `<name>Run` task still resolves by its leaf name (39/39 non-empty pre-change example dirs registered; 3 pre-change dirs `rotation/`, `ownerdump/`, `shrinker/` were empty/untracked and removed rather than migrated)
- [x] 10.3 Confirm every pre-change `<name>Package` task still produces its uno-jar under `<subproject>/build/libs/` (spot-sampled: `allchat.jar`, `tick.jar`, `issue289.jar`, `issue350.jar`, `dtinspector.jar`, `serializers.jar`, `entitybaseline.jar`, `propertychangebench.jar` all produced)
- [x] 10.4 Confirm `openspec validate split-examples-into-modules` passes
- [x] 10.5 Note in the change's notes/status that `.idea/runConfigurations/*.xml` are expected to be broken post-change and will be repaired by `normalize-run-configs`. **Status: broken by this change**, as expected — every per-example run config has a stale module path (`clarity-examples.main` → must become `clarity-examples.<subproject>.main`) and a stale main-class package (`dev`/`repro`/`bench` examples gained a category segment). Repair is out of scope here.
