package skadistats.clarity.examples.metadata;

import skadistats.clarity.Clarity;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.wire.dota.s2.proto.DOTAS2MatchMetadata;

import java.io.IOException;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Reads a Dota 2 match metadata file with {@link Clarity#metadataForFile(String)} and prints the decoded
 * {@code CDOTAMatchMetadataFile} protobuf message.
 *
 * <p>Demonstrates the static helper in {@link Clarity} that parses the file directly, without a
 * {@code Runner}, {@code Source} or any processors. The file is read as one {@code CDOTAMatchMetadataFile} message
 * ({@code DOTAS2MatchMetadata}, Dota 2 Source 2); passing an ordinary {@code .dem} replay is not what this method is for.
 *
 * <p>Run:
 * <pre>
 * ./gradlew :examples:metadataRun --args "path/to/metadata-file"
 * </pre>
 */
@Example(name = "metadata", description = "Extract and display Dota 2 match metadata", category = Category.DOCS)
public class Main {

    public static void main(String[] args) throws IOException {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        // Throws IOException if the file does not exist or does not hold a valid metadata message.
        DOTAS2MatchMetadata.CDOTAMatchMetadataFile metadata = Clarity.metadataForFile(replay);
        System.out.println(metadata);
    }

}
