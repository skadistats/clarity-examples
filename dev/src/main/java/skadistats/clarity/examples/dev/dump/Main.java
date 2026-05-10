package skadistats.clarity.examples.dev.dump;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.cs.csgo.proto.CsgoNetMessages;
import skadistats.clarity.wire.dota.s1.proto.DOTAS1NetMessages;
import skadistats.clarity.wire.shared.s2.proto.S2NetMessages;

@Example(name = "dump", description = "Dump all replay messages with optional content", category = Category.DEV)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private boolean dumpAudio;
    private boolean dumpMessage;

    @OnMessage(GeneratedMessage.class)
    public void onMessage(GeneratedMessage message) {
        if (!dumpAudio && isAudio(message)) {
            return;
        }
        log.info(message.getClass().getName());
        if (dumpMessage) {
            log.info(message.toString());
        }
    }

    private boolean isAudio(GeneratedMessage message) {
        return
                message instanceof DOTAS1NetMessages.CSVCMsg_VoiceData
                || message instanceof CsgoNetMessages.CSVCMsg_VoiceData
                || message instanceof S2NetMessages.CSVCMsg_VoiceData;
    }

    public void run(String replayFile, boolean dumpAudio, boolean dumpMessage) throws Exception {
        this.dumpAudio = dumpAudio;
        this.dumpMessage = dumpMessage;
        long tStart = System.currentTimeMillis();
        try (MappedFileSource source = new MappedFileSource(replayFile)) {
            new SimpleRunner(source).runWith(this);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        new Main().run(replay, false, true);
    }

}
