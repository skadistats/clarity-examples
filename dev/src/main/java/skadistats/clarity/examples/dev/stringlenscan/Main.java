package skadistats.clarity.examples.dev.stringlenscan;

import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.resources.Resources;
import skadistats.clarity.processor.resources.UsesResources;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.processor.stringtables.OnStringTableCreated;
import skadistats.clarity.processor.stringtables.StringTables;
import skadistats.clarity.processor.stringtables.UsesStringTable;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Example(name = "stringlenscan", description = "Scan all replays and report max string lengths in string tables and resources", category = Category.DEV)
public class Main {

    @UsesStringTable("*")
    static class Scanner {
        final List<String> tableNames = new ArrayList<>();

        @OnStringTableCreated
        public void onStringTableCreated(int numTables, StringTable table) {
            tableNames.add(table.getName());
        }
    }

    @UsesStringTable("*")
    @UsesResources
    static class ScannerS2 extends Scanner {}

    // String table aggregates
    private final Map<String, Integer> maxEntryLen = new TreeMap<>();
    private final Map<String, String> maxEntryExample = new TreeMap<>();
    private final Map<String, Set<String>> tableEngines = new TreeMap<>();
    private int maxTableNameLen = 0;
    private String maxTableNameExample = "";

    // Resources aggregates
    private int maxResDir = 0;
    private String maxResDirExample = "";
    private int maxResExt = 0;
    private String maxResExtExample = "";
    private int maxResName = 0;
    private String maxResNameExample = "";
    private int maxResPath = 0;
    private String maxResPathExample = "";

    private int totalReplays = 0;
    private int failedReplays = 0;

    void run() throws Exception {
        var replayDir = Path.of("replays");
        try (var paths = Files.walk(replayDir, FileVisitOption.FOLLOW_LINKS)) {
            paths.filter(p -> p.toString().endsWith(".dem"))
                 .sorted()
                 .forEach(this::processReplay);
        }
        printReport();
    }

    private void processReplay(Path path) {
        totalReplays++;
        var engine = detectEngine(path);
        var scanner = "S2".equals(engine) ? new ScannerS2() : new Scanner();
        try (var source = new MappedFileSource(path.toString())) {
            var runner = new SimpleRunner(source).runWith(scanner);
            var st = runner.getContext().getProcessor(StringTables.class);

            for (var name : scanner.tableNames) {
                if (name.length() > maxTableNameLen) {
                    maxTableNameLen = name.length();
                    maxTableNameExample = name;
                }
                tableEngines.computeIfAbsent(name, k -> new TreeSet<>()).add(engine);
                var table = st.forName(name);
                if (table == null) continue;
                for (int i = 0; i < table.getEntryCount(); i++) {
                    var entryName = table.getNameByIndex(i);
                    if (entryName == null) continue;
                    var prev = maxEntryLen.getOrDefault(name, 0);
                    if (entryName.length() > prev) {
                        maxEntryLen.put(name, entryName.length());
                        maxEntryExample.put(name, entryName);
                    }
                }
            }

            if ("S2".equals(engine)) {
                var resources = runner.getContext().getProcessor(Resources.class);
                collectResourceStats(resources.getGameSessionManifest());
                for (var m : resources.getManifests()) {
                    collectResourceStats(m);
                }
            }
        } catch (Exception e) {
            failedReplays++;
            System.err.printf("SKIP %s: %s%n", path.getFileName(), e.getMessage());
        }
    }

    private void collectResourceStats(Resources.Manifest manifest) {
        if (manifest == null) return;
        for (var entry : manifest.getEntries()) {
            var dir = entry.getDir();
            var name = entry.getName();
            var ext = entry.getExtension();
            var path = entry.toString();
            if (dir != null && dir.length() > maxResDir) { maxResDir = dir.length(); maxResDirExample = dir; }
            if (name != null && name.length() > maxResName) { maxResName = name.length(); maxResNameExample = name; }
            if (ext != null && ext.length() > maxResExt) { maxResExt = ext.length(); maxResExtExample = ext; }
            if (path.length() > maxResPath) { maxResPath = path.length(); maxResPathExample = path; }
        }
    }

    private static String detectEngine(Path path) {
        var s = path.toString();
        if (s.contains("/s1/") || s.contains("\\s1\\")) return "S1";
        if (s.contains("/s2/") || s.contains("\\s2\\")) return "S2";
        return "??";
    }

    private void printReport() {
        System.out.printf("Replays: %d scanned, %d failed%n%n", totalReplays, failedReplays);
        System.out.printf("Max table name length: %d (\"%s\")%n%n", maxTableNameLen, maxTableNameExample);

        System.out.printf("%-38s  %4s  %-6s  %s%n", "Table", "Max", "Engine", "Longest entry name");
        System.out.println("-".repeat(110));
        maxEntryLen.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .forEach(e -> {
                var name = e.getKey();
                var len = e.getValue();
                var engines = String.join(",", tableEngines.getOrDefault(name, Collections.emptySet()));
                var ex = maxEntryExample.getOrDefault(name, "");
                if (ex.length() > 60) ex = ex.substring(0, 57) + "...";
                System.out.printf("%-38s  %4d  %-6s  %s%n", name, len, engines, ex);
            });

        var noEntries = new ArrayList<String>();
        for (var name : tableEngines.keySet()) {
            if (!maxEntryLen.containsKey(name)) noEntries.add(name);
        }
        if (!noEntries.isEmpty()) {
            System.out.println();
            System.out.println("Tables with no entries observed:");
            noEntries.stream().sorted().forEach(n ->
                System.out.printf("  %-38s  %s%n", n, String.join(",", tableEngines.get(n))));
        }

        System.out.println();
        System.out.println("Resources (S2 only):");
        System.out.printf("  Dirs:       max %4d  \"%s\"%n", maxResDir, maxResDirExample);
        System.out.printf("  Extensions: max %4d  \"%s\"%n", maxResExt, maxResExtExample);
        System.out.printf("  Names:      max %4d  \"%s\"%n", maxResName, maxResNameExample);
        System.out.printf("  Full paths: max %4d  \"%s\"%n", maxResPath,
            maxResPathExample.length() > 80 ? maxResPathExample.substring(0, 77) + "..." : maxResPathExample);
    }

    public static void main(String[] args) throws Exception {
        new Main().run();
    }
}
