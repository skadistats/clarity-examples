package skadistats.clarity.examples.shared;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

/**
 * Resolves the replay file for an example invocation via a fixed cascade:
 * <ol>
 *   <li>{@code args[0]} if it points at a readable file</li>
 *   <li>{@code CLARITY_REPLAY} environment variable if set to a readable file</li>
 *   <li>History: the last interactively-selected replay, stored globally
 *       for this project in {@link java.util.prefs.Preferences} under
 *       {@code Preferences.userNodeForPackage(ReplayChooser.class)}, key
 *       {@code lastReplay}</li>
 *   <li>In a GUI environment: the native OS file picker (AWT
 *       {@link FileDialog}) rooted at {@code replays/}, preselecting the
 *       history entry when it still points at a readable file</li>
 *   <li>In a headless environment without usable history: a clear
 *       error message and {@link System#exit(int) System.exit(2)}</li>
 * </ol>
 *
 * <p>Usage from an example's {@code main}:
 * <pre>{@code
 *   public static void main(String[] args) throws Exception {
 *       String replay = ReplayChooser.choose(args);
 *       if (replay == null) return; // user cancelled the chooser
 *       try (Source source = new MappedFileSource(replay)) { ... }
 *   }
 * }</pre>
 *
 * <p>History is a single last-chosen replay shared across every example —
 * pick once, every example preselects that replay the next time it runs.
 */
public final class ReplayChooser {

    private static final Logger log = LoggerFactory.getLogger(ReplayChooser.class);

    private static final String PREF_KEY = "lastReplay";
    private static final String ENV_VAR = "CLARITY_REPLAY";

    private ReplayChooser() {
    }

    public static String choose(String[] args) {
        if (args != null && args.length > 0 && isReadableFile(args[0])) {
            return absolute(args[0]);
        }

        String env = System.getenv(ENV_VAR);
        if (env != null && isReadableFile(env)) {
            return absolute(env);
        }

        String history = readHistory();

        if (GraphicsEnvironment.isHeadless()) {
            if (history != null && isReadableFile(history)) {
                log.info("No replay arg; reusing last-used replay: {}", history);
                return history;
            }
            System.err.println();
            System.err.println("No replay selected.");
            System.err.println("Supply one via:");
            System.err.println("  * first CLI arg");
            System.err.println("  * CLARITY_REPLAY environment variable");
            System.err.println("  * run interactively (with a display) to pick one");
            System.exit(2);
            return null;
        }

        File preselect = (history != null && isReadableFile(history)) ? new File(history) : null;
        String picked = showDialog(preselect);
        if (picked == null) {
            return null;
        }
        writeHistory(picked);
        return picked;
    }

    private static boolean isReadableFile(String path) {
        if (path == null || path.isBlank()) return false;
        Path p = Paths.get(path);
        return Files.isRegularFile(p) && Files.isReadable(p);
    }

    private static String absolute(String path) {
        return Paths.get(path).toAbsolutePath().normalize().toString();
    }

    private static Preferences prefs() {
        return Preferences.userNodeForPackage(ReplayChooser.class);
    }

    private static String readHistory() {
        try {
            return prefs().get(PREF_KEY, null);
        } catch (Exception e) {
            log.warn("Could not read Preferences history: {}", e.toString());
            return null;
        }
    }

    private static void writeHistory(String path) {
        try {
            prefs().put(PREF_KEY, path);
        } catch (Exception e) {
            log.warn("Could not write Preferences history: {}", e.toString());
        }
    }

    private static String showDialog(File preselect) {
        File startDir;
        if (preselect != null && preselect.getParentFile() != null) {
            startDir = preselect.getParentFile();
        } else {
            File replays = new File("replays");
            startDir = replays.isDirectory() ? replays : new File(".");
        }

        FileDialog dialog = new FileDialog((Frame) null, "Select replay", FileDialog.LOAD);
        dialog.setDirectory(startDir.getAbsolutePath());
        if (preselect != null) {
            dialog.setFile(preselect.getName());
        }
        dialog.setAlwaysOnTop(true);
        dialog.setMultipleMode(false);
        dialog.setVisible(true); // blocks until the user confirms or cancels
        try {
            String file = dialog.getFile();
            if (file == null) {
                return null;
            }
            String dir = dialog.getDirectory();
            return new File(dir, file).getAbsolutePath();
        } finally {
            dialog.dispose();
        }
    }
}
