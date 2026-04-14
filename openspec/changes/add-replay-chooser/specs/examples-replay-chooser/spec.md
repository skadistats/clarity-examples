## ADDED Requirements

### Requirement: Unified replay resolution

Examples SHALL obtain the replay filename for their invocation via a single shared component (`ReplayChooser`) that applies a fixed resolution cascade and returns the absolute path as a `String`.

#### Scenario: CLI arg wins

- **WHEN** `args[0]` is set and points to a readable file
- **THEN** `ReplayChooser` returns `args[0]`, consults neither env nor history, and does not open a GUI

#### Scenario: Env var fallback

- **WHEN** `args` is empty AND the `CLARITY_REPLAY` environment variable is set to a readable file
- **THEN** `ReplayChooser` returns the value of `$CLARITY_REPLAY` and does not consult history or open a GUI

#### Scenario: Headless with usable history

- **WHEN** `args` is empty AND `CLARITY_REPLAY` is unset AND the runtime is headless AND the history file has an entry for this example AND the entry points at a readable file
- **THEN** `ReplayChooser` logs the resolved path to stdout/log and returns it

#### Scenario: Headless without usable history

- **WHEN** `args` is empty AND `CLARITY_REPLAY` is unset AND the runtime is headless AND the history entry is missing or stale
- **THEN** `ReplayChooser` exits the example with a clear, human-readable error explaining how to supply a replay (arg, env var, or run interactively)

#### Scenario: GUI with history

- **WHEN** `args` is empty AND `CLARITY_REPLAY` is unset AND the runtime is not headless AND the history has a valid entry
- **THEN** a native OS file picker (AWT `FileDialog`) opens rooted at `$rootDir/replays/`, with the history entry preselected
- **AND** pressing Enter or clicking Open confirms the selection, records it back to history, and returns the absolute path

#### Scenario: GUI without history

- **WHEN** `args` is empty AND `CLARITY_REPLAY` is unset AND the runtime is not headless AND the history entry is missing or stale
- **THEN** a native OS file picker opens rooted at `$rootDir/replays/` with no preselection
- **AND** the selection, once confirmed, is recorded to history and its absolute path returned

#### Scenario: Chooser cancellation

- **WHEN** the user cancels the file picker
- **THEN** `ReplayChooser` returns `null` without raising an exception, leaving it to the calling example to exit gracefully

### Requirement: History persistence via `java.util.prefs.Preferences`

`ReplayChooser` SHALL persist exactly one last interactively-selected replay path for the project using the JDK's `java.util.prefs.Preferences` API, under the node returned by `Preferences.userNodeForPackage(ReplayChooser.class)` with the key `lastReplay`. History is global across every example — not keyed by caller class or example.

#### Scenario: Single global entry

- **WHEN** `ReplayChooser.choose(args)` records a confirmed interactive selection
- **THEN** it calls `Preferences.userNodeForPackage(ReplayChooser.class).put("lastReplay", path)` with the absolute replay path, overwriting any prior value

#### Scenario: Single global lookup

- **WHEN** `ReplayChooser.choose(args)` needs to consult history
- **THEN** it calls `Preferences.userNodeForPackage(ReplayChooser.class).get("lastReplay", null)` and treats a `null` result as "no history entry"

#### Scenario: Picking in one example preselects in every other

- **GIVEN** the user has just confirmed a selection while running example A
- **WHEN** the user next runs example B with no args and no env var
- **THEN** the chooser preselects the same replay the user picked in A

#### Scenario: Absolute paths only

- **WHEN** `ReplayChooser` writes a path to history
- **THEN** the stored string is the absolute filesystem path of the selected replay

#### Scenario: Backing store errors are non-fatal

- **WHEN** `Preferences` cannot write (for example, a locked-down Windows registry or a read-only home on Linux)
- **THEN** `ReplayChooser` logs a warning and returns the chosen path to the caller anyway; subsequent reads returning `null` are treated as "no history entry"

### Requirement: Recording policy

`ReplayChooser` SHALL record to history only when an interactive selection is confirmed by the user.

#### Scenario: Arg/env paths do not rewrite history

- **WHEN** a replay is resolved via `args[0]` or `CLARITY_REPLAY`
- **THEN** the history file is not modified for that example

#### Scenario: Recording happens at selection time

- **WHEN** the user confirms a selection in the file picker
- **THEN** the history is updated with the absolute path of the selection *before* `choose()` returns

#### Scenario: Cancelled selection does not rewrite history

- **WHEN** the user cancels the file picker
- **THEN** the history is not modified

### Requirement: ReplayChooser API shape

`ReplayChooser` SHALL expose a single static entry point, `choose(String[] args): String`, consumed from each example's `main` method. It SHALL return the absolute path of the resolved replay, or `null` if the user cancelled an interactive chooser dialog. The chooser takes no owner parameter and performs no call-site inspection — history is global.

#### Scenario: Caller opens the returned filename

- **WHEN** an example calls `String replay = ReplayChooser.choose(args)` and receives a non-null value
- **THEN** the caller constructs a `Source` with its existing `new MappedFileSource(replay)` line, wrapped in its existing `try (Source source = …)` block

#### Scenario: Caller handles cancellation

- **WHEN** `ReplayChooser.choose(args)` returns `null`
- **THEN** the calling example may return from `main` immediately without constructing a `Source` and without raising an exception
