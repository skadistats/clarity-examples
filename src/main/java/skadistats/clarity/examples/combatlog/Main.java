package skadistats.clarity.examples.combatlog;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import skadistats.clarity.Clarity;
import skadistats.clarity.match.Match;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.parser.Profile;
import skadistats.clarity.parser.TickIterator;

public class Main {
    
    private static final PeriodFormatter GAMETIME_FORMATTER = new PeriodFormatterBuilder()
        .minimumPrintedDigits(2)
        .printZeroAlways()
        .appendMinutes()
        .appendLiteral(":")
        .appendSeconds()
        .appendLiteral(".")
        .appendMillis3Digit()
        .toFormatter();
    
    public static void main(String[] args) throws Exception {

        long tStart = System.currentTimeMillis();

        Logger log = LoggerFactory.getLogger("combatlog");

        boolean initialized = false;
        GameEventDescriptor combatLogDescriptor = null;
        Match match = new Match();
        TickIterator iter = Clarity.tickIteratorForFile(args[0], Profile.COMBAT_LOG);
        
        while(iter.hasNext()) {
            iter.next().apply(match);

            if (!initialized) {
                combatLogDescriptor = match.getGameEventDescriptors().forName("dota_combatlog"); 
                CombatLogEntry.init(
                    match.getStringTables().forName("CombatLogNames"), 
                    combatLogDescriptor
                );
                initialized = true;
            }
            
            for (GameEvent g : match.getGameEvents()) {
                if (g.getEventId() != combatLogDescriptor.getEventId()) {
                    continue;
                }
                CombatLogEntry cle = new CombatLogEntry(g);
                String time = "[" + GAMETIME_FORMATTER.print(Duration.millis((int)(1000.0f * cle.getTimestamp())).toPeriod()) +  "]";
                switch(cle.getType()) {
                    case 0:
                        log.info("{} {} hits {}{} for {} damage{}", 
                            time, 
                            cle.getAttackerNameCompiled(),
                            cle.getTargetNameCompiled(), 
                            cle.getInflictorName() != null ? String.format(" with %s", cle.getInflictorName()) : "",
                            cle.getValue(),
                            cle.getHealth() != 0 ? String.format(" (%s->%s)", cle.getHealth() + cle.getValue(), cle.getHealth()) : ""
                        );
                        break;
                    case 1:
                        log.info("{} {}'s {} heals {} for {} health ({}->{})", 
                            time, 
                            cle.getAttackerNameCompiled(), 
                            cle.getInflictorName(), 
                            cle.getTargetNameCompiled(), 
                            cle.getValue(), 
                            cle.getHealth() - cle.getValue(), 
                            cle.getHealth()
                        );
                        break;
                    case 2:
                        log.info("{} {} receives {} buff/debuff from {}", 
                            time, 
                            cle.getTargetNameCompiled(), 
                            cle.getInflictorName(), 
                            cle.getAttackerNameCompiled()
                        );
                        break;
                    case 3:
                        log.info("{} {} loses {} buff/debuff", 
                            time, 
                            cle.getTargetNameCompiled(), 
                            cle.getInflictorName()
                        );
                        break;
                    case 4:
                        log.info("{} {} is killed by {}", 
                            time, 
                            cle.getTargetNameCompiled(), 
                            cle.getAttackerNameCompiled()
                        );
                        break;
                    default:
                        log.info("\nUNKNOWN: {}\n", g);
                        break;
                }
            }
        }

        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
        
    }

}
