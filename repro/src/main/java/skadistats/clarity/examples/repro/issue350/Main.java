package skadistats.clarity.examples.repro.issue350;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityPropertyChanged;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;

/**
 * Reproducer for https://github.com/skadistats/clarity/issues/350
 *
 * The reporter clarified that the hang only manifests when at least one
 * processor with entity listeners is attached. This Main attaches the same
 * two listeners they used (CCSGameRulesProxy.m_pGameRules.m_totalRoundsPlayed
 * and CCSPlayerController.m_iPawnHealth) and drives the demo through a
 * ControllableRunner.
 */
@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class);

    private int rounds = 0;
    private int healthChanges = 0;

    @OnEntityPropertyChanged(classPattern = "CCSGameRulesProxy", propertyPattern = "m_pGameRules.m_totalRoundsPlayed")
    public void onRoundsPlayed(Context ctx, Entity e, FieldPath fp) {
        rounds++;
    }

    @OnEntityPropertyChanged(classPattern = "CCSPlayerController", propertyPattern = "m_iPawnHealth")
    public void onHealth(Context ctx, Entity e, FieldPath fp) {
        healthChanges++;
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        int ticks = 0;
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            ControllableRunner runner = new ControllableRunner(source).runWith(this);
            try {
                while (!runner.isAtEnd()) {
                    runner.tick();
                    ticks++;
                }
            } finally {
                runner.halt();
                runner.join();
            }
        }
        long elapsed = System.currentTimeMillis() - tStart;
        log.info("processed {} ticks in {}s, rounds={} healthChanges={}", ticks, elapsed / 1000.0, rounds, healthChanges);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
