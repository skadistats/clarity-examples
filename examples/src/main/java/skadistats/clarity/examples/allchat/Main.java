package skadistats.clarity.examples.allchat;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.s2.proto.S2UserMessages;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints all-chat messages of a Source 2 replay as {@code param1: param2}.
 *
 * <p>Demonstrates the simplest Clarity setup: a plain class with a single
 * {@link OnMessage} handler, run once over the replay by a {@link SimpleRunner}. The handler
 * receives {@link S2UserMessages.CUserMessageSayText2}; {@code param1} carries the sender's
 * name and {@code param2} the message text.
 *
 * <p>Works with Source 2 replays only, since the message class is a Source 2 user message.
 *
 * <p>Run: {@code ./gradlew :examples:allchatRun --args "path/to/replay.dem"}
 */
@Example(name = "allchat", description = "Parse and print all-chat messages from a replay", category = Category.DOCS)
public class Main {
    // value() selects the message class; it is matched against the exact class of each message.
    // Handlers may take a Context as optional first parameter; it is not needed here.
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
            // 4) and hand it over to the runner; runWith blocks until the replay is processed
            runner.runWith(processor);
        }
    }

}
