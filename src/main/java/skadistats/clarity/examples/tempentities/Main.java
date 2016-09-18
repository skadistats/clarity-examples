package skadistats.clarity.examples.tempentities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.tempentities.OnTempEntity;
import skadistats.clarity.source.MappedFileSource;


public class Main {

    @OnTempEntity
    public void onTempEntity(Entity e) {
        System.out.println(e);
    }

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
