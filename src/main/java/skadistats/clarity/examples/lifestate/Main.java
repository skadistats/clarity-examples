package skadistats.clarity.examples.lifestate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @Insert
    private Context ctx;

    @OnEntitySpawned
    public void onSpawned(Entity e) {
        System.out.printf("%06d: %s at index %d has spawned\n", ctx.getTick(), e.getDtClass().getDtName(), e.getIndex());
    }

    @OnEntityDied
    public void onDied(Entity e) {
        System.out.printf("%06d: %s at index %d has died\n", ctx.getTick(), e.getDtClass().getDtName(), e.getIndex());
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        SimpleRunner r = null;
        try {
            r = new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        } finally {
            long tMatch = System.currentTimeMillis() - tStart;
            log.info("total time taken: {}s", (tMatch) / 1000.0);
            if (r != null) {
                r.getSource().close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
