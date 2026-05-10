package skadistats.clarity.examples.dev.test;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.cs.csgo.proto.CsgoNetMessages;
import skadistats.clarity.wire.dota.s1.proto.DOTAS1NetMessages;
import skadistats.clarity.wire.shared.s2.proto.S2NetMessages;

@UsesEntities
@Example(name = "test", description = "Test message dispatch and seeking (internal)", category = Category.DEV)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(GeneratedMessage.class)
    public void onMessage(Context ctx, GeneratedMessage message) {
        if (message instanceof DOTAS1NetMessages.CSVCMsg_VoiceData || message instanceof CsgoNetMessages.CSVCMsg_VoiceData || message instanceof S2NetMessages.CSVCMsg_VoiceData) {
            return;
        }
        log.info("{}: {}", ctx.getTick(), message.getClass().getSimpleName());
    }

    public void runSeek(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            ControllableRunner runner = new ControllableRunner(source).runWith(this);
            try {
                runner.seek(30000);
                System.out.println("at 30000\n\n");
                runner.seek(0);
                System.out.println("at 0\n\n");
            } finally {
                runner.halt();
                runner.join();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
