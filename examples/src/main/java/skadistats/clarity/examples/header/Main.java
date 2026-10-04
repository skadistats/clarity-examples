package skadistats.clarity.examples.header;

import skadistats.clarity.Clarity;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints the {@code CDemoFileHeader} of a replay.
 *
 * <p>Demonstrates {@link Clarity#headerForFile(String)}, which reads the header without parsing the
 * rest of the replay, so no runner or processor is involved.
 *
 * <p>Supported for Dota 2 (Source 1 and Source 2), CS2 and Deadlock demos; Source 1 CS:GO demos
 * are not supported by this helper.
 *
 * <p>Run: {@code ./gradlew :examples:headerRun --args "path/to/replay.dem"}
 */
@Example(name = "header", description = "Extract and display the demo file header", category = Category.DOCS)
public class Main {

    public static void main(String[] args) throws Exception {

        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        Demo.CDemoFileHeader header = Clarity.headerForFile(replay);
        System.out.println(header);

    }

}
