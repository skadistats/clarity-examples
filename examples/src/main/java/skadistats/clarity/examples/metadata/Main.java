package skadistats.clarity.examples.metadata;

import skadistats.clarity.Clarity;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.wire.dota.s2.proto.DOTAS2MatchMetadata;

import java.io.IOException;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

@Example(name = "metadata", description = "Extract and display Dota 2 match metadata", category = Category.DOCS)
public class Main {

    public static void main(String[] args) throws IOException {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        DOTAS2MatchMetadata.CDOTAMatchMetadataFile metadata = Clarity.metadataForFile(replay);
        System.out.println(metadata);
    }

}
