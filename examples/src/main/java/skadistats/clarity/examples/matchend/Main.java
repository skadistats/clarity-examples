package skadistats.clarity.examples.matchend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.io.Util;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.TextTable;
import skadistats.clarity.examples.shared.ReplayChooser;

import java.io.IOException;
import skadistats.clarity.examples.shared.Category;
import skadistats.clarity.examples.shared.Example;

/**
 * Prints the final scoreboard (name, level, kills, deaths, assists, gold, last hits, denies) of all Radiant and Dire
 * players of a Dota 2 replay.
 *
 * <p>Demonstrates {@link ControllableRunner}: it runs the parser on a background thread and lets the controlling
 * thread {@code seek} to a tick. The example seeks to the last tick, then reads the scoreboard directly from the
 * entities that exist at that moment, using {@link Entities#stream()} with {@link Entities#byDtName(String)},
 * {@link Entity#getFieldPathForName(String)} and {@link Entity#getPropertyForFieldPath(FieldPath)}. There are no
 * event listeners; {@link UsesEntities} only makes the runtime register the {@code Entities} processor.
 *
 * <p>Works with Dota 2 on both engines, and the property layout is picked at runtime:
 * <ul>
 * <li>Source 1 ({@code EngineId.DOTA_S1}): entity {@code DT_DOTA_PlayerResource}, one array property per statistic
 * (for example {@code m_iKills.0003}).</li>
 * <li>early Source 2 betas, detected by a missing {@code m_vecPlayerData} property: the same per-statistic arrays on
 * {@code CDOTA_PlayerResource}.</li>
 * <li>later Source 2: per-player structs in {@code m_vecPlayerData.<i>} and {@code m_vecPlayerTeamData.<i>} on
 * {@code CDOTA_PlayerResource}, and gold, last hits and denies in {@code m_vecDataTeam.<pos>} on
 * {@code CDOTA_DataRadiant} / {@code CDOTA_DataDire}.</li>
 * </ul>
 * Team ids 2 and 3 are Radiant and Dire; other teams (such as spectators) are skipped. Logs the total run time.
 *
 * <p>Run:
 * <pre>
 * ./gradlew :examples:matchendRun --args "path/to/replay.dem"
 * </pre>
 */
@UsesEntities
@Example(name = "matchend", description = "Display final match scoreboard with player stats", category = Category.DOCS)
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public static void main(String[] args) throws Exception {
        String replay = ReplayChooser.choose(args);
        if (replay == null) return;
        long tStart = System.currentTimeMillis();
        new Main(replay).showScoreboard();
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }



    private final ControllableRunner runner;

    public Main(String fileName) throws IOException, InterruptedException {
        try (MappedFileSource source = new MappedFileSource(fileName)) {
            // runWith starts the runner thread and returns; the runner then waits at the end of tick 0.
            runner = new ControllableRunner(source).runWith(this);
            try {
                // Blocks until the end of the last tick is reached; the entity state is then the final state.
                runner.seek(runner.getLastTick());
            } finally {
                // The runner thread does not end by itself: halt it and wait for it before the source is closed.
                runner.halt();
                runner.join();
            }
        }
    }

    /** Chooses the property layout for the replay's engine and format, then prints the table. */
    private void showScoreboard() {
        boolean isSource1 = runner.getEngineType().getId() == EngineId.DOTA_S1;
        boolean isEarlyBetaFormat = !isSource1 && getEntity("PlayerResource").getFieldPathForName("m_vecPlayerData") == null;
        if (isSource1 || isEarlyBetaFormat) {
            showTableWithColumns(
                    new DefaultResolver<Integer>("PlayerResource", "m_iPlayerTeams.%i"),
                    new ColumnDef("Name", new DefaultResolver<String>("PlayerResource", "m_iszPlayerNames.%i")),
                    new ColumnDef("Level", new DefaultResolver<Integer>("PlayerResource", "m_iLevel.%i")),
                    new ColumnDef("K", new DefaultResolver<Integer>("PlayerResource", "m_iKills.%i")),
                    new ColumnDef("D", new DefaultResolver<Integer>("PlayerResource", "m_iDeaths.%i")),
                    new ColumnDef("A", new DefaultResolver<Integer>("PlayerResource", "m_iAssists.%i")),
                    new ColumnDef("Gold", new DefaultResolver<Integer>("PlayerResource", (isSource1 ? "EndScoreAndSpectatorStats." : "") + "m_iTotalEarnedGold.%i")),
                    new ColumnDef("LH", new DefaultResolver<Integer>("PlayerResource", "m_iLastHitCount.%i")),
                    new ColumnDef("DN", new DefaultResolver<Integer>("PlayerResource", "m_iDenyCount.%i"))
            );
        } else {
            showTableWithColumns(
                    new DefaultResolver<Integer>("PlayerResource", "m_vecPlayerData.%i.m_iPlayerTeam"),
                    new ColumnDef("Name", new DefaultResolver<String>("PlayerResource", "m_vecPlayerData.%i.m_iszPlayerName")),
                    new ColumnDef("Level", new DefaultResolver<Integer>("PlayerResource", "m_vecPlayerTeamData.%i.m_iLevel")),
                    new ColumnDef("K", new DefaultResolver<Integer>("PlayerResource", "m_vecPlayerTeamData.%i.m_iKills")),
                    new ColumnDef("D", new DefaultResolver<Integer>("PlayerResource", "m_vecPlayerTeamData.%i.m_iDeaths")),
                    new ColumnDef("A", new DefaultResolver<Integer>("PlayerResource", "m_vecPlayerTeamData.%i.m_iAssists")),
                    new ColumnDef("Gold", new DefaultResolver<Integer>("Data%n", "m_vecDataTeam.%p.m_iTotalEarnedGold")),
                    new ColumnDef("LH", new DefaultResolver<Integer>("Data%n", "m_vecDataTeam.%p.m_iLastHitCount")),
                    new ColumnDef("DN", new DefaultResolver<Integer>("Data%n", "m_vecDataTeam.%p.m_iDenyCount"))
            );
        }
    }

    /**
     * Prints one row per Radiant or Dire player. {@code teamResolver} yields the team of player slot {@code idx};
     * the first column's resolver is evaluated for every slot until it throws, which ends the loop.
     */
    private void showTableWithColumns(ValueResolver<Integer> teamResolver, ColumnDef... columnDefs) {
        TextTable.Builder b = new TextTable.Builder();
        for (int c = 0; c < columnDefs.length; c++) {
            b.addColumn(columnDefs[c].name, c == 0 ? TextTable.Alignment.LEFT : TextTable.Alignment.RIGHT);
        }
        TextTable table = b.build();

        int team = 0;
        int pos = 0;
        int r = 0;

        for (int idx = 0; idx < 256; idx++) {
            try {
                int newTeam = teamResolver.resolveValue(idx, team, pos);
                if (newTeam != team) {
                    team = newTeam;
                    pos = 0;
                } else {
                    pos++;
                }
            } catch (Exception e) {
                // when the team resolver throws an exception, this was the last index there was
                break;
            }
            if (team != 2 && team != 3) {
                continue;
            }
            for (int c = 0; c < columnDefs.length; c++) {
                table.setData(r, c, columnDefs[c].resolver.resolveValue(idx, team, pos));
            }
            r++;
        }

        System.out.println(table);
    }

    /** Maps a generic entity name to the engine's DT class name: {@code DT_DOTA_} prefix on Source 1, {@code CDOTA_} on Source 2. */
    private String getEngineDependentEntityName(String entityName) {
        switch (runner.getEngineType().getId()) {
            case DOTA_S1:
                return "DT_DOTA_" + entityName;
            case DOTA_S2:
                return "CDOTA_" + entityName;
            default:
                throw new RuntimeException("invalid engine type");
        }
    }

    private String getTeamName(int team) {
        switch(team) {
            case 2:
                return "Radiant";
            case 3:
                return "Dire";
            default:
                return "";
        }
    }

    /** Returns the first entity whose DT class name equals the engine-specific form of {@code entityName}, or {@code null}. */
    private Entity getEntity(String entityName) {
        return runner.getContext().getProcessor(Entities.class).stream()
                .filter(Entities.byDtName(getEngineDependentEntityName(entityName)))
                .findFirst()
                .orElse(null);
    }

    /** A table column: header text and how to obtain the cell value for a player. */
    private class ColumnDef {
        private final String name;
        private final ValueResolver<?> resolver;

        public ColumnDef(String name, ValueResolver<?> resolver) {
            this.name = name;
            this.resolver = resolver;
        }
    }

    /** Computes a cell value from the player slot index, the team id and the player's position within the team. */
    private interface ValueResolver<V> {
        V resolveValue(int index, int team, int pos);
    }

    /**
     * Resolves a property by name pattern on a named entity. In {@code pattern}, {@code %i}, {@code %t} and {@code %p}
     * are replaced by the slot index, team id and position in team, each as a four-digit zero-padded array index
     * ({@link Util#arrayIdxToString(int)}), which is how array elements appear in property names. In
     * {@code entityName}, {@code %n} is replaced by {@code Radiant} or {@code Dire}.
     */
    private class DefaultResolver<V> implements ValueResolver<V> {
        private final String entityName;
        private final String pattern;

        public DefaultResolver(String entityName, String pattern) {
            this.entityName = entityName;
            this.pattern = pattern;
        }

        @Override
        public V resolveValue(int index, int team, int pos) {
            String fieldPathString = pattern
                    .replaceAll("%i", Util.arrayIdxToString(index))
                    .replaceAll("%t", Util.arrayIdxToString(team))
                    .replaceAll("%p", Util.arrayIdxToString(pos));
            String compiledName = entityName.replaceAll("%n", getTeamName(team));
            Entity entity = getEntity(compiledName);
            FieldPath fieldPath = entity.getFieldPathForName(fieldPathString);
            return entity.getPropertyForFieldPath(fieldPath);
        }
    }

}
