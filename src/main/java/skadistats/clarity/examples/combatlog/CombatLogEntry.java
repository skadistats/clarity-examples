package skadistats.clarity.examples.combatlog;

import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.model.StringTable;

public class CombatLogEntry {

    private static StringTable combatLogNames;

    private static int typeIdx;
    private static int sourceNameIdx;
    private static int targetNameIdx;
    private static int attackerNameIdx;
    private static int inflictorNameIdx;
    private static int attackerIllusionIdx;
    private static int targetIllusionIdx;
    private static int valueIdx;
    private static int healthIdx;
    private static int timestampIdx;
    private static int targetSourceNameIdx;
    
    public static void init(StringTable combatLogNamesTable, GameEventDescriptor descriptor) {
        combatLogNames = combatLogNamesTable;
        typeIdx = descriptor.getIndexForKey("type");
        sourceNameIdx = descriptor.getIndexForKey("sourcename");
        targetNameIdx = descriptor.getIndexForKey("targetname");
        attackerNameIdx = descriptor.getIndexForKey("attackername");
        inflictorNameIdx = descriptor.getIndexForKey("inflictorname");
        attackerIllusionIdx = descriptor.getIndexForKey("attackerillusion");
        targetIllusionIdx = descriptor.getIndexForKey("targetillusion");
        valueIdx = descriptor.getIndexForKey("value");
        healthIdx = descriptor.getIndexForKey("health");
        timestampIdx = descriptor.getIndexForKey("timestamp");
        targetSourceNameIdx = descriptor.getIndexForKey("targetsourcename");
    }
    
    private final GameEvent event;
    
    public CombatLogEntry(GameEvent event) {
        this.event = event;
    }
    
    private String readCombatLogName(int idx) {
        return idx == 0 ? null : combatLogNames.getNameByIndex(idx);
    }
    
    private String translate(String in) {
        // TODO: translate modifier_XXX, or npc_hero_XXX into correct names...
        return in;
    }
    
    public int getType() {
        return event.getProperty(typeIdx);
    }
    
    public String getSourceName() {
        return translate(readCombatLogName((int)event.getProperty(sourceNameIdx)));
    }
    
    public String getTargetName() {
        return translate(readCombatLogName((int)event.getProperty(targetNameIdx)));
    }
    
    public String getTargetNameCompiled() {
        return getTargetName() + (isTargetIllusion() ? " (Illusion)" : "");
    }

    public String getAttackerName() {
        return translate(readCombatLogName((int)event.getProperty(attackerNameIdx)));
    }
    
    public String getAttackerNameCompiled() {
        return getAttackerName() + (isAttackerIllusion() ? " (Illusion)" : "");
    }

    public String getInflictorName() {
        return translate(readCombatLogName((int)event.getProperty(inflictorNameIdx)));
    }
    
    public boolean isAttackerIllusion() {
        return event.getProperty(attackerIllusionIdx);
    }
    
    public boolean isTargetIllusion() {
        return event.getProperty(targetIllusionIdx);
    }
    
    public int getValue() {
        return event.getProperty(valueIdx);
    }
    
    public int getHealth() {
        return event.getProperty(healthIdx);
    }
    
    public float getTimestamp() {
        return event.getProperty(timestampIdx);
    }
    
    public String getTargetSourceName() {
        return translate(readCombatLogName((int)event.getProperty(targetSourceNameIdx)));
    }
    
}
