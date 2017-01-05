package skadistats.clarity.examples.metadata;

import skadistats.clarity.Clarity;
import skadistats.clarity.wire.s2.proto.S2DotaMatchMetadata;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        S2DotaMatchMetadata.CDOTAMatchMetadataFile metadata = Clarity.metadataForFile(args[0]);
        System.out.println(metadata);
    }

}
