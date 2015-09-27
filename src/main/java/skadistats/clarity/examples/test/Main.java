package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

//    @OnMessage(GeneratedMessage.class)
//    public void onMessage(Context ctx, GeneratedMessage message) {
//        if (message instanceof S1NetMessages.CSVCMsg_VoiceData || message instanceof S2NetMessages.CSVCMsg_VoiceData) {
//            return;
//        }
//        log.info("{}: {}", ctx.getTick(), message.getClass().getSimpleName());
//    }
//
    public void runSeek(String[] args) throws Exception {
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        runner.seek(3000);
        System.out.println("at 3000");
        runner.seek(0);
        System.out.println("at 0");
        runner.halt();
    }

    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
