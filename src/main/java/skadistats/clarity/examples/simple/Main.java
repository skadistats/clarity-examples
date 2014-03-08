package skadistats.clarity.examples.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.Clarity;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.DemoInputStreamIterator;
import skadistats.clarity.parser.Profile;

public class Main {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("simple");

        Match match = new Match();
        DemoInputStreamIterator iter = Clarity.iteratorForFile(args[0], Profile.ENTITIES);
        
        while(iter.hasNext()) {
            iter.next().apply(match);
        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        
    }

}
