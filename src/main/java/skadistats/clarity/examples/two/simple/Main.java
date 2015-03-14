package skadistats.clarity.examples.two.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.processor.stringtables.UseStringTable;
import skadistats.clarity.two.runner.Runner;

import java.io.FileInputStream;

public class Main {

    @UseStringTable("CombatLogNames")
    public static class Test {
    }

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("simple");

        new Runner().runWith(new FileInputStream(args[0]), new Test());

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        
    }

}
