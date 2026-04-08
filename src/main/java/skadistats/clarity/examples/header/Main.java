package skadistats.clarity.examples.header;

import skadistats.clarity.Clarity;
import skadistats.clarity.wire.shared.demo.proto.Demo;

public class Main {

    public static void main(String[] args) throws Exception {

        Demo.CDemoFileHeader header = Clarity.headerForFile(args[0]);
        System.out.println(header);

    }

}
