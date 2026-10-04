package skadistats.clarity.examples.gameevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.processor.gameevents.OnGameEvent;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Logs every game event of a replay.
 *
 * <p>Demonstrates {@link OnGameEvent}. Without a {@code value()}, the handler is called for every
 * game event; the {@link GameEvent} is decoded against the descriptors the replay announces
 * ({@code CSVCMsg_GameEventList}) and printed with {@code toString()}. Giving an event name,
 * e.g. {@code @OnGameEvent("player_death")}, restricts the handler to that event.
 *
 * <p>The example is not tied to one game; the set of events and their keys depends on the game.
 *
 * <p>Run: {@code ./gradlew :examples:gameeventRun --args "path/to/replay.dem"}
 */
@Example(name = "gameevent", description = "Print all game events from a replay", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class);

    // no value(): matches all events
    @OnGameEvent
    public void onGameEvent(GameEvent event) {
        log.info("{}", event.toString());
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

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
