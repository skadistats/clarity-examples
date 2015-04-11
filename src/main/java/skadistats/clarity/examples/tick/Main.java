package skadistats.clarity.examples.tick;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private int tick;
    private int count;

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
        tick = ctx.getTick();
        count = 0;
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        log.info("tick {}, synthetic {}, had {} messages", tick, synthetic, count);
        if (synthetic && count > 0) {
            throw new RuntimeException("oops 1");
        }
        if (!synthetic && count == 0) {
            throw new RuntimeException("oops 2");
        }
    }

    @OnMessage
    public void onMessage(Context ctx, GeneratedMessage message) {
        count++;
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public void runControlled(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        while(!runner.isAtEnd()) {
            runner.tick();
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }


    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
