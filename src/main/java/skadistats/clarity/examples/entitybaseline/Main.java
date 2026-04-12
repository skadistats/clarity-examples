package skadistats.clarity.examples.entitybaseline;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

/**
 * Baseline benchmark for raw entity parsing throughput.
 * Exercises the hot path: readFieldOp() → Huffman decode → readUBitVarFieldPath → readUBitInt.
 * No listeners attached — measures pure parsing cost.
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final int WARMUP = 5;
    private static final int RUNS = 20;

    @UsesEntities
    public static class Baseline {
    }

    private static long parse(String path) throws Exception {
        long t0 = System.nanoTime();
        try (MappedFileSource src = new MappedFileSource(path)) {
            new SimpleRunner(src).runWith(new Baseline());
        }
        return System.nanoTime() - t0;
    }

    private static void bench(String label, String path) throws Exception {
        log.info("=== {} : {} ===", label, path);
        for (int i = 0; i < WARMUP; i++) {
            double ms = parse(path) / 1_000_000.0;
            log.info(String.format("  warmup %d : %.1f ms", i + 1, ms));
        }
        long sum = 0;
        long min = Long.MAX_VALUE;
        long max = Long.MIN_VALUE;
        for (int i = 0; i < RUNS; i++) {
            long t = parse(path);
            sum += t;
            if (t < min) min = t;
            if (t > max) max = t;
            log.info(String.format("  run %d : %.1f ms", i + 1, t / 1_000_000.0));
        }
        double avgMs = (sum / (double) RUNS) / 1_000_000.0;
        double minMs = min / 1_000_000.0;
        double maxMs = max / 1_000_000.0;
        log.info(String.format("  RESULT  avg=%.1f ms  min=%.1f ms  max=%.1f ms", avgMs, minMs, maxMs));
    }

    public void run(String[] args) throws Exception {
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
        log.info("Entity baseline benchmark (warmup={}, runs={})", WARMUP, RUNS);
        for (int i = 0; i < paths.length; i++) {
            bench(labels[i], paths[i]);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
