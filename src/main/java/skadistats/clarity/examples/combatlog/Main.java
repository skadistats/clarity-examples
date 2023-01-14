package skadistats.clarity.examples.combatlog;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.model.CombatLogEntry;
import skadistats.clarity.processor.gameevents.OnCombatLogEntry;
import skadistats.clarity.processor.runner.SimpleRunner;
import skadistats.clarity.source.MappedFileSource;
import skadistats.clarity.wire.common.proto.DotaUserMessages;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Main {

    private final Logger log = LoggerFactory.getLogger(Main.class.getPackage().getClass());

    private final DateTimeFormatter GAMETIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private String compileName(String attackerName, boolean isIllusion) {
        return attackerName != null ? attackerName + (isIllusion ? " (illusion)" : "") : "UNKNOWN";
    }

    private String getAttackerNameCompiled(CombatLogEntry cle) {
        return compileName(cle.getAttackerName(), cle.isAttackerIllusion());
    }

    private String getTargetNameCompiled(CombatLogEntry cle) {
        return compileName(cle.getTargetName(), cle.isTargetIllusion());
    }

    @OnCombatLogEntry
    public void onCombatLogEntry(CombatLogEntry cle) {
        Duration gameTimeMillis = Duration.ofMillis((int) (1000.0f * cle.getTimestamp()));
        LocalTime gameTime = LocalTime.MIDNIGHT.plus(gameTimeMillis);
        String time = "[" + GAMETIME_FORMATTER.format(gameTime) + "]";
        switch (cle.getType()) {
            case DOTA_COMBATLOG_DAMAGE:
                log.info("{} {} hits {}{} for {} damage{}",
                    time,
                    getAttackerNameCompiled(cle),
                    getTargetNameCompiled(cle),
                    cle.getInflictorName() != null ? String.format(" with %s", cle.getInflictorName()) : "",
                    cle.getValue(),
                    cle.getHealth() != 0 ? String.format(" (%s->%s)", cle.getHealth() + cle.getValue(), cle.getHealth()) : ""
                );
                break;
            case DOTA_COMBATLOG_HEAL:
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
            case DOTA_COMBATLOG_MODIFIER_ADD:
                log.info("{} {} receives {} buff/debuff from {}",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getInflictorName(),
                    getAttackerNameCompiled(cle)
                );
                break;
            case DOTA_COMBATLOG_MODIFIER_REMOVE:
                log.info("{} {} loses {} buff/debuff",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getInflictorName()
                );
                break;
            case DOTA_COMBATLOG_DEATH:
                log.info("{} {} is killed by {}",
                    time,
                    getTargetNameCompiled(cle),
                    getAttackerNameCompiled(cle)
                );
                break;
            case DOTA_COMBATLOG_ABILITY:
                log.info("{} {} {} ability {} (lvl {}){}{}",
                    time,
                    getAttackerNameCompiled(cle),
                    cle.isAbilityToggleOn() || cle.isAbilityToggleOff() ? "toggles" : "casts",
                    cle.getInflictorName(),
                    cle.getAbilityLevel(),
                    cle.isAbilityToggleOn() ? " on" : cle.isAbilityToggleOff() ? " off" : "",
                    cle.getTargetName() != null ? " on " + getTargetNameCompiled(cle) : ""
                );
                break;
            case DOTA_COMBATLOG_ITEM:
                log.info("{} {} uses {}",
                    time,
                    getAttackerNameCompiled(cle),
                    cle.getInflictorName()
                );
                break;
            case DOTA_COMBATLOG_GOLD:
                log.info("{} {} {} {} gold",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getValue() < 0 ? "looses" : "receives",
                    Math.abs(cle.getValue())
                );
                break;
            case DOTA_COMBATLOG_GAME_STATE:
                log.info("{} game state is now {}",
                    time,
                    cle.getValue()
                );
                break;
            case DOTA_COMBATLOG_XP:
                log.info("{} {} gains {} XP",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getValue()
                );
                break;
            case DOTA_COMBATLOG_PURCHASE:
                log.info("{} {} buys item {}",
                    time,
                    getTargetNameCompiled(cle),
                    cle.getValueName()
                );
                break;
            case DOTA_COMBATLOG_BUYBACK:
                log.info("{} player in slot {} has bought back",
                    time,
                    cle.getValue()
                );
                break;

            default:
                DotaUserMessages.DOTA_COMBATLOG_TYPES type = cle.getType();
                log.info("\n{} ({}): {}\n", type.name(), type.ordinal(), cle);
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
