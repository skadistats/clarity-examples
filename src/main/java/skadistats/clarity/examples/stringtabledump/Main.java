package skadistats.clarity.examples.stringtabledump;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.Runner;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.OnStringTableCreated;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;

import java.util.HashSet;
import java.util.Set;

@UsesStringTable("*")
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private Set<String> names = new HashSet<>();

    @OnStringTableCreated
    public void onStringTableCreated(int numTables, StringTable table) {
        names.add(table.getName());
        System.out.println(table.getName());
    }


    public void runSeek(String[] args) throws Exception {
        Runner runner = new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        StringTables st = runner.getContext().getProcessor(StringTables.class);
        for (String name : names) {
            StringTable t = st.forName(name);
            System.out.println(t.toString());
        }
    }

    public static void main(String[] args) throws Exception {
        new Main().runSeek(args);
    }

}
