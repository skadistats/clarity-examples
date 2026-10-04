package skadistats.clarity.examples.shared;

import com.formdev.flatlaf.FlatLightLaf;
import org.atteo.classindex.ClassIndex;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

/**
 * Entry point that lists every {@link Example}-annotated {@code Main} on the classpath and runs one of them
 * in-process.
 * <ul>
 *   <li>With {@code args[0]} = example name: runs that example on a worker thread, forwarding the remaining
 *       arguments (usually the replay path). Unknown name: lists the examples, exit code 1.</li>
 *   <li>Without arguments: opens a Swing window (tree by {@link Category}, Run button, log pane); the examples'
 *       stdout/stderr is mirrored into the log pane. Headless: prints usage, exit code 2. Examples started from
 *       the window get no arguments, so they obtain the replay via {@link ReplayChooser}.</li>
 * </ul>
 * <p>Run: {@code ./gradlew :shared:launcher} or {@code ./gradlew :shared:launcher --args "<name> <replay.dem>"}.
 * The {@code shared} project has the examples, repro and dev projects on its runtime classpath.
 */
public final class ExampleLauncher {

    private ExampleLauncher() {
    }

    /** Launcher entry point; see the class documentation for argument handling. */
    public static void main(String[] args) {
        Map<String, ExampleEntry> registry = buildRegistry();
        if (registry.isEmpty()) {
            System.err.println("No @Example-annotated classes found on the classpath.");
            System.exit(1);
            return;
        }

        if (args.length > 0) {
            ExampleEntry entry = registry.get(args[0]);
            if (entry == null) {
                System.err.println("Unknown example: '" + args[0] + "'");
                printAvailable(registry);
                System.exit(1);
                return;
            }
            String[] forwarded = Arrays.copyOfRange(args, 1, args.length);
            runOnWorker(entry, forwarded);
            return;
        }

        if (GraphicsEnvironment.isHeadless()) {
            System.err.println("Headless environment: no-arg launch requires a display.");
            System.err.println("Pass an example name as args[0] instead.");
            printAvailable(registry);
            System.exit(2);
            return;
        }

        installLookAndFeel();

        JTextArea logArea = new JTextArea();
        installStdoutRedirect(logArea);

        SwingUtilities.invokeLater(() -> new LauncherWindow(registry, logArea).setVisible(true));
    }

    private static void installLookAndFeel() {
        if (FlatLightLaf.setup()) return;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
    }

    private static void installStdoutRedirect(JTextArea logArea) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        OutputStream sink = new TextAreaSink(logArea);
        System.setOut(new PrintStream(new TeeStream(originalOut, sink), true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(new TeeStream(originalErr, sink), true, StandardCharsets.UTF_8));
    }

    /** Collects all {@link Example} classes into a name-sorted map; later entries with a duplicate name replace earlier ones. */
    static Map<String, ExampleEntry> buildRegistry() {
        Map<String, ExampleEntry> m = new TreeMap<>();
        for (Class<?> cls : ClassIndex.getAnnotated(Example.class)) {
            Example ex = cls.getAnnotation(Example.class);
            if (ex == null) continue;
            m.put(ex.name(), new ExampleEntry(ex.name(), ex.description(), ex.category(), cls));
        }
        return m;
    }

    /**
     * Invokes {@code entry}'s {@code main(String[])} reflectively on a new non-daemon thread named
     * {@code example-<name>}. Exceptions are printed to stderr, not rethrown.
     *
     * @return the started thread
     */
    static Thread runOnWorker(ExampleEntry entry, String[] args) {
        Thread t = new Thread(() -> {
            try {
                Method main = entry.mainClass().getMethod("main", String[].class);
                main.invoke(null, (Object) args);
            } catch (InvocationTargetException ite) {
                Throwable cause = ite.getCause() != null ? ite.getCause() : ite;
                cause.printStackTrace();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }, "example-" + entry.name());
        t.setDaemon(false);
        t.start();
        return t;
    }

    private static void printAvailable(Map<String, ExampleEntry> registry) {
        System.err.println("Available examples:");
        registry.values().stream()
                .sorted((a, b) -> {
                    int c = a.category().compareTo(b.category());
                    return c != 0 ? c : a.name().compareTo(b.name());
                })
                .forEach(e -> System.err.format("  [%s] %-24s %s%n", e.category(), e.name(), e.description()));
    }

    /** Registry entry: the {@link Example} attributes plus the annotated class. */
    record ExampleEntry(String name, String description, Category category, Class<?> mainClass) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static final class TeeStream extends OutputStream {
        private final OutputStream a;
        private final OutputStream b;

        TeeStream(OutputStream a, OutputStream b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public void write(int i) throws IOException {
            a.write(i);
            b.write(i);
        }

        @Override
        public void write(byte[] buf, int off, int len) throws IOException {
            a.write(buf, off, len);
            b.write(buf, off, len);
        }

        @Override
        public void flush() throws IOException {
            a.flush();
            b.flush();
        }
    }

    /**
     * Buffers bytes and flushes them to a JTextArea on the EDT. Coalesces writes
     * so high-volume examples (dumpmana, position) don't drown the EDT in events.
     */
    private static final class TextAreaSink extends OutputStream {
        private final JTextArea target;
        private final StringBuilder buf = new StringBuilder();
        private final Deque<String> pending = new ArrayDeque<>();
        private boolean flushScheduled;

        TextAreaSink(JTextArea target) {
            this.target = target;
        }

        @Override
        public synchronized void write(int i) {
            buf.append((char) (i & 0xFF));
            if ((char) i == '\n') scheduleFlush();
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            buf.append(new String(b, off, len, StandardCharsets.UTF_8));
            if (buf.length() > 4096 || indexOfNewline(b, off, len) >= 0) scheduleFlush();
        }

        private static int indexOfNewline(byte[] b, int off, int len) {
            for (int i = 0; i < len; i++) if (b[off + i] == '\n') return i;
            return -1;
        }

        private void scheduleFlush() {
            if (buf.length() == 0) return;
            pending.add(buf.toString());
            buf.setLength(0);
            if (!flushScheduled) {
                flushScheduled = true;
                SwingUtilities.invokeLater(this::drain);
            }
        }

        private void drain() {
            String chunk;
            synchronized (this) {
                StringBuilder all = new StringBuilder();
                String p;
                while ((p = pending.poll()) != null) all.append(p);
                flushScheduled = false;
                chunk = all.toString();
            }
            if (chunk.isEmpty()) return;
            target.append(chunk);
            target.setCaretPosition(target.getDocument().getLength());
        }
    }
}
