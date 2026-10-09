package skadistats.clarity.examples.dev.umscan;

import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.processor.reader.OnMessageContainer;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.protobuf.ByteString;
import skadistats.clarity.source.MappedFileSource;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;

/**
 * Scans the embedded messages of an S2 replay for the given kinds and prints tick, size and payload of each
 * occurrence, followed by a per-kind summary (count, min/max size).
 * <p>Arguments: {@code [replay] kind...}.
 * <p>Run: {@code ./gradlew :dev:umscanRun --args "path/to/replay.dem 637 638"}
 */
@Example(name = "umscan", description = "Find embedded messages of given kinds and dump their payload", category = Category.DEV)
public class Main {

    private final Set<Integer> kinds = new HashSet<>();
    private final TreeMap<Integer, int[]> stats = new TreeMap<>();
    private int tick;

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
        tick = ctx.getTick();
    }

    @OnMessageContainer
    public void onContainer(Class<?> clazz, ByteString bytes) {
        var bs = BitStream.createBitStream(bytes);
        while (bs.remaining() >= 8) {
            var kind = bs.readUBitVar();
            if (kind == 0) break;
            var size = bs.readVarUInt();
            if (!kinds.contains(kind)) {
                bs.skip(size * 8);
                continue;
            }
            var data = new byte[size];
            bs.readBitsIntoByteArray(data, size * 8);
            var s = stats.computeIfAbsent(kind, k -> new int[]{0, Integer.MAX_VALUE, 0});
            s[0]++;
            s[1] = Math.min(s[1], size);
            s[2] = Math.max(s[2], size);
            var hex = new StringBuilder();
            for (byte b : data) hex.append(String.format("%02x", b));
            System.out.format("tick=%d kind=%d size=%d data=%s%n", tick, kind, size, hex);
        }
    }

    public static void main(String[] args) throws Exception {
        var p = new Main();
        for (int i = 1; i < args.length; i++) p.kinds.add(Integer.parseInt(args[i]));
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (var source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(p);
        }
        p.stats.forEach((k, s) -> System.out.format("kind %d: count=%d size=[%d..%d]%n", k, s[0], s[1], s[2]));
        if (p.stats.isEmpty()) System.out.println("no matching kinds found");
    }

}
