package skadistats.clarity.examples.s1tempentities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.tempentities.OnTempEntity;
import skadistats.clarity.source.MappedFileSource;

import java.util.Map;
import java.util.TreeMap;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Demonstrates the {@code OnTempEntity} event on Source 1 replays.
 *
 * <p>In Source 1 (both Dota 2 and CSGO), temp entities are sent through
 * {@code CSVCMsg_TempEntities} as bit-packed property updates against a
 * regular DT class. Clarity's {@code TempEntities} processor turns each
 * one into an {@link Entity} so consumers can read fields the same way
 * they would on a persistent networked entity.</p>
 *
 * <p>This example listens to the event and builds a histogram of how many
 * temp entities of each DT class were observed across the whole replay.
 * The DT class set differs by game:</p>
 *
 * <ul>
 *   <li><b>Dota 2 Source 1</b> — {@code DT_TEDOTAProjectile},
 *       {@code DT_TEDOTAProjectileLoc}, {@code DT_TEDotaBloodImpact},
 *       {@code DT_TEEffectDispatch}, {@code DT_TEUnitAnimation},
 *       {@code DT_TEUnitAnimationEnd}.</li>
 *   <li><b>CSGO Source 1</b> — {@code DT_TEFireBullets} (every shot),
 *       {@code DT_TEEffectDispatch}, {@code DT_TEDecal} /
 *       {@code DT_TEWorldDecal} (impact decals),
 *       {@code DT_TEBreakModel}, {@code DT_TEPhysicsProp},
 *       {@code DT_TEExplosion}, {@code DT_TEDynamicLight}.</li>
 * </ul>
 *
 * <p>Prints the histogram (DT class name, count) and the total run time through the logger at the end.
 * Shows {@link OnTempEntity} with {@link SimpleRunner}. The event is only provided for the Source 1 engines
 * (Dota 2 S1 and CSGO); on Source 2 replays no listener call ever happens.</p>
 *
 * <p>Run: {@code ./gradlew :examples:s1tempentitiesRun --args "path/to/replay.dem"}</p>
 *
 * <p>For Source 2 (modern Dota 2, CS2, Deadlock), see the
 * {@code s2effectdispatch} and {@code s2dotatempentities} examples — S2
 * ships temp entities through entirely different mechanisms.</p>
 */
@Example(name = "s1tempentities", description = "Count temp entities in Source 1 replays", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getName());

    private final Map<String, Long> byDtClass = new TreeMap<>();

    // Temp entities have no index/serial and are not tracked after this call; only their DT class and state are meaningful.
    @OnTempEntity
    public void onTempEntity(Entity e) {
        byDtClass.merge(e.getDtClass().getDtName(), 1L, Long::sum);
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
        long tMatch = System.currentTimeMillis() - tStart;

        log.info("=== s1 temp-entity histogram ===");
        long total = 0;
        for (var e : byDtClass.entrySet()) {
            log.info("  {} : {}", e.getKey(), e.getValue());
            total += e.getValue();
        }
        log.info("total: {} temp entities across {} dt classes", total, byDtClass.size());
        log.info("total time taken: {}s", tMatch / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
