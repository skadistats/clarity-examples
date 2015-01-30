package skadistats.clarity.examples.two.simple;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.processor.reader.event.OnFileInfoOffset;
import skadistats.clarity.two.processor.reader.event.OnMessage;
import skadistats.clarity.two.processor.reader.event.OnMessageContainer;
import skadistats.clarity.two.runner.Context;
import skadistats.clarity.two.runner.Runner;

import java.io.FileInputStream;

public class Main {

    public static class Test {
        @OnMessage
        public void message(Context ctx, GeneratedMessage message) {
            //System.out.println(message.getClass().getName());
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
