package skadistats.clarity.examples.simple;

import com.dota2.proto.DotaUsermessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.runner.Runner;

import java.io.FileInputStream;


public class Main {

    @OnMessage(DotaUsermessages.CDOTAUserMsg_ChatWheel.class)
    public void doIt(Context ctx, DotaUsermessages.CDOTAUserMsg_ChatWheel m) {
        System.out.println(ctx.getTick());
        System.out.println(m.toString());
    }

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

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
