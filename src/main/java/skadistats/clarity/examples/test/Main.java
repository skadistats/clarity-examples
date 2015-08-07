package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.processor.gameevents.OnGameEvent;
import skadistats.clarity.processor.gameevents.OnGameEventDescriptor;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.sendtables.UsesDTClasses;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.s2.proto.S2NetMessages;

import java.io.IOException;

@UsesStringTable("*")
@UsesDTClasses
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    @OnMessage(S2NetMessages.CSVCMsg_CreateStringTable.class)
    public void onCreateStringTable(Context ctx, S2NetMessages.CSVCMsg_CreateStringTable message) throws IOException {
        //System.out.println(message.getName());
    }

    @OnGameEventDescriptor()
    public void onGameEventDescriptor(Context ctx, GameEventDescriptor descriptor) {
        System.out.println(descriptor);
    }

    @OnGameEvent
    public void onGameEvent(Context ctx, GameEvent event) {
        System.out.println(event);
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        runner.tick();
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
