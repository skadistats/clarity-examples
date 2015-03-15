package skadistats.clarity.examples.tick;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.Runner;

import java.io.FileInputStream;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private int tick;
    private int count;

    @OnTickStart
    public void onTickStart(Context ctx) {
        tick = ctx.getTick();
        count = 0;
    }

    @OnTickEnd
    public void onTickEnd(Context ctx) {
        log.info("tick {} had {} messages", tick, count);
    }

    @OnMessage
    public void onMessage(Context ctx, GeneratedMessage message) {
        count++;
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
