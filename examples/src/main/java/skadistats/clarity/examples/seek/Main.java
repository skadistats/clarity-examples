package skadistats.clarity.examples.seek;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.Random;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Benchmarks random seeking with {@link ControllableRunner}.
 *
 * <p>Unlike {@code SimpleRunner}, which runs the whole replay once from start to end, a
 * {@link ControllableRunner} processes the replay in a background thread and can be moved to arbitrary ticks.
 * After {@code runWith} it waits at the end of tick 0. {@link ControllableRunner#seek(int)} blocks until the
 * runner is at the end of the requested tick, so the state of the processors (here: entities) is consistent
 * when it returns. A small forward step is simply processed; a backward or far-forward seek repositions the
 * source to the nearest preceding full packet / string table data and processes from there.</p>
 *
 * <p>The example performs {@code N_SEEKS} (1000) seeks to random ticks between 0 and the last tick and logs the
 * total time and the time per seek, as warnings. No entity data is printed. The example code is not
 * engine-specific; it needs a source that can be repositioned, such as {@link MappedFileSource}. For
 * CSGO, which does not allow full-packet seeking, a far-forward seek is processed forward from the current
 * position instead of from a full packet.</p>
 *
 * <p>Run: {@code ./gradlew :examples:seekRun --args "path/to/replay.dem"}</p>
 */
@UsesEntities
@Example(name = "seek", description = "Benchmark random seeking performance on a replay", category = Category.DOCS)
public class Main {

    private final int N_SEEKS = 1000;

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void runSeek(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            // Starts the runner thread; it stops at the end of tick 0 and waits for seek()/tick() calls.
            ControllableRunner runner = new ControllableRunner(source).runWith(this);
            try {
                int lastTick = runner.getLastTick();
                Random r = new Random();
                int i = N_SEEKS;
                long tStart = System.nanoTime();
                while (i-- > 0) {
                    int nextTick = r.nextInt(lastTick);
                    log.warn("seeking to {}", nextTick);
                    runner.seek(nextTick); // blocks until the end of nextTick is reached
                }
                long tTick = System.nanoTime() - tStart;
                double tMs = tTick / 1000000.0d;
                log.warn("{} seek operations took {}ms, {}ms/seek", N_SEEKS, tMs, tMs / N_SEEKS);
            } finally {
                // The runner thread does not end by itself: halt it and wait for it before the source is closed.
                runner.halt();
                runner.join();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
