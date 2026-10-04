package skadistats.clarity.examples.lifestate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints when entities spawn and die, as {@code <tick>: <class name> at index <n> has spawned/died}.
 *
 * <p>Demonstrates custom events: {@link OnEntitySpawned} and {@link OnEntityDied} (and
 * {@link OnEntityDying}, unused here) are annotations defined in this package and raised by the
 * processor {@link SpawnsAndDeaths}, which derives them from changes of the {@code m_lifeState}
 * property. This class only declares handlers; Clarity finds the providing processor through its
 * {@code @Provides} annotation and instantiates it, so no explicit registration is needed. Also
 * shows {@code @Insert} of the {@link Context} to read the current tick.
 *
 * <p>Applies to entities that have an {@code m_lifeState} property, as in Dota 2.
 *
 * <p>Run: {@code ./gradlew :examples:lifestateRun --args "path/to/replay.dem"}
 */
@Example(name = "lifestate", description = "Track entity spawn and death events in replay", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    // injects the runner context; getTick() returns the tick currently being processed
    @Insert
    private Context ctx;

    // custom event: raised by SpawnsAndDeaths when m_lifeState becomes 0
    @OnEntitySpawned
    public void onSpawned(Entity e) {
        System.out.printf("%06d: %s at index %d has spawned\n", ctx.getTick(), e.getDtClass().getDtName(), e.getIndex());
    }

    // custom event: raised by SpawnsAndDeaths when m_lifeState becomes 2
    @OnEntityDied
    public void onDied(Entity e) {
        System.out.printf("%06d: %s at index %d has died\n", ctx.getTick(), e.getDtClass().getDtName(), e.getIndex());
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        } finally {
            long tMatch = System.currentTimeMillis() - tStart;
            log.info("total time taken: {}s", (tMatch) / 1000.0);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
