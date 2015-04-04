package skadistats.clarity.examples.stringtables;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.StringTableDecoder;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.StringTableEntry;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.proto.Netmessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@UsesStringTable("instancebaseline")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private List<String> names = new ArrayList<>();

    @OnMessage(Netmessages.CSVCMsg_CreateStringTable.class)
    public void onCreate(Context ctx, Netmessages.CSVCMsg_CreateStringTable message) {
        log.info("table created: {} with {} entries", message.getName(), message.getNumEntries());
        names.add(message.getName());
    }

    @OnMessage(Netmessages.CSVCMsg_UpdateStringTable.class)
    public void onUpdate(Context ctx, Netmessages.CSVCMsg_UpdateStringTable message) {
        if (!names.get(message.getTableId()).equals("instancebaseline")) {
            return;
        }
        StringTable table = ctx.getProcessor(StringTables.class).forId(message.getTableId());
        List<StringTableEntry> changes = StringTableDecoder.decode(table, message.getStringData(), message.getNumChangedEntries());
        Set<Integer> indices = new TreeSet<>();
        for (StringTableEntry c : changes) {
            indices.add(c.getIndex());
        }

        log.info("table updated: {} with {} entries: {}", names.get(message.getTableId()), message.getNumChangedEntries(), indices);
    }

//    @OnFullPacket
//    public void onFullPacket(Context ctx, Demo.CDemoFullPacket message) {
//        log.info("full packet received");
//        Demo.CDemoStringTables dst = message.getStringTable();
//        for (Demo.CDemoStringTables.table_t t : dst.getTablesList()) {
//            log.info("- table {} updated with {} items and {} clientside items", t.getTableName(), t.getItemsCount(), t.getItemsClientsideCount());
//        }
//    }


    public void run(String[] args) throws Exception {
        ControllableRunner runner = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        while(!runner.isAtEnd()) {
            runner.tick();
        }
        runner.halt();
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
