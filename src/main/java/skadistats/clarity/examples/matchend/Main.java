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

import java.io.IOException;

@UsesEntities
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public static void main(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new Main(args[0]).showScoreboard();
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }



    private final ControllableRunner runner;

    public Main(String fileName) throws IOException, InterruptedException {
        runner = new ControllableRunner(new MappedFileSource(fileName)).runWith(this);
        runner.seek(runner.getLastTick());
        runner.halt();
    }

    private void showScoreboard() {
        boolean isSource1 = runner.getEngineType().getId() == EngineId.SOURCE1;
        boolean isEarlyBetaFormat = !isSource1 && getEntity("PlayerResource").getDtClass().getFieldPathForName("m_vecPlayerData") == null;
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

    private String getEngineDependentEntityName(String entityName) {
        switch (runner.getEngineType().getId()) {
            case SOURCE1:
                return "DT_DOTA_" + entityName;
            case SOURCE2:
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

    private Entity getEntity(String entityName) {
        return runner.getContext().getProcessor(Entities.class).getByDtName(getEngineDependentEntityName(entityName));
    }

    private class ColumnDef {
        private final String name;
        private final ValueResolver<?> resolver;

        public ColumnDef(String name, ValueResolver<?> resolver) {
            this.name = name;
            this.resolver = resolver;
        }
    }

    private interface ValueResolver<V> {
        V resolveValue(int index, int team, int pos);
    }

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
            FieldPath fieldPath = entity.getDtClass().getFieldPathForName(fieldPathString);
            return entity.getPropertyForFieldPath(fieldPath);
        }
    }

}
