package skadistats.clarity.examples.info;

import skadistats.clarity.Clarity;
import skadistats.clarity.wire.shared.demo.proto.Demo;

public class Main {

    public static void main(String[] args) throws Exception {

        Demo.CDemoFileInfo info = Clarity.infoForFile(args[0]);
        System.out.println(info);

    }

}
