package skadistats.clarity.examples.tick;

import skadistats.clarity.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Checks the semantics of {@link OnTickStart} and {@link OnTickEnd}.
 *
 * <p>Both events carry a {@code synthetic} flag. A tick is synthetic if the replay has no data for that tick
 * number; the runner raises start/end events for such ticks to step through the gap up to the next tick that has
 * data. The example counts the messages seen between start and end of each tick (using {@link OnMessage} with the
 * default {@code GeneratedMessage}, which matches every message), logs one line per tick
 * ({@code tick N, synthetic X, had M messages}) and throws if a synthetic tick had messages or a real tick had
 * none.</p>
 *
 * <p>Works with all supported engines. {@code main} uses {@link #run} with a {@link SimpleRunner}.
 * {@link #runControlled} does the same with a {@link ControllableRunner}, stepping through the replay with
 * {@code tick()}; it is not called from {@code main}.</p>
 *
 * <p>Run: {@code ./gradlew :examples:tickRun --args "path/to/replay.dem"}</p>
 */
@Example(name = "tick", description = "Validate tick event semantics (start/end, synthetic)", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private int tick;
    private int count;

    // synthetic == true: no data for this tick number (gap between two data ticks).
    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
        tick = ctx.getTick(); // the runner's current tick number
        count = 0;
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        log.info("tick {}, synthetic {}, had {} messages", tick, synthetic, count);
        if (synthetic && count > 0) {
            throw new RuntimeException("oops 1");
        }
        if (!synthetic && count == 0) {
            throw new RuntimeException("oops 2");
        }
    }

    // No value(): called for every decoded message.
    @OnMessage
    public void onMessage(Context ctx, GeneratedMessage message) {
        count++;
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public void runControlled(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            // runWith leaves the runner at the end of tick 0; each tick() blocks until the end of the next tick.
            ControllableRunner runner = new ControllableRunner(source).runWith(this);
            try {
                while (!runner.isAtEnd()) {
                    runner.tick();
                }
            } finally {
                runner.halt();
                runner.join();
            }
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }


    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
