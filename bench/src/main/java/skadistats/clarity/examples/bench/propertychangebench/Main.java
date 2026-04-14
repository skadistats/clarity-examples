package skadistats.clarity.examples.bench.propertychangebench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityPropertyChanged;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

/**
 * Benchmark for the cost of {@link OnEntityPropertyChanged} listeners.
 * Compares baseline (no listeners) against several realistic listener
 * configurations on the same replay file.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int WARMUP = 1;
    private static final int RUNS = 3;

    /** Baseline: do not even pull in PropertyChange. */
    @UsesEntities
    public static class Baseline {
    }

    /** Single narrow listener (matches the existing propertychange example). */
    @UsesEntities
    public static class NarrowSingle {
        long hits;
        @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_lifeState")
        public void on(Entity e, FieldPath fp) { hits++; }
    }

    /** One listener that matches everything (worst case for predicate calls). */
    @UsesEntities
    public static class WildcardSingle {
        long hits;
        @OnEntityPropertyChanged
        public void on(Entity e, FieldPath fp) { hits++; }
    }

    /** Many listeners with non-trivial regexes — stresses cache + regex cost. */
    @UsesEntities
    public static class ManyListeners {
        long hits;
        @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_iHealth")
        public void h1(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_lifeState")
        public void h2(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTA_Unit_Hero_.*", propertyPattern = "m_iCurrentLevel")
        public void h3(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTA_BaseNPC.*", propertyPattern = "m_iTeamNum")
        public void h4(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTA_Item_.*", propertyPattern = "m_iCurrentCharges")
        public void h5(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTA_NPC_.*", propertyPattern = "m_vecOrigin")
        public void h6(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTAPlayer.*", propertyPattern = "m_iKills")
        public void h7(Entity e, FieldPath fp) { hits++; }
        @OnEntityPropertyChanged(classPattern = "CDOTAPlayer.*", propertyPattern = "m_iDeaths")
        public void h8(Entity e, FieldPath fp) { hits++; }
    }

    private static long parse(String path, Object processor) throws Exception {
        long t0 = System.nanoTime();
        try (MappedFileSource src = new MappedFileSource(path)) {
            new SimpleRunner(src).runWith(processor);
        }
        return System.nanoTime() - t0;
    }

    private static void bench(String name, String path, java.util.function.Supplier<Object> factory) throws Exception {
        for (int i = 0; i < WARMUP; i++) parse(path, factory.get());
        long sum = 0;
        long min = Long.MAX_VALUE;
        long hits = -1;
        for (int i = 0; i < RUNS; i++) {
            Object p = factory.get();
            long t = parse(path, p);
            sum += t;
            if (t < min) min = t;
            if (p instanceof NarrowSingle ns) hits = ns.hits;
            else if (p instanceof WildcardSingle ws) hits = ws.hits;
            else if (p instanceof ManyListeners ml) hits = ml.hits;
        }
        double avgMs = (sum / (double) RUNS) / 1_000_000.0;
        double minMs = min / 1_000_000.0;
        log.info(String.format("%-16s avg=%8.1f ms  min=%8.1f ms%s",
                name, avgMs, minMs, hits >= 0 ? "  hits=" + hits : ""));
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: propertychangebench <replay>");
            System.exit(1);
        }
        String path = args[0];
        log.info("benchmarking {}  (warmup={}, runs={})", path, WARMUP, RUNS);
        bench("baseline",   path, Baseline::new);
        bench("wildcard-1", path, WildcardSingle::new);
        bench("narrow-1",   path, NarrowSingle::new);
        bench("many-8",     path, ManyListeners::new);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
