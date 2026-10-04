package skadistats.clarity.examples.modifiers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.modifiers.OnModifierTableEntry;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.dota.common.proto.DOTAModifiers;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;


/**
 * Prints every modifier (buff/debuff) table entry of a Dota 2 replay.
 *
 * <p>Demonstrates {@link OnModifierTableEntry}, an event that Clarity raises for each entry with data in the
 * {@code "ActiveModifiers"} string table, already decoded to a {@code CDOTAModifierBuffTableEntry} protobuf message.
 * Each entry is printed with its protobuf {@code toString()}.
 *
 * <p>Dota 2 only; the processor is documented as Dota 2 and uses the Dota modifier message type. Logs the total run time.
 *
 * <p>Run:
 * <pre>
 * ./gradlew :examples:modifiersRun --args "path/to/replay.dem"
 * </pre>
 */
@Example(name = "modifiers", description = "Print modifier/buff table entries from replay", category = Category.DOCS)
public class Main {

    // Handler signature: ([Context ctx,] CDOTAModifierBuffTableEntry entry). If an entry fails to parse, Clarity logs it
    // and passes the incomplete message.
    @OnModifierTableEntry()
    public void onModifierEntry(DOTAModifiers.CDOTAModifierBuffTableEntry e) {
        System.out.println(e);
    }

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

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

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
