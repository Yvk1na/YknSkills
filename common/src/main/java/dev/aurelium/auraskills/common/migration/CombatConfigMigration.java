package dev.aurelium.auraskills.common.migration;

import dev.aurelium.auraskills.common.AuraSkillsPlugin;
import dev.aurelium.auraskills.common.util.file.FileUtil;
import org.spongepowered.configurate.ConfigurationNode;

import java.io.File;
import java.util.List;

/** Applies the one-time configuration changes for the Fighting/Archery merge. */
public final class CombatConfigMigration {

    private static final int VERSION = 1;
    private final AuraSkillsPlugin plugin;

    public CombatConfigMigration(AuraSkillsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean migrate() {
        File skillsFile = file("skills.yml");
        try {
            ConfigurationNode skills = FileUtil.loadYamlFile(skillsFile);
            if (skills.node("combat_merge_version").getInt(0) >= VERSION) return false;

            updateSkills(skills);
            updateAbilities();
            updateSources();
            updateRewards();
            updateMessages("en");
            updateMessages("zh-CN");

            skills.node("combat_merge_version").set(VERSION);
            FileUtil.saveYamlFile(skillsFile, skills);
            plugin.logger().info("Migrated Fighting and Archery into the Combat skill");
            return true;
        } catch (Exception exception) {
            plugin.logger().warn("Failed to migrate Combat skill configuration: " + exception.getMessage());
            exception.printStackTrace();
            return false;
        }
    }

    private void updateSkills(ConfigurationNode root) throws Exception {
        ConfigurationNode archery = root.node("skills", "auraskills/archery");
        archery.node("options", "enabled").set(false);
        archery.node("mana_ability").set(null);

        ConfigurationNode fighting = root.node("skills", "auraskills/fighting");
        fighting.node("abilities").set(List.of(
                "auraskills/parry",
                "auraskills/fighter",
                "auraskills/sword_master",
                "auraskills/first_strike",
                "auraskills/bleed"));
        fighting.node("mana_ability").set(null);
        fighting.node("options", "enabled").set(true);
        fighting.node("options", "max_level").set(100);
        fighting.node("options", "entity_xp_multiplier").set(0.5);
    }

    private void updateAbilities() throws Exception {
        File target = file("abilities.yml");
        ConfigurationNode root = FileUtil.loadYamlFile(target);

        setAbility(root, "parry", 6.0, 2.0, 18);
        ConfigurationNode parry = ability(root, "parry");
        parry.node("ranged_base_value").set(5.0);
        parry.node("ranged_value_per_level").set(5.0);
        parry.node("ranged_delay_sec").set(3.0);

        setAbility(root, "fighter", 10.0, 10.0, 10);
        setAbility(root, "sword_master", 2.0, 2.0, 20);

        setAbility(root, "first_strike", 15.0, 5.0, 18);
        ConfigurationNode firstStrike = ability(root, "first_strike");
        firstStrike.node("ranged_base_value").set(9.0);
        firstStrike.node("ranged_value_per_level").set(3.0);

        setAbility(root, "bleed", 3.0, 3.0, 20);
        ConfigurationNode bleed = ability(root, "bleed");
        bleed.node("secondary_base_value").set(0.5);
        bleed.node("secondary_value_per_level").set(0.5);
        bleed.node("ranged_base_value").set(1.0);
        bleed.node("ranged_value_per_level").set(1.0);
        bleed.node("ranged_speed_reduction").set(0.2);

        FileUtil.saveYamlFile(target, root);
    }

    private void setAbility(ConfigurationNode root, String key, double base, double perLevel, int maxLevel)
            throws Exception {
        ConfigurationNode node = ability(root, key);
        node.node("enabled").set(true);
        node.node("base_value").set(base);
        node.node("value_per_level").set(perLevel);
        node.node("max_level").set(maxLevel);
    }

    private ConfigurationNode ability(ConfigurationNode root, String key) {
        return root.node("abilities", "auraskills/" + key);
    }

    private void updateSources() throws Exception {
        File target = file("sources/fighting.yml");
        ConfigurationNode root = FileUtil.loadYamlFile(target);
        root.node("default", "damager").set(null);
        root.node("default", "damagers").set(List.of("player", "projectile"));
        FileUtil.saveYamlFile(target, root);
    }

    private void updateRewards() throws Exception {
        File target = file("rewards/fighting.yml");
        if (!target.exists()) plugin.saveResource("rewards/fighting.yml", false);
        ConfigurationNode root = FileUtil.loadYamlFile(target);
        root.node("patterns").set(List.of(
                statPattern("crit_damage", 1),
                statPattern("regeneration", 2),
                statPattern("crit_chance", 1),
                statPattern("strength", 2)));
        FileUtil.saveYamlFile(target, root);
    }

    private java.util.Map<String, Object> statPattern(String stat, int interval) {
        return java.util.Map.of(
                "type", "stat",
                "stat", stat,
                "value", 1,
                "pattern", java.util.Map.of("interval", interval));
    }

    private void updateMessages(String language) throws Exception {
        File target = file("messages/messages_" + language + ".yml");
        if (!target.exists()) return;
        ConfigurationNode root = FileUtil.loadYamlFile(target);
        boolean chinese = language.equals("zh-CN");

        setMessages(root.node("skills", "fighting"),
                chinese ? "战斗" : "Combat",
                chinese ? "使用近战武器或弓箭击杀生物以获得战斗 {xp_unit}"
                        : "Defeat mobs with melee weapons or bows to earn Combat {xp_unit}", null);
        setMessages(root.node("abilities", "parry"),
                chinese ? "战斗反射" : "Combat Reflexes",
                chinese ? "近战：在攻击落空后的 {time} 秒内招架，使所受伤害降低 {value}% 并免疫击退。远程：落地 {delay} 秒后的箭矢会在 {ranged_value} 格范围内自动回收。"
                        : "Melee: Missing a sword swing within {time}s before being hit reduces damage by {value}% and cancels knockback. Ranged: Arrows are retrieved after {delay}s when within {ranged_value} blocks.",
                chinese ? "招架 {value}% / 箭矢回收" : "{value}% Parry / Arrow Retrieval");
        setMessages(root.node("abilities", "fighter"),
                chinese ? "战斗专精" : "Combat Training",
                chinese ? "近战与弓箭击杀获得额外 {value}% 的战斗经验。"
                        : "Earn {value}% more Combat XP from melee and ranged kills.",
                chinese ? "+{value}% 熟练度" : "+{value}% XP");
        setMessages(root.node("abilities", "sword_master"),
                chinese ? "武器大师" : "Weapon Master",
                chinese ? "剑与弓造成的伤害提高 {value}%。"
                        : "Deal {value}% more damage with swords and bows.",
                chinese ? "+{value}% 剑与弓伤害" : "+{value}% Sword and Bow Damage");
        setMessages(root.node("abilities", "first_strike"),
                chinese ? "破阵" : "Break Formation",
                chinese ? "近战首次攻击额外造成 {value}% 伤害；远程攻击有 {ranged_value}% 几率额外获得 1 级穿透。"
                        : "Melee first hits deal {value}% more damage. Ranged attacks have a {ranged_value}% chance to gain one piercing level.",
                chinese ? "首击强化 / 远程穿透" : "First Hit / Ranged Piercing");
        setMessages(root.node("abilities", "bleed"),
                chinese ? "压制" : "Suppression",
                chinese ? "近战有 {value}% 几率使敌人流血 {base_ticks} 次，每次造成 {value_2} 点伤害；远程有 {ranged_value}% 几率使敌人减速 {reduction_percent}%。"
                        : "Melee attacks have a {value}% chance to bleed enemies for {base_ticks} ticks at {value_2} damage per tick. Ranged attacks have a {ranged_value}% chance to slow enemies by {reduction_percent}%.",
                chinese ? "近战流血 / 远程眩晕" : "Melee Bleed / Ranged Stun");

        FileUtil.saveYamlFile(target, root);
    }

    private void setMessages(ConfigurationNode node, String name, String description, String info) throws Exception {
        node.node("name").set(name);
        node.node("desc").set(description);
        if (info != null) node.node("info").set(info);
    }

    private File file(String path) {
        return new File(plugin.getPluginFolder(), path);
    }
}
