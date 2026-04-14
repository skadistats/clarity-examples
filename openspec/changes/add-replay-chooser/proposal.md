## Why

Every example `Main.main` reads its replay from `args[0]`. That means:

- Running without an arg crashes with `ArrayIndexOutOfBoundsException: Index 0 out of bounds` — no help text, no chooser, unfriendly to newcomers.
- Running from an IDE requires one `.idea/runConfigurations/*.xml` per example with a hardcoded replay path — 40 files today, one per example, all brittle (every rename/move breaks them, as `split-examples-into-modules` just demonstrated).
- Examples that want to run many replays (`csgo2test`) resort to hardcoded absolute paths buried in `main()` — a sign the "one path from args[0]" model is too thin.

We also want `add-example-launcher` (follow-up) to have a reusable, self-contained replay picker it can reuse from a visual GUI, so the chooser must exist first as a shared component.

## What Changes

- Rename the `launcher/` subproject (created empty by `split-examples-into-modules`) to `shared/`, including its Java package (`skadistats.clarity.examples.launcher` → `skadistats.clarity.examples.shared`).
- Introduce `skadistats.clarity.examples.shared.ReplayChooser` in the renamed `shared/` subproject — a single entry point `choose(String[] args): String` that resolves and returns the replay *filename* (absolute path) for an example, with a fixed cascade:
  1. **`args[0]`** — if present and a readable file, use it.
  2. **`CLARITY_REPLAY` env var** — if set to a readable file, use it.
  3. **History** — reuse the single last-chosen replay shared across every example, read from and written to `java.util.prefs.Preferences` under `Preferences.userNodeForPackage(ReplayChooser.class)`, key `lastReplay`. One slot, global across the project.
  4. **Interactive chooser** — in a GUI environment, open the OS-native file picker (AWT `FileDialog`) rooted at `$rootDir/replays/`, preselecting the history entry when it exists, and record the selection back to history.
  5. **Headless + no history** — exit with a clear error.
- Stale history entries (path no longer exists) fall through to the chooser with no preselection. User-cancelled GUI returns `null` so the calling example can exit gracefully.
- History storage is the JDK's `java.util.prefs.Preferences` API — platform-native (Windows registry, macOS `~/Library/Preferences`, Linux `~/.java/.userPrefs/`) with zero custom persistence code. **One global string** for the whole project: no per-example history, no tags, no favorites, no lists. Minimal by design.
- **The chooser returns a filename, not a `Source`.** This means every example keeps its existing `new MappedFileSource(...)` line; the only thing that changes is the *source of the filename*. Example migration pattern:

  ```java
  // before
  try (Source source = new MappedFileSource(args[0])) { ... }

  // after
  String replay = ReplayChooser.choose(args);
  if (replay == null) return;
  try (Source source = new MappedFileSource(replay)) { ... }
  ```
- Make every content subproject (`examples`, `repro`, `dev`, `bench`) depend on `:shared`.

## Capabilities

### Modified Capabilities
- `project-structure`: the `launcher/` subproject from the prior change becomes `shared/`; contents and dependency graph are extended to host `ReplayChooser`.

### New Capabilities
- `examples-replay-chooser`: how examples obtain a replay `Source` — the args/env/history/GUI cascade, the history file format, and the behavior on headless/stale states.

## Impact

- **Build config**: `settings.gradle.kts` updates `:launcher` → `:shared`; each content subproject gains `implementation(project(":shared"))`.
- **Source tree**: 39 example `Main.java` files each lose one `new MappedFileSource(args[0])` call and gain `ReplayChooser.open(...)`. Imports adjust accordingly. The rest of each main method is untouched.
- **User-facing behavior**:
  - Running an example without any arg now opens a file dialog (or reuses last replay in headless).
  - Existing `./gradlew <name>Run --args "path"` invocations are unchanged in behavior (arg wins).
  - A new `CLARITY_REPLAY` env var is honored.
  - A new `~/.config/clarity-examples/history` file appears the first time an example runs.
- **csgo2test**: its hardcoded-path rig is orthogonal to the chooser — it does not go through `main(String[])` with a replay-path arg. We leave it alone in this change; the bulk migration applies to the other 38 examples.
- **Teaching code**: `examples/` subproject Mains still visibly show the Source → Runner → handoff dance; only the Source *acquisition* changes from `new MappedFileSource(args[0])` to `ReplayChooser.open(args)`. Pedagogy preserved.
- **Follow-up `add-example-launcher`**: will reuse `ReplayChooser` as the replay picker behind its visual UI.
- **Follow-up `normalize-run-configs`**: will no longer need a per-example replay path in every `.idea/runConfigurations/*.xml` — the chooser fills that in at runtime.
