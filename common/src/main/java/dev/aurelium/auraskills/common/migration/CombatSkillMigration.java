package dev.aurelium.auraskills.common.migration;

import dev.aurelium.auraskills.api.ability.Abilities;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.user.User;

/** Migrates legacy Fighting and Archery progress into the unified Combat skill. */
public final class CombatSkillMigration {

    private static final String MARKER = "combat_merge_v1";

    private CombatSkillMigration() {
    }

    public static void migrate(AuraSkillsPlugin plugin, User user) {
        var markerData = user.getAbilityData(Abilities.FIGHTER);
        if (markerData.getBoolean(MARKER)) return;
        if (!Skills.FIGHTING.isEnabled() || Skills.ARCHERY.isEnabled()) return;

        double fightingTotal = cumulativeXp(plugin, user, Skills.FIGHTING);
        double archeryTotal = cumulativeXp(plugin, user, Skills.ARCHERY);
        applyCumulativeXp(plugin, user, Skills.FIGHTING, (fightingTotal + archeryTotal) * 0.5);
        markerData.setData(MARKER, true);
    }

    private static double cumulativeXp(AuraSkillsPlugin plugin, User user, Skill skill) {
        int startLevel = plugin.config().getStartLevel();
        int level = Math.max(startLevel, user.getSkillLevel(skill));
        double total = Math.max(0.0, user.getSkillXp(skill));
        for (int reachedLevel = startLevel + 1; reachedLevel <= level; reachedLevel++) {
            int required = plugin.getXpRequirements().getXpRequired(skill, reachedLevel);
            if (required <= 0) break;
            total += required;
        }
        return total;
    }

    private static void applyCumulativeXp(AuraSkillsPlugin plugin, User user, Skill skill, double total) {
        int level = plugin.config().getStartLevel();
        int maxLevel = skill.getMaxLevel();
        double remaining = Math.max(0.0, total);
        while (level < maxLevel) {
            int required = plugin.getXpRequirements().getXpRequired(skill, level + 1);
            if (required <= 0 || remaining < required) break;
            remaining -= required;
            level++;
        }
        user.setSkillLevel(skill, level);
        user.setSkillXp(skill, level >= maxLevel ? 0.0 : remaining);
    }
}
