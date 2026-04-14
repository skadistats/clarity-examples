package skadistats.clarity.examples.tick;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.reader.OnTickEnd;
import skadistats.clarity.processor.reader.OnTickStart;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

@Example(name = "tick", description = "Validate tick event semantics (start/end, synthetic)", category = Category.DOCS)
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private int tick;
    private int count;

    @OnTickStart
    public void onTickStart(Context ctx, boolean synthetic) {
        tick = ctx.getTick();
        count = 0;
    }

    @OnTickEnd
    public void onTickEnd(Context ctx, boolean synthetic) {
        log.info("tick {}, synthetic {}, had {} messages", tick, synthetic, count);
        if (synthetic && count > 0) {
            throw new RuntimeException("oops 1");
        }
        if (!synthetic && count == 0) {
            throw new RuntimeException("oops 2");
        }
    }

    @OnMessage
    public void onMessage(Context ctx, GeneratedMessage message) {
        count++;
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            new SimpleRunner(source).runWith(this);
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public void runControlled(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        try (MappedFileSource source = new MappedFileSource(replay)) {
            ControllableRunner runner = new ControllableRunner(source).runWith(this);
            try {
                while (!runner.isAtEnd()) {
                    runner.tick();
                }
            } finally {
                runner.halt();
                runner.join();
            }
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }


    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
