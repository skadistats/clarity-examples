package skadistats.clarity.examples.test;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.s1.proto.S1NetMessages;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(GeneratedMessage.class)
    public void onMessage(Context ctx, GeneratedMessage message) {
        if (message instanceof S1NetMessages.CSVCMsg_VoiceData || message instanceof S2NetMessages.CSVCMsg_VoiceData) {
            return;
        }
        log.info("{}: {}", ctx.getTick(), message.getClass().getSimpleName());
    }

    public void runSeek(String[] args) throws Exception {
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        runner.seek(30000);
        System.out.println("at 30000\n\n");
        runner.seek(0);
        System.out.println("at 0\n\n");
        runner.halt();
    }

    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
