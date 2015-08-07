package skadistats.clarity.examples.combatlog;

import org.joda.time.Duration;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.processor.gameevents.CombatLog;
import skadistats.clarity.processor.gameevents.OnCombatLogEntry;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private final PeriodFormatter GAMETIME_FORMATTER = new PeriodFormatterBuilder()
        .minimumPrintedDigits(2)
        .printZeroAlways()
        .appendHours()
        .appendLiteral(":")
        .appendMinutes()
        .appendLiteral(":")
        .appendSeconds()
        .appendLiteral(".")
        .appendMillis3Digit()
        .toFormatter();

    private String getAttackerNameCompiled(CombatLog.Entry cle) {
        return cle.getAttackerName() + (cle.isAttackerIllusion() ? " (illusion)" : "");
    }

    private String getTargetNameCompiled(CombatLog.Entry cle) {
        return cle.getTargetName() + (cle.isTargetIllusion() ? " (illusion)" : "");
    }


    @OnCombatLogEntry
    public void onCombatLogEntry(Context ctx, CombatLog.Entry cle) {
        String time = "[" + GAMETIME_FORMATTER.print(Duration.millis((int) (1000.0f * cle.getTimestamp())).toPeriod()) + "]";
        switch (cle.getType()) {
            case 0:
                log.info("{} {} hits {}{} for {} damage{}",
                    time,
                    getAttackerNameCompiled(cle),
                    getTargetNameCompiled(cle),
                    cle.getInflictorName() != null ? String.format(" with %s", cle.getInflictorName()) : "",
                    cle.getValue(),
                    cle.getHealth() != 0 ? String.format(" (%s->%s)", cle.getHealth() + cle.getValue(), cle.getHealth()) : ""
                );
                break;
            case 1:
                log.info("{} {}'s {} heals {} for {} health ({}->{})",
                    time,
                    getAttackerNameCompiled(cle),
                    cle.getInflictorName(),
                    getTargetNameCompiled(cle),
                    cle.getValue(),
                    cle.getHealth() - cle.getValue(),
                    cle.getHealth()
                );
                break;
            case 2:
                log.info("{} {} receives {} buff/debuff from {}",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getInflictorName(),
                    getAttackerNameCompiled(cle)
                );
                break;
            case 3:
                log.info("{} {} loses {} buff/debuff",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getInflictorName()
                );
                break;
            case 4:
                log.info("{} {} is killed by {}",
                    time,
                    getTargetNameCompiled(cle),
                    getAttackerNameCompiled(cle)
                );
                break;
            case 5:
                log.info("{} {} {} ability {} (lvl {}){}{}",
                    time,
                    getAttackerNameCompiled(cle),
                    cle.isAbilityToggleOn() || cle.isAbilityToggleOff() ? "toggles" : "casts",
                    cle.getInflictorName(),
                    cle.getAbilityLevel(),
                    cle.isAbilityToggleOn() ? " on" : cle.isAbilityToggleOff() ? " off" : "",
                    cle.getTargetName() != null ? " on " + getAttackerNameCompiled(cle) : ""
                );
                break;
            case 6:
                log.info("{} {} uses {}",
                    time,
                    getAttackerNameCompiled(cle),
                    cle.getInflictorName()
                );
                break;
            case 8:
                log.info("{} {} {} {} gold",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getValue() < 0 ? "looses" : "receives",
                    Math.abs(cle.getValue())
                );
                break;
            case 9:
                log.info("{} game state is now {}",
                    time,
                    cle.getValue()
                );
                break;
            case 10:
                log.info("{} {} gains {} XP",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getValue()
                );
                break;
            case 11:
                log.info("{} {} buys item {}",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getValue()
                );
                break;
            case 12:
                log.info("{} player in slot {} has bought back",
                    time,
                    cle.getValue()
                );
                break;

            default:
                DotaUserMessages.DOTA_COMBATLOG_TYPES type = DotaUserMessages.DOTA_COMBATLOG_TYPES.valueOf(cle.getType());
                log.info("\n{} ({}): {}\n", type.name(), type.ordinal(), cle.getGameEvent());
                break;

        }
    }

    public void run(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        new SimpleRunner(new MappedFileSource(args[0])).runWith(this);
        long tMatch = System.currentTimeMillis() - tStart;
        log.info("total time taken: {}s", (tMatch) / 1000.0);
    }

    public static void main(String[] args) throws Exception {
        new Main().run(args);
    }

}
