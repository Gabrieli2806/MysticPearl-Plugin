package com.g2806.EnderMen;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Manages all configuration settings for the MysticPearl plugin
 */
public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    /**
     * Load or create the configuration file
     */
    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");

        if (!configFile.exists()) {
            plugin.getLogger().info("Creating new config.yml...");
            createDefaultConfig();
        }

        config = YamlConfiguration.loadConfiguration(configFile);
        plugin.getLogger().info("Configuration loaded successfully!");
    }

    /**
     * Create default configuration file
     */
    private void createDefaultConfig() {
        try {
            plugin.saveResource("config.yml", false);
        } catch (IllegalArgumentException e) {
            // File doesn't exist in resources, create manually
            createManualDefaultConfig();
        }
    }

    /**
     * Create manual default configuration if resource doesn't exist
     */
    private void createManualDefaultConfig() {
        config = new YamlConfiguration();

        // Item Names and Display
        config.set("items.mystic-pearl.name", "§5Mistic Enderpearl");
        config.set("items.mystic-pearl.lore", Arrays.asList(
                "§7A mystical pearl imbued with End magic",
                "§7Teleports its wielder, but fears water"
        ));

        config.set("items.water-resistance-potion.name", "§bPotion of Water Resistance");
        config.set("items.water-resistance-potion.lore", Arrays.asList(
                "§7Grants water resistance for 1 hour"
        ));

        config.set("items.elixir.name", "§dElixir of Eternal Water Resistance");
        config.set("items.elixir.lore", Arrays.asList(
                "§7Grants permanent water resistance"
        ));

        // Gameplay Settings
        config.set("gameplay.pearl-spawn-chance-min", 0.05);
        config.set("gameplay.pearl-spawn-chance-max", 0.15);
        config.set("gameplay.pearl-teleport-cooldown-ticks", 20); // 1 second
        config.set("gameplay.water-resistance-potion-duration-ticks", 72000); // 1 hour
        config.set("gameplay.friendly-enderman-detection-range", 16);
        config.set("gameplay.enderman-behavior-task-interval-ticks", 40); // 2 seconds
        config.set("gameplay.pumpkin-invisibility-task-interval-ticks", 20); // 1 second
        config.set("gameplay.friendly-enderman-defense-chance", 0.5);
        config.set("gameplay.enderman-flower-pickup-chance", 0.2);
        config.set("gameplay.enderman-flower-chance", 0.75);

        // Pickable Blocks for Endermen
        config.set("gameplay.pickable-blocks", Arrays.asList(
                "DIRT", "GRASS_BLOCK", "SAND", "POPPY", "DANDELION"
        ));

        // Messages
        config.set("messages.potion-consumed", "§5You feel resistant to water!");
        config.set("messages.potion-expired", "§5Your water resistance has worn off.");
        config.set("messages.elixir-consumed", "§5You are now permanently resistant to water!");
        config.set("messages.pearl-received", "§5¡Recibiste una Mistic Enderpearl!");
        config.set("messages.no-permission", "§cYou must be an operator to use this command.");
        config.set("messages.player-only", "§cThis command can only be used by players.");

        // Debug Settings
        config.set("debug.enabled", false);
        config.set("debug.log-events", false);

        // Save the config
        saveConfig();
        plugin.getLogger().info("Default configuration created!");
    }

    /**
     * Save the current configuration
     */
    public void saveConfig() {
        try {
            File configFile = new File(plugin.getDataFolder(), "config.yml");
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save config.yml: " + e.getMessage());
        }
    }

    /**
     * Reload the configuration from disk
     */
    public void reloadConfig() {
        loadConfig();
    }

    // Item Names
    public String getMysticPearlName() {
        return config.getString("items.mystic-pearl.name", "§5Mistic Enderpearl");
    }

    public List<String> getMysticPearlLore() {
        return config.getStringList("items.mystic-pearl.lore");
    }

    public String getWaterResistancePotionName() {
        return config.getString("items.water-resistance-potion.name", "§bPotion of Water Resistance");
    }

    public List<String> getWaterResistancePotionLore() {
        return config.getStringList("items.water-resistance-potion.lore");
    }

    public String getElixirName() {
        return config.getString("items.elixir.name", "§dElixir of Eternal Water Resistance");
    }

    public List<String> getElixirLore() {
        return config.getStringList("items.elixir.lore");
    }

    // Gameplay Settings
    public double getPearlSpawnChanceMin() {
        return config.getDouble("gameplay.pearl-spawn-chance-min", 0.05);
    }

    public double getPearlSpawnChanceMax() {
        return config.getDouble("gameplay.pearl-spawn-chance-max", 0.15);
    }

    public int getPearlTeleportCooldownTicks() {
        return config.getInt("gameplay.pearl-teleport-cooldown-ticks", 20);
    }

    public long getWaterResistancePotionDurationTicks() {
        return config.getLong("gameplay.water-resistance-potion-duration-ticks", 72000L);
    }

    public int getFriendlyEndermanDetectionRange() {
        return config.getInt("gameplay.friendly-enderman-detection-range", 16);
    }

    public long getEndermanBehaviorTaskIntervalTicks() {
        return config.getLong("gameplay.enderman-behavior-task-interval-ticks", 40L);
    }

    public long getPumpkinInvisibilityTaskIntervalTicks() {
        return config.getLong("gameplay.pumpkin-invisibility-task-interval-ticks", 20L);
    }

    public double getFriendlyEndermanDefenseChance() {
        return config.getDouble("gameplay.friendly-enderman-defense-chance", 0.5);
    }

    public double getEndermanFlowerPickupChance() {
        return config.getDouble("gameplay.enderman-flower-pickup-chance", 0.2);
    }

    public double getEndermanFlowerChance() {
        return config.getDouble("gameplay.enderman-flower-chance", 0.75);
    }

    public List<String> getPickableBlocks() {
        return config.getStringList("gameplay.pickable-blocks");
    }

    // Messages
    public String getMessagePotionConsumed() {
        return config.getString("messages.potion-consumed", "§5You feel resistant to water!");
    }

    public String getMessagePotionExpired() {
        return config.getString("messages.potion-expired", "§5Your water resistance has worn off.");
    }

    public String getMessageElixirConsumed() {
        return config.getString("messages.elixir-consumed", "§5You are now permanently resistant to water!");
    }

    public String getMessagePearlReceived() {
        return config.getString("messages.pearl-received", "§5¡Recibiste una Mistic Enderpearl!");
    }

    public String getMessageNoPermission() {
        return config.getString("messages.no-permission", "§cYou must be an operator to use this command.");
    }

    public String getMessagePlayerOnly() {
        return config.getString("messages.player-only", "§cThis command can only be used by players.");
    }

    // Debug Settings
    public boolean isDebugEnabled() {
        return config.getBoolean("debug.enabled", false);
    }

    public boolean shouldLogEvents() {
        return config.getBoolean("debug.log-events", false);
    }

    // Pearl Interactions Settings
    public boolean isDoubleCrouchTeleportEnabled() {
        return config.getBoolean("gameplay.pearl-interactions.enable-double-crouch-teleport", true);
    }

    public long getDoubleCrouchWindowMs() {
        return config.getLong("gameplay.pearl-interactions.double-crouch-window-ms", 500L);
    }

    public int getRandomTeleportCooldownTicks() {
        return config.getInt("gameplay.pearl-interactions.random-teleport-cooldown-ticks", 60);
    }

    public int getRandomTeleportMaxDistance() {
        return config.getInt("gameplay.pearl-interactions.random-teleport-max-distance", 32);
    }

    public int getRandomTeleportMinDistance() {
        return config.getInt("gameplay.pearl-interactions.random-teleport-min-distance", 5);
    }

    // Additional Messages
    public String getMessageRandomTeleportCooldown() {
        return config.getString("messages.random-teleport-cooldown", "Random teleport is on cooldown!");
    }
}
