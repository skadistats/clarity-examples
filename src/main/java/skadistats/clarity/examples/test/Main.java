package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;

import java.io.File;
import java.util.Random;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

//    @OnMessage
//    public void onMessage(Context ctx, GeneratedMessage message) {
//        //System.out.println(message.getClass().getSimpleName());
//    }
//
    public void run(String[] args) throws Exception {
        ControllableRunner runner = new ControllableRunner(new File(args[0])).runWith(this);
        while(!runner.isAtEnd()) {
            long tStart = System.nanoTime();
            runner.tick();
            long tTick = System.nanoTime() - tStart;
            log.info("tick {} took {} microseconds", runner.getTick(), tTick / 1000.0f);
        }
        runner.halt();
    }

    public void runSeek(String[] args) throws Exception {
        ControllableRunner runner = new ControllableRunner(new File(args[0])).runWith(this);
        int lastTick = runner.getLastTick();
        Random r = new Random();
        int i = 100;
        while (i > 0) {
            long tStart = System.nanoTime();
            runner.seek(r.nextInt(lastTick));
            long tTick = System.nanoTime() - tStart;
            log.info("seek to {} took {} microseconds", runner.getTick(), tTick / 1000.0f);
            i--;
        }
        runner.halt();
    }


    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
