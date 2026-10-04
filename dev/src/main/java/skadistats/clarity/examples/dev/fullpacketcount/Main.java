package skadistats.clarity.examples.dev.fullpacketcount;

import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Counts {@code CDemoFullPacket} messages (S2 replays) and prints tick info for them.
 * <p>Prints the first five and then every 50th occurrence with its tick, followed by a summary with the total
 * count and the first and last tick. Useful to see the full-packet (keyframe) spacing of a replay.
 * <p>Arguments: {@code [replay]}.
 * <p>Run: {@code ./gradlew :dev:fullpacketcountRun --args "path/to/replay.dem"}
 */
@Example(name = "fullpacketcount", description = "Count and log CDemoFullPacket occurrences", category = Category.DEV)
public class Main {

    private int count;
    private int firstTick = -1;
    private int lastTick = -1;

    @OnMessage(Demo.CDemoFullPacket.class)
    public void onFullPacket(Context ctx, Demo.CDemoFullPacket m) {
        count++;
        int t = ctx.getTick();
        if (firstTick < 0) firstTick = t;
        lastTick = t;
        if (count <= 5 || count % 50 == 0) {
            System.out.format("CDemoFullPacket #%d at tick=%d%n", count, t);
        }
    }

    public static void main(String[] args) throws Exception {
        Main p = new Main();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (Source source = new MappedFileSource(replay)) {
            SimpleRunner runner = new SimpleRunner(source);
            runner.runWith(p);
        }
        System.out.format("%n=== Summary ===%nCDemoFullPacket count: %d%nfirst=%d last=%d%n",
            p.count, p.firstTick, p.lastTick);
    }
}
