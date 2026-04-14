## Context

`split-examples-into-modules` reorganized the repo into five subprojects and scaffolded an empty `launcher/` module as a landing pad for "launcher + runner code from follow-up proposals". This change populates the module and, while at it, renames it to a name that actually describes what lives there.

Today, every example has this pattern:
```java
public static void main(String[] args) throws Exception {
    try (Source source = new MappedFileSource(args[0])) {
        new SimpleRunner(source).runWith(new Main());
    }
}
```
No-arg invocation crashes. The `.idea/runConfigurations/*.xml` files encode a replay path per example, which means any repo reorg (like the one we just did) invalidates all 40. The `csgo2test` example, which needs to try many replays, works around this by hardcoding absolute paths in `main()`.

We want one piece of code that resolves "which replay?" with all the ways a developer might want to answer that question, so no individual example has to reinvent it, and so the follow-up visual launcher has a reusable dependency.

## Goals / Non-Goals

**Goals:**
- Unify replay resolution across all examples: `args[0]` → `CLARITY_REPLAY` → history → GUI chooser.
- Never crash on "no arg" for a normal desktop run; fall through to the chooser.
- Preserve existing CLI invocation unchanged when an arg IS provided.
- Provide a reusable component (`ReplayChooser`) that `add-example-launcher` will depend on.
- Keep teaching code's main() method visibly showing Source → Runner → handoff.
- Minimalism: one global last-chosen replay shared across every example. No per-example history, no tags, no favorites, no multi-entry lists.

**Non-Goals:**
- Not a boilerplate-elimination library. The Source creation line is the only thing that changes in each example. `SimpleRunner`/`ControllableRunner`/`runWith` calls stay verbatim.
- Not touching `csgo2test`'s hardcoded-path rig (it doesn't go through the standard args[0] path).
- Not removing `.idea/runConfigurations/*.xml` — that's `normalize-run-configs` later.
- Not introducing an annotation on examples (that's `add-example-launcher`).
- Uses the OS-native file picker via AWT `FileDialog` — no custom Swing UI.

## Decisions

### D1. Resolution cascade order
**Choice:** `args[0]` → `CLARITY_REPLAY` env var → per-example history → (headless: use-or-error) / (GUI: chooser with preselect).

**Rationale:**
- `args[0]` first because it's the most explicit and preserves all existing `./gradlew <name>Run --args` invocations.
- Env var second because it's useful for bulk scripts ("run every example against this one replay") and CI-style automation without rewriting every example's args.
- History third because it's the ergonomic default — developers typically iterate on the same replay.
- Chooser is the final human-in-the-loop fallback.

### D2. History stored in `java.util.prefs.Preferences`, one global entry
**Choice:** The last interactively-selected replay path is persisted via `java.util.prefs.Preferences.userNodeForPackage(ReplayChooser.class)` under the key `lastReplay`. One string for the whole project, shared across every example. No custom file format.

**Rationale:**
- Platform-native persistence with zero extra code: Windows registry (`HKCU\Software\JavaSoft\Prefs\...`), macOS `~/Library/Preferences/...plist`, Linux `~/.java/.userPrefs/...prefs.xml`. The JDK handles all of it.
- One slot is the simplest possible model. Picking a replay in any example sets it for every other example.
- Clearing state is a single `prefs.remove("lastReplay")` call (or the platform-native equivalent of deleting one preferences key).

**Alternatives considered:**
- Per-example history keyed by caller class (via `StackWalker`): earlier iteration of this design. Rejected in favor of the global model because simplicity across the rest of the spec (no tags, no favorites, no lists) implies the same principle should apply to the key — one replay, one slot. Developers typically iterate on one replay across many examples; a per-example slot adds state without matching usage.
- Flat text file at `~/.config/clarity-examples/history`: human-readable, but opens cross-platform location questions (Windows `%APPDATA%`? leading-dot dir?) and needs hand-rolled parse/write/directory-create code. Rejected because the trade-off isn't worth the bytes.

**Trade-off:** The stored data is not human-readable / editable with a text editor. Users who need to reset state call the Preferences API (or, on Windows, delete the relevant registry key). Acceptable for a dev tool.

### D3. One global string, overwritten on each selection
**Choice:** Exactly one replay path for the whole project, stored as a single `String` under the `lastReplay` key. Each confirmed selection overwrites the prior value. No per-example slots, no list, no timestamp, no metadata.

**Rationale:** Explicit user preference — the simplest possible model. If future needs emerge (per-example affinity, multi-replay history, tagging) they arrive as their own proposal.

### D4. Stale history fall-through
**Choice:** If the history entry points at a file that no longer exists, behave as if there's no history entry — open the chooser with no preselection in GUI mode, error in headless mode.

**Rationale:** Replay files get moved, renamed, and pruned all the time in this repo. Silent fall-through is less surprising than errors.

### D5. Headless detection
**Choice:** `java.awt.GraphicsEnvironment.isHeadless()`.

**Rationale:** Standard JDK API, no new deps. Works for CI, `-Djava.awt.headless=true`, and environments without a display.

### D6. OS-native file picker via AWT `FileDialog` at `$rootDir/replays/`
**Choice:** Use `java.awt.FileDialog` (not Swing `JFileChooser`). On Linux this delegates to the GTK file chooser; on macOS to `NSOpenPanel`; on Windows to the Win32 common file dialog. Root the dialog at the repo's `replays/` directory. Preselect the last-used replay (from history) when it exists — preselection does not require the file to be under `replays/`.

**Rationale:**
- The dialog looks exactly like every other file picker on the user's desktop. No custom Swing styling, no Metal L&F, no FlatLaf dep, no JavaFX bootstrap. Zero new dependencies.
- `FileDialog` is modal and native; Enter/Open confirms, Escape/Cancel closes it cleanly.
- `replays/` is where everyone keeps their test data in this repo.

**Alternatives considered:**
- Swing `JFileChooser` with `UIManager.setSystemLookAndFeel`: still looks foreign on modern Linux desktops (tested: user rejected).
- FlatLaf + `JFileChooser`: nicer cross-platform styling, but adds a ~500KB dependency.
- JavaFX `FileChooser`: ~50MB of per-platform deps plus Application bootstrap. Overkill for one dialog.

**Trade-off:** `FileDialog`'s API is narrower than `JFileChooser` (no programmatic custom filter UI, no preview pane), but we don't need any of that for "pick one .dem file".

### D7. ReplayChooser returns a filename, not a `Source`
**Choice:** `ReplayChooser.choose(String[] args): String`. Returns the absolute path of the chosen replay, or `null` if the user cancels the interactive chooser.

**Rationale:** Keeping the existing `new MappedFileSource(...)` line intact in every example preserves a pedagogical moment — a reader still sees the step where a `Source` is constructed from a path. The chooser becomes a distinct, clearly separate "where does the filename come from?" step, which is arguably *more* readable than the previous "opaque thing that happens to return a Source". The caller continues to own the resulting `Source`'s lifecycle via its own try-with-resources.

No owner parameter and no `StackWalker` — because history is global (D2), the chooser does not need to identify its caller.


### D8. Record to history on interactive selection
**Choice:** Write to history only after the interactive chooser returns a user-confirmed selection. Args/env paths do not write to history (they're explicit, not a default-for-next-time). Recording happens as part of `choose()`, before the filename is returned — the chooser has no visibility into whether the caller will successfully open it afterwards.

**Rationale:** History is the developer's "last thing I picked interactively". CLI/env passes shouldn't silently rewrite that. Whether the subsequent `new MappedFileSource(path)` succeeds is the caller's concern; history shouldn't depend on caller-side outcomes.

### D9. Rename `launcher/` → `shared/`
**Choice:** The subproject created empty by `split-examples-into-modules` is renamed before its first content lands.

**Rationale:** With both a passive helper (`ReplayChooser`, consumed by examples) and an active CLI (`ExampleLauncher` in the follow-up) landing here, "launcher" described only half of it. "shared" is honest about the module's role: shared components the example subprojects depend on. Dependency arrows also flow more naturally (examples → shared) than they would with a module called "launcher".

## Risks / Trade-offs

- **[R1] First-run UX surprise.** A developer expecting a crash may now see an unexpected file dialog. Mitigated by the chooser being obvious (native OS file picker, titled) and by CLI invocations still working exactly as before.
- **[R2] Tests / CI.** If any downstream script runs examples headlessly with no arg and no env, behavior changes from "crash immediately" to "error with clear message about no replay". Acceptable — the new error is strictly more informative.
- **[R3] History file permissions on shared hosts.** Not a real concern for this project (single-user maintainer machines), but the history path is XDG-ish (`~/.config/...`) and the writer should tolerate missing directories by creating them.
- **[R4] Preselection may point at a path outside `replays/`.** `FileDialog` handles that fine — the current directory just adjusts to the preselected file's parent.

## Open Questions

- Should the chooser also accept glob patterns in `args[0]` (e.g. for easy multi-replay runs)? Assumption: no, that belongs in `csgo2test` / a future multi-run feature.
- Should `CLARITY_REPLAY` support a list of paths (space-separated)? Assumption: no, scalar only.
