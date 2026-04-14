package skadistats.clarity.examples.lifestate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.event.Insert;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;

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
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        } finally {
            long tMatch = System.currentTimeMillis() - tStart;
            log.info("total time taken: {}s", (tMatch) / 1000.0);
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
