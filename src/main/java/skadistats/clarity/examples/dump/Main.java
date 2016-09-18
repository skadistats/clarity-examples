package skadistats.clarity.examples.dump;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.s1.proto.S1NetMessages;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(GeneratedMessage.class)
    public void onMessage(GeneratedMessage message) {
        if (message instanceof S1NetMessages.CSVCMsg_VoiceData || message instanceof S2NetMessages.CSVCMsg_VoiceData) {
            return;
        }
        log.info(message.getClass().getName());
        log.info(message.toString());
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
