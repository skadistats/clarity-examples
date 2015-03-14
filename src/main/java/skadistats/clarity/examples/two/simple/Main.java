package skadistats.clarity.examples.two.simple;

import com.dota2.proto.Networkbasetypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.runner.Runner;

import java.io.FileInputStream;

public class Main {

    public static class Test {

        @OnMessage(Networkbasetypes.CNETMsg_Tick.class)
        public void doIt(Context ctx, Networkbasetypes.CNETMsg_Tick m) {
            System.out.println(ctx.getTick() + m.toString());
        }
    }

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("simple");

        new Runner().runWith(new FileInputStream(args[0]), new Test());

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        
    }

}
