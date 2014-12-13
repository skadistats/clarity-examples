package skadistats.clarity.examples.matchend;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.Clarity;
import skadistats.clarity.match.Match;
import skadistats.clarity.model.Entity;
import skadistats.clarity.parser.Profile;
import skadistats.clarity.parser.TickIterator;

public class Main {

    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("simple");

        Match match = new Match();
        TickIterator iter = Clarity.tickIteratorForFile(args[0], Profile.ENTITIES);
        
        while(iter.hasNext()) {
            iter.next().apply(match);
        }

        class ColDef {
        	String columnName;
        	String propertyName;
        	List<String> values;
        	int width;
			public ColDef(String columnName, String propertyName) {
				this.columnName = columnName;
				this.propertyName = propertyName;
				this.width = columnName.length();
			}
        }
        
        ColDef[] columns = new ColDef[] {
        		new ColDef("Name", "m_iszPlayerNames"),
        		new ColDef("Level", "m_iLevel"),
        		new ColDef("K", "m_iKills"),
        		new ColDef("D", "m_iDeaths"),
        		new ColDef("A", "m_iAssists"),
        		new ColDef("Gold", "EndScoreAndSpectatorStats.m_iTotalEarnedGold"),
        		new ColDef("LH", "m_iLastHitCount"),
        		new ColDef("DN", "m_iDenyCount"),
        };
        
        Entity ps = match.getPlayerResource();
        
        for (ColDef c : columns) {
        	c.values = new ArrayList<String>();
        	int baseIndex = ps.getDtClass().getPropertyIndex(c.propertyName + ".0000");
        	for (int p = 0; p < 10; p++) {
        		String v = new String(ps.getState()[baseIndex + p].toString().getBytes("ISO-8859-1"));
        		c.values.add(v);
        		c.width = Math.max(c.width, v.length());
        	}
        }
        
    	StringBuffer buf = new StringBuffer();
    	String space = "                                                                  ";
        for (ColDef c : columns) {
        	buf.append(c.columnName);
        	buf.append(space, 0, c.width - c.columnName.length() + 2);
        }
    	System.out.println(buf);
        for (int p = 0; p < 10; p++) {
        	buf.setLength(0);
            for (ColDef c : columns) {
            	buf.append(c.values.get(p));
            	buf.append(space, 0, c.width - c.values.get(p).length() + 2);
            }
        	System.out.println(buf);
        }
        
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        
    }

}
