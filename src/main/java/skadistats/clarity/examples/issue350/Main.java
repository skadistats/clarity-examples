package skadistats.clarity.examples.issue350;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;

/**
 * Reproducer for https://github.com/skadistats/clarity/issues/350
 *
 * Drives a CS2 demo (HLTV #103994 m1 anubis, played on an old map version)
 * through a ControllableRunner. Historically, clarity hung on this kind of
 * replay; this Main verifies it now runs to completion.
 */
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class);

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        int ticks = 0;
        while (!runner.isAtEnd()) {
            runner.tick();
            ticks++;
        }
        long elapsed = System.currentTimeMillis() - tStart;
        log.info("processed {} ticks in {}s", ticks, elapsed / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }
}
