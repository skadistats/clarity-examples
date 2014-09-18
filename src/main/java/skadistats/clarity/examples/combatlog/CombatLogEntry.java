package skadistats.clarity.examples.combatlog;

import skadistats.clarity.model.GameEvent;

public class CombatLogEntry {

	private final CombatLogContext ctx;
    private final GameEvent event;
    
    public CombatLogEntry(CombatLogContext ctx, GameEvent event) {
    	this.ctx = ctx;
        this.event = event;
    }
    
    private String readCombatLogName(int idx) {
        return idx == 0 ? null : ctx.combatLogNames.getNameByIndex(idx);
    }
    
    private String translate(String in) {
        // TODO: translate modifier_XXX, or npc_hero_XXX into correct names...
        return in;
    }
    
    public int getType() {
        return event.getProperty(ctx.typeIdx);
    }
    
    public String getSourceName() {
        return translate(readCombatLogName((int)event.getProperty(ctx.sourceNameIdx)));
    }
    
    public String getTargetName() {
        return translate(readCombatLogName((int)event.getProperty(ctx.targetNameIdx)));
    }
    
    public String getTargetNameCompiled() {
        return getTargetName() + (isTargetIllusion() ? " (Illusion)" : "");
    }

    public String getAttackerName() {
        return translate(readCombatLogName((int)event.getProperty(ctx.attackerNameIdx)));
    }
    
    public String getAttackerNameCompiled() {
        return getAttackerName() + (isAttackerIllusion() ? " (Illusion)" : "");
    }

    public String getInflictorName() {
        return translate(readCombatLogName((int)event.getProperty(ctx.inflictorNameIdx)));
    }
    
    public boolean isAttackerIllusion() {
        return event.getProperty(ctx.attackerIllusionIdx);
    }
    
    public boolean isTargetIllusion() {
        return event.getProperty(ctx.targetIllusionIdx);
    }
    
    public int getValue() {
        return event.getProperty(ctx.valueIdx);
    }
    
    public int getHealth() {
        return event.getProperty(ctx.healthIdx);
    }
    
    public float getTimestamp() {
        return event.getProperty(ctx.timestampIdx);
    }
    
    public String getTargetSourceName() {
        return translate(readCombatLogName((int)event.getProperty(ctx.targetSourceNameIdx)));
    }
    
    public float getTimestampRaw() {
        return event.getProperty(ctx.timestampRawIdx);
    }
    
    public boolean isAttackerHero() {
        return event.getProperty(ctx.attackerHeroIdx);
    }

    public boolean isTargetHero() {
        return event.getProperty(ctx.targetHeroIdx);
    }
    
    public boolean isAbilityToggleOn() {
        return event.getProperty(ctx.abilityToggleOnIdx);
    }

    public boolean isAbilityToggleOff() {
        return event.getProperty(ctx.abilityToggleOffIdx);
    }

    public int getAbilityLevel() {
        return event.getProperty(ctx.abilityLevelIdx);
    }

    public int getGoldReason() {
        return event.getProperty(ctx.goldReasonIdx);
    }
    
}
