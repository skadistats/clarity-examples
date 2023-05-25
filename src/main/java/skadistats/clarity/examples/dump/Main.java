package skadistats.clarity.examples.dump;

import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.Clarity;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());
    private static boolean OUTPUT = false;

    @OnMessage(GeneratedMessage.class)
    public void onMessage(GeneratedMessage message) {
        if (OUTPUT) {
            log.info(message.getClass().getName());
            log.info(message.toString());
        }
        OUTPUT = false;
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        String[][] data = {
                {"DOTA_S1", "/home/spheenik/projects/replays/dota/s1/normal/271145478.dem"},
                {"DOTA_S2", "/home/spheenik/projects/replays/dota/s2/297/7048355297.dem"},
                {"CSGO_S1", "/home/spheenik/projects/replays/csgo/s1/issue-271/astralis-vs-godsent-m1-nuke.dem"},
                {"CSGO_S2", "/home/spheenik/projects/replays/csgo/s2/prelaunch/bayes_demo.dem"}
        };
        for (String[] datum : data) {
            OUTPUT = true;
            System.out.println(datum[0]);
            new Main().run(new String[] {datum[1]});
        }
        for (String[] datum : data) {
            System.out.println(datum[0]);
            try {
                System.out.println(Clarity.infoForFile(datum[1]));
            } catch (Exception e) {
                log.error("Info not found", e);
            }
        }
    }

}
