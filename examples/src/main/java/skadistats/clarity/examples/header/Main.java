package skadistats.clarity.examples.header;

import skadistats.clarity.Clarity;
import skadistats.clarity.examples.shared.ReplayChooser;
import skadistats.clarity.wire.shared.demo.proto.Demo;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

@Example(name = "header", description = "Extract and display the demo file header", category = Category.DOCS)
public class Main {

    public static void main(String[] args) throws Exception {

        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        Demo.CDemoFileHeader header = Clarity.headerForFile(replay);
        System.out.println(header);

    }

}
