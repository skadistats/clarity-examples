package skadistats.clarity.examples.matchend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.ControllableRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.util.TextTable;

import java.io.UnsupportedEncodingException;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        ControllableRunner r = new ControllableRunner(new MappedFileSource(args[0])).runWith(this);
        r.seek(r.getLastTick());
        r.halt();
        if (r.getEngineType() == EngineType.SOURCE1) {
            summary(r.getContext());
        } else {
            summary2(r.getContext());
        }
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    private void summary2(Context ctx) throws UnsupportedEncodingException {
        Entity ps = ctx.getProcessor(Entities.class).getByDtName("CDOTA_PlayerResource");
        Entity dr = ctx.getProcessor(Entities.class).getByDtName("CDOTA_DataRadiant");
        Entity dd = ctx.getProcessor(Entities.class).getByDtName("CDOTA_DataDire");

        String[][] columns;
        if (ps.getDtClass().getFieldPathForName("m_vecPlayerData") != null) {
            columns = new String[][]{
                {"Name", "ps", "m_vecPlayerData.%i.m_iszPlayerName"},
                {"Level", "ps", "m_vecPlayerTeamData.%i.m_iLevel"},
                {"K", "ps",  "m_vecPlayerTeamData.%i.m_iKills"},
                {"D", "ps", "m_vecPlayerTeamData.%i.m_iDeaths"},
                {"A", "ps", "m_vecPlayerTeamData.%i.m_iAssists"},
                {"Gold", "d", "m_vecDataTeam.%i.m_iTotalEarnedGold"},
                {"LH", "d", "m_vecDataTeam.%i.m_iLastHitCount"},
                {"DN", "d", "m_vecDataTeam.%i.m_iDenyCount"},
            };
        } else {
            columns = new String[][]{
                {"Name", "ps", "m_iszPlayerNames.%i"},
                {"Level", "ps", "m_iLevel.%i"},
                {"K", "ps",  "m_iKills.%i"},
                {"D", "ps", "m_iDeaths.%i"},
                {"A", "ps", "m_iAssists.%i"},
                {"Gold", "ps", "m_iTotalEarnedGold.%i"},
                {"LH", "ps", "m_iLastHitCount.%i"},
                {"DN", "ps", "m_iDenyCount.%i"},
            };
        }

        TextTable.Builder b = new TextTable.Builder();
        for (int c = 0; c < columns.length; c++) {
            b.addColumn(columns[c][0], c == 0 ? TextTable.Alignment.LEFT : TextTable.Alignment.RIGHT);
        }
        TextTable t = b.build();

        for (int c = 0; c < columns.length; c++) {
            for (int r = 0; r < 10; r++) {
                String entityStr = columns[c][1];
                Entity e;
                int idx;
                if ("ps".equals(entityStr)) {
                    e = ps;
                    idx = r;
                } else {
                    e = r < 5 ? dr : dd;
                    idx = r % 5;
                }
                FieldPath fp = e.getDtClass().getFieldPathForName(columns[c][2].replace("%i", Util.arrayIdxToString(idx)));
                Object val = e.getPropertyForFieldPath(fp);
                String str = new String(val.toString().getBytes("ISO-8859-1"));
                t.setData(r, c, str);
            }
        }

        System.out.println(t);

    }

    private void summary(Context ctx) throws UnsupportedEncodingException {

        Entity ps = ctx.getProcessor(Entities.class).getByDtName("DT_DOTA_PlayerResource");

        String[][] columns = new String[][]{
            {"Name", "m_iszPlayerNames"},
            {"Level", "m_iLevel"},
            {"K", "m_iKills"},
            {"D", "m_iDeaths"},
            {"A", "m_iAssists"},
            {"Gold", "EndScoreAndSpectatorStats.m_iTotalEarnedGold"},
            {"LH", "m_iLastHitCount"},
            {"DN", "m_iDenyCount"},
        };

        TextTable.Builder b = new TextTable.Builder();
        for (int c = 0; c < columns.length; c++) {
            b.addColumn(columns[c][0], c == 0 ? TextTable.Alignment.LEFT : TextTable.Alignment.RIGHT);
        }
        TextTable t = b.build();

        for (int c = 0; c < columns.length; c++) {
            FieldPath base = ps.getDtClass().getFieldPathForName(columns[c][1] + ".0000");
            for (int r = 0; r < 10; r++) {
                FieldPath fp = new FieldPath(base);
                fp.path[0] += r;
                Object val = ps.getPropertyForFieldPath(fp);
                String str = new String(val.toString().getBytes("ISO-8859-1"));
                t.setData(r, c, str);
            }
        }

        System.out.println(t);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
