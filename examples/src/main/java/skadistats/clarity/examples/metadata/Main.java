package skadistats.clarity.examples.metadata;

import skadistats.clarity.Clarity;
import skadistats.clarity.wire.dota.s2.proto.DOTAS2MatchMetadata;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        DOTAS2MatchMetadata.CDOTAMatchMetadataFile metadata = Clarity.metadataForFile(args[0]);
        System.out.println(metadata);
    }

}
