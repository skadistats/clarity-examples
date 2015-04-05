package skadistats.clarity.examples.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.entities.Entities;
import skadistats.clarity.processor.entities.UsesEntities;
import skadistats.clarity.processor.runner.AbstractRunner;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.InputStreamSource;
import skadistats.clarity.util.TextTable;

import java.io.UnsupportedEncodingException;

@UsesEntities
public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        AbstractRunner<SimpleRunner> r = new SimpleRunner(new InputStreamSource(System.in)).runWith(this);
        summary(r.getContext());
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
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
            int baseIndex = ps.getDtClass().getPropertyIndex(columns[c][1] + ".0000");
            for (int r = 0; r < 10; r++) {
                Object val = ps.getState()[baseIndex + r];
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
