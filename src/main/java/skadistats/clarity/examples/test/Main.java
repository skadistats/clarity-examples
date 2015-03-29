package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.wire.proto.Netmessages;

import java.io.FileInputStream;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(Netmessages.CSVCMsg_FullFrameSplit.class)
    public void onMessage(Context ctx, Netmessages.CSVCMsg_FullFrameSplit message) {
        System.out.format("FULL FRAME %s %s/%s with %s bytes\n", message.getTick(), message.getSection(), message.getTotal(), message.getData().size());
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        Context ctx = new SimpleRunner(new FileInputStream(args[0])).runWith(this).getContext();
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        if (System.console() != null) System.console().readLine();
        new Main().run(args);
    }

}
