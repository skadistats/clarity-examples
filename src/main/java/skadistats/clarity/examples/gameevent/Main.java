package skadistats.clarity.examples.gameevent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.two.processor.gameevents.OnGameEvent;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.runner.Runner;

import java.io.FileInputStream;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnGameEvent
    public void onGameEvent(Context ctx, GameEvent event) {
        log.info("{}", event.toString());
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new Runner().runWith(new FileInputStream(args[0]), this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
