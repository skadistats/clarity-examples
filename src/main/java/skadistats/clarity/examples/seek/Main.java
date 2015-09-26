package skadistats.clarity.examples.seek;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;

import java.util.Random;

@UsesEntities
public class Main {

    private final int N_SEEKS = 1000;

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void runSeek(String[] args) throws Exception {
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        int lastTick = runner.getLastTick();
        Random r = new Random();
        int i = N_SEEKS;
        try {
            long tStart = System.nanoTime();
            while (i-- > 0) {
                int nextTick = r.nextInt(lastTick);
                log.warn("seeking to {}", nextTick);
                runner.seek(nextTick);
            }
            long tTick = System.nanoTime() - tStart;
            double tMs = tTick / 1000000.0d;
            log.warn("{} seek operations took {}ms, {}ms/seek", N_SEEKS, tMs, tMs / N_SEEKS);
        } finally {
            runner.halt();
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
