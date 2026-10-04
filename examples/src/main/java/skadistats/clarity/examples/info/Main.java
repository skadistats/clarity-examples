package skadistats.clarity.examples.info;

import skadistats.clarity.Clarity;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints the {@code CDemoFileInfo} of a replay.
 *
 * <p>Demonstrates {@link Clarity#infoForFile(String)}, which seeks directly to the summary record
 * stored in the demo file and parses only that, without running a full parse. No runner or
 * processor is involved. A missing or invalid file throws an {@code IOException}.
 *
 * <p>Each engine type provides the offset of the info record, so the call is not tied to one game;
 * what the info message contains depends on the game.
 *
 * <p>Run: {@code ./gradlew :examples:infoRun --args "path/to/replay.dem"}
 */
@Example(name = "info", description = "Extract and display the demo file info metadata", category = Category.DOCS)
public class Main {

    public static void main(String[] args) throws Exception {

        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        Demo.CDemoFileInfo info = Clarity.infoForFile(replay);
        System.out.println(info);

    }

}
