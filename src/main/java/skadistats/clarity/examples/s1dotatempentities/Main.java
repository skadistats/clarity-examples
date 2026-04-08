package skadistats.clarity.examples.s1dotatempentities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.tempentities.OnTempEntity;
import skadistats.clarity.source.MappedFileSource;

import java.util.Map;
import java.util.TreeMap;

/**
 * Demonstrates the {@code OnTempEntity} event on Source 1 Dota 2 replays.
 *
 * <p>In Dota 2 Source 1, temp entities are sent through
 * {@code CSVCMsg_TempEntities} as bit-packed property updates against a
 * regular DT class (e.g. {@code DT_DOTA_TempEntity_*}). Clarity's existing
 * {@code TempEntities} processor turns each one into an {@link Entity}
 * object so consumers can access fields the same way they would on a
 * persistent networked entity.</p>
 *
 * <p>This example listens to the event and builds a histogram of how many
 * temp entities of each DT class were observed across the whole replay.</p>
 *
 * <p>Note: this only works on Source 1 Dota 2 replays. CSGO Source 1 uses
 * a different (currently unsupported) wire format for temp entities, and
 * Source 2 ships them through entirely different mechanisms — see the
 * {@code s2effectdispatch} and {@code s2dotatempentities} examples.</p>
 */
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getName());

    private final Map<String, Long> byDtClass = new TreeMap<>();

    @OnTempEntity
    public void onTempEntity(Entity e) {
        byDtClass.merge(e.getDtClass().getDtName(), 1L, Long::sum);
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;

        log.info("=== s1 dota temp-entity histogram ===");
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
