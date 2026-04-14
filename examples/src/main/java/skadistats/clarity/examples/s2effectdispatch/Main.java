package skadistats.clarity.examples.s2effectdispatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.shared.s2.proto.S2TempEntities.CMsgEffectData;

import java.util.Map;
import java.util.TreeMap;
import skadistats.clarity.examples.shared.ReplayChooser;

/**
 * Demonstrates the {@link EffectDispatches} provider on a Source 2 replay.
 *
 * Counts effect dispatches by resolved handler name and prints a histogram
 * at the end of the run.
 */
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getName());

    private final Map<String, Long> byKind = new TreeMap<>();

    @OnEffectDispatch
    public void onEffectDispatch(String kind, CMsgEffectData data) {
        byKind.merge(kind != null ? kind : "<unresolved>", 1L, Long::sum);
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this, new EffectDispatches());
        }
        long tMatch = System.currentTimeMillis() - tStart;

        log.info("=== effect dispatch histogram ===");
        long total = 0;
        for (var e : byKind.entrySet()) {
            log.info("  {} : {}", e.getKey(), e.getValue());
            total += e.getValue();
        }
        log.info("total: {} dispatches across {} handlers", total, byKind.size());
        log.info("total time taken: {}s", tMatch / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
