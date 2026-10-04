package skadistats.clarity.examples.livesource;

import skadistats.clarity.protobuf.GeneratedMessage;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.LiveSource;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.concurrent.TimeUnit;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Streams a replay through {@link LiveSource} while it is still being written, and prints the simple class name of
 * every message.
 *
 * <p>Demonstrates {@link LiveSource}, which reads a replay file that is growing (for example one written by a running
 * game server) and blocks for more data instead of failing at the current end of the file, and a catch-all
 * {@link OnMessage} listener. To simulate a running game, a background thread copies the source replay to the
 * destination file in 8 KiB chunks with a 25 ms pause between chunks. The run ends when {@code LiveSource} gets no
 * new data for 5 seconds (a timeout exception) or reads a {@code CDemoStop} message followed by the end of the data.
 *
 * <p>Works with any replay the parser supports (Source 1 or Source 2); it only looks at message classes.
 *
 * <p>Run (does not use the replay chooser; both arguments are required):
 * <pre>
 * ./gradlew :examples:livesourceRun --args "path/to/replay.dem path/to/copy.dem"
 * </pre>
 * {@code args[0]} is the replay to read, {@code args[1]} is the file to write and stream from; it is overwritten.
 */
@Example(name = "livesource", description = "Demonstrate real-time replay streaming from a file", category = Category.DOCS)
public class Main {

    public void run(String[] args) throws Exception {
        String srcFile = args[0];
        String dstFile = args[1];
        createWriterThread(srcFile, dstFile);

        // The timeout is how long a read waits for the file to grow before it fails; the file may not exist yet.
        try (LiveSource source = new LiveSource(dstFile, 5, TimeUnit.SECONDS)) {
            new SimpleRunner(source).runWith(new Object() {
                // No value() on @OnMessage: the default GeneratedMessage.class matches every message.
                @OnMessage
                public void onMessage(GeneratedMessage msg) {
                    System.out.println(msg.getClass().getSimpleName());
                }
            });
        }
    }

    /** Simulates a game server: copies {@code srcFile} to {@code dstFile} slowly, in the background. */
    private void createWriterThread(final String srcFile, final String dstFile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try (FileInputStream src = new FileInputStream(srcFile);
                     FileOutputStream dst = new FileOutputStream(dstFile)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = src.read(buf)) != -1) {
                        dst.write(buf, 0, n);
                        dst.flush();
                        Thread.sleep(25);
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }


    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
