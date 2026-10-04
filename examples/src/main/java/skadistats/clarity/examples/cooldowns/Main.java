package skadistats.clarity.examples.cooldowns;

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
 * Prints ability and item cooldown starts, resets and expirations of a Dota 2 replay, as
 * {@code <tick>: <ability class> on <owner class> cooldown started/reset/ended}.
 *
 * <p>Demonstrates consuming custom events: {@link OnAbilityCooldownStart},
 * {@link OnAbilityCooldownReset} and {@link OnAbilityCooldownEnd} are annotations defined in this
 * package and raised by the processor {@link Cooldowns}. This class only declares handlers;
 * Clarity finds the providing processor through its {@code @Provides} annotation and instantiates it. The
 * handlers are plain methods whose parameters follow the {@code Listener} interface of each
 * annotation. Also shows {@code @Insert} of the {@link Context} to read the current tick.
 *
 * <p>Dota 2 Source 2 only: the end check reads the game time from the {@code CDOTAGamerulesProxy}
 * entity.
 *
 * <p>Run: {@code ./gradlew :examples:cooldownsRun --args "path/to/replay.dem"}
 */
@Example(name = "cooldowns", description = "Track ability cooldown events (start, reset, end)", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class);

    // injects the runner context; getTick() returns the tick currently being processed
    @Insert
    private Context ctx;

    @OnAbilityCooldownStart
    public void onCooldownStart(Entity ability, Entity owner, Float endTime) {
        System.out.printf("%06d: %s on %s cooldown started, ends at %.2f%n",
                ctx.getTick(),
                ability.getDtClass().getDtName(),
                owner != null ? owner.getDtClass().getDtName() : "(unknown)",
                endTime);
    }

    @OnAbilityCooldownReset
    public void onCooldownReset(Entity ability, Entity owner) {
        System.out.printf("%06d: %s on %s cooldown reset%n",
                ctx.getTick(),
                ability.getDtClass().getDtName(),
                owner != null ? owner.getDtClass().getDtName() : "(unknown)");
    }

    @OnAbilityCooldownEnd
    public void onCooldownEnd(Entity ability, Entity owner) {
        System.out.printf("%06d: %s on %s cooldown ended%n",
                ctx.getTick(),
                ability.getDtClass().getDtName(),
                owner != null ? owner.getDtClass().getDtName() : "(unknown)");
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
