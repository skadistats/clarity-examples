package skadistats.clarity.examples.eventdispatchbench;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.OnEntityCreated;
import skadistats.clarity.processor.entities.OnEntityDeleted;
import skadistats.clarity.processor.entities.OnEntityUpdated;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

/**
 * Benchmark for the raw event dispatch path: {@link OnEntityUpdated} fires
 * for every networked entity update and goes through {@code Event.raise()}
 * without the per-DTClass caching that {@code OnEntityPropertyChanged} has.
 *
 * Establishes a baseline for evaluating optimizations to the event system
 * (TreeMap/HashSet flattening, varargs allocation, predicate hoisting,
 * MethodHandle dispatch).
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int WARMUP = 2;
    private static final int RUNS = 5;

    /** Baseline: entity system on, but no event listeners attached. */
    @UsesEntities
    public static class Baseline {
    }

    /** Single @OnEntityUpdated listener — measures per-update dispatch cost. */
    @UsesEntities
    public static class Updated1 {
        long hits;
        @OnEntityUpdated
        public void on(Entity e, FieldPath[] fp, int n) { hits++; }
    }

    /** Eight @OnEntityUpdated listeners — scales with listener count. */
    @UsesEntities
    public static class Updated8 {
        long hits;
        @OnEntityUpdated public void h1(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h2(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h3(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h4(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h5(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h6(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h7(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated public void h8(Entity e, FieldPath[] fp, int n) { hits++; }
    }

    /** Eight @OnEntityUpdated listeners with class filter — exercises predicate hot path. */
    @UsesEntities
    public static class Updated8Filtered {
        long hits;
        @OnEntityUpdated(classPattern = "CDOTA_Unit_Hero_.*")    public void h1(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "CDOTA_BaseNPC.*")      public void h2(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "CDOTA_Item_.*")        public void h3(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "CDOTA_NPC_.*")         public void h4(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "CDOTAPlayer.*")        public void h5(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "CCSPlayer.*")          public void h6(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "CWeapon.*")            public void h7(Entity e, FieldPath[] fp, int n) { hits++; }
        @OnEntityUpdated(classPattern = "C.*GameRules.*")       public void h8(Entity e, FieldPath[] fp, int n) { hits++; }
    }

    /** Lifecycle listeners — much rarer than updates, sanity check. */
    @UsesEntities
    public static class Lifecycle {
        long created, deleted;
        @OnEntityCreated public void onC(Entity e) { created++; }
        @OnEntityDeleted public void onD(Entity e) { deleted++; }
    }

    private static long parse(String path, Object processor) throws Exception {
        long t0 = System.nanoTime();
        try (MappedFileSource src = new MappedFileSource(path)) {
            new SimpleRunner(src).runWith(processor);
        }
        return System.nanoTime() - t0;
    }

    private static long hitsOf(Object p) {
        if (p instanceof Updated1 x)         return x.hits;
        if (p instanceof Updated8 x)         return x.hits;
        if (p instanceof Updated8Filtered x) return x.hits;
        if (p instanceof Lifecycle x)        return x.created + x.deleted;
        return -1;
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
            hits = hitsOf(p);
        }
        double avgMs = (sum / (double) RUNS) / 1_000_000.0;
        double minMs = min / 1_000_000.0;
        log.info(String.format("  %-18s avg=%8.1f ms  min=%8.1f ms%s",
                name, avgMs, minMs, hits >= 0 ? "  hits=" + hits : ""));
    }

    private static void benchAll(String label, String path) throws Exception {
        log.info("=== {} : {} ===", label, path);
        bench("baseline",         path, Baseline::new);
        bench("lifecycle",        path, Lifecycle::new);
        bench("updated-1",        path, Updated1::new);
        bench("updated-8",        path, Updated8::new);
        bench("updated-8-filt",   path, Updated8Filtered::new);
    }

    public void run(String[] args) throws Exception {
        // Default: one replay each from Dota 2, CS2, Deadlock.
        // Override by passing explicit paths on the command line.
        String[] paths;
        String[] labels;
        if (args.length == 0) {
            labels = new String[] { "dota", "cs2", "deadlock" };
            paths = new String[] {
                    "replays/dota/s2/normal/1560289528.dem",
                    "replays/cs2/350/3dmax-vs-falcons-m1-anubis.dem",
                    "replays/deadlock/newer/19206063.dem",
            };
        } else {
            paths = args;
            labels = new String[args.length];
            for (int i = 0; i < args.length; i++) labels[i] = "arg" + i;
        }
        log.info("warmup={}, runs={}", WARMUP, RUNS);
        for (int i = 0; i < paths.length; i++) {
            benchAll(labels[i], paths[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
