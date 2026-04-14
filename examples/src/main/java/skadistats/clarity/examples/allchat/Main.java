package skadistats.clarity.examples.allchat;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.s2.proto.S2UserMessages;
import skadistats.clarity.examples.shared.ReplayChooser;

public class Main {
    @OnMessage(S2UserMessages.CUserMessageSayText2.class)
    public void onMessage(Context ctx, S2UserMessages.CUserMessageSayText2 message) {
        System.out.format("%s: %s\n", message.getParam1(), message.getParam2());
    }
    public static void main(String[] args) throws Exception {
        // 1) create an input source from the replay (try-with-resources closes it)
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (Source source = new MappedFileSource(replay)) {
            // 2) create a simple runner that will read the replay once
            SimpleRunner runner = new SimpleRunner(source);
            // 3) create an instance of your processor
            Main processor = new Main();
            // 4) and hand it over to the runner
            runner.runWith(processor);
        }
    }

}
