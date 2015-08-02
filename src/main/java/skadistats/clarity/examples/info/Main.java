package skadistats.clarity.examples.info;

import skadistats.clarity.Clarity;
import skadistats.clarity.wire.common.proto.Demo.CDemoFileInfo;

public class Main {
    
    public static void main(String[] args) throws Exception {

        CDemoFileInfo info = Clarity.infoForFile(args[0]);
        System.out.println(info);
        
    }

}
