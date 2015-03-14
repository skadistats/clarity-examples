package skadistats.clarity.examples.simple;

import com.dota2.proto.Usermessages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.processor.reader.OnMessage;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.runner.Runner;

import java.io.FileInputStream;

public class Main {

    public static class Test {
        @OnMessage(Usermessages.CUserMsg_SayText2.class)
        public void doIt(Context ctx, Usermessages.CUserMsg_SayText2 m) {
            System.out.println(ctx.getTick());
            System.out.println(m.toString());
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
