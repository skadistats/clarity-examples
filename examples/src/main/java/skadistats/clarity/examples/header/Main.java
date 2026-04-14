package skadistats.clarity.examples.header;

import skadistats.clarity.Clarity;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.wire.shared.demo.proto.Demo;

public class Main {

    public static void main(String[] args) throws Exception {

        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        Demo.CDemoFileHeader header = Clarity.headerForFile(replay);
        System.out.println(header);

    }

}
