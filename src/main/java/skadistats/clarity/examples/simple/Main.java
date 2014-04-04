package skadistats.clarity.examples.simple;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.Clarity;
import skadistats.clarity.match.Match;
import skadistats.clarity.parser.DemoInputStreamIterator;
import skadistats.clarity.parser.Peek;
import skadistats.clarity.parser.Profile;

import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_ChatEvent;
import com.dota2.proto.Netmessages.CNETMsg_Tick;

public class Main {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("simple");

        Match match = new Match();
        DemoInputStreamIterator iter = Clarity.iteratorForFile(args[0], Profile.CHAT_MESSAGES);

        while (iter.hasNext()) {
            Peek p = iter.next();
            if (p.getMessage() instanceof CNETMsg_Tick) {
                // applying this peek will start a new tick, and clear the game events,
                // so I will output all accumulated game events
                for (CDOTAUserMsg_ChatEvent c : match.getChatEvents().getAll()) {
                    System.out.println(c);
                }
            }
            p.apply(match);
        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);

    }

}
