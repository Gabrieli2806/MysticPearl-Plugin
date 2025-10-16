package com.g2806.EnderMen;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * Manages player data persistence and effects
 */
public class PlayerDataManager {
    private final JavaPlugin plugin;
    private final File playersDataFolder;
    private static final String PERMANENT_WATER_RESISTANCE_KEY = "permanent-water-resistance";
    private static final String TEMPORARY_WATER_RESISTANCE_REMAINING_KEY = "temporary-water-resistance-remaining";

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.playersDataFolder = new File(plugin.getDataFolder(), "players_data");

        // Create the folder if it doesn't exist
        if (!playersDataFolder.exists()) {
            playersDataFolder.mkdirs();
            plugin.getLogger().info("Created players_data folder!");
        }
    }

    /**
     * Get the data file for a player
     */
    private File getPlayerDataFile(UUID playerUUID) {
        return new File(playersDataFolder, playerUUID + ".yml");
    }

    /**
     * Load a player's data from file
     */
    private FileConfiguration loadPlayerData(UUID playerUUID) {
        File playerFile = getPlayerDataFile(playerUUID);
        if (!playerFile.exists()) {
            return new YamlConfiguration();
        }
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    /**
     * Save a player's data to file
     */
    private void savePlayerData(UUID playerUUID, FileConfiguration data) {
        try {
            File playerFile = getPlayerDataFile(playerUUID);
            data.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save player data for " + playerUUID + ": " + e.getMessage());
        }
    }

    /**
     * Check if a player has permanent water resistance
     */
    public boolean hasPermanentWaterResistance(Player player) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration data = loadPlayerData(playerUUID);
        return data.getBoolean(PERMANENT_WATER_RESISTANCE_KEY, false);
    }

    /**
     * Set permanent water resistance for a player
     */
    public void setPermanentWaterResistance(Player player, boolean value) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration data = loadPlayerData(playerUUID);
        data.set(PERMANENT_WATER_RESISTANCE_KEY, value);
        savePlayerData(playerUUID, data);

        if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Set permanent water resistance for " + player.getName() + ": " + value);
        }
    }

    /**
     * Load all player effects from disk
     */
    public void loadPlayerEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration data = loadPlayerData(playerUUID);

        // Load permanent water resistance
        if (data.getBoolean(PERMANENT_WATER_RESISTANCE_KEY, false)) {
            player.setMetadata("PermanentWaterResistance",
                    new org.bukkit.metadata.FixedMetadataValue(plugin, true));

            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().fine("Restored permanent water resistance for " + player.getName());
            }
        }

        // Load temporary water resistance if time remaining
        long tempResistanceRemaining = data.getLong(TEMPORARY_WATER_RESISTANCE_REMAINING_KEY, 0);
        if (tempResistanceRemaining > 0) {
            // Apply the effect with remaining ticks
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.LUCK,
                    (int) Math.max(tempResistanceRemaining, 1),
                    0,
                    false,
                    true,
                    false
            ));

            if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                plugin.getLogger().fine("Restored temporary water resistance for " + player.getName()
                        + " with " + tempResistanceRemaining + " ticks remaining");
            }
        }
    }

    /**
     * Save all player effects to disk
     */
    public void savePlayerEffects(Player player) {
        UUID playerUUID = player.getUniqueId();
        FileConfiguration data = loadPlayerData(playerUUID);

        // Save permanent water resistance
        data.set(PERMANENT_WATER_RESISTANCE_KEY, player.hasMetadata("PermanentWaterResistance"));

        // Save temporary water resistance remaining time
        PotionEffect luckEffect = player.getPotionEffect(PotionEffectType.LUCK);
        if (luckEffect != null) {
            data.set(TEMPORARY_WATER_RESISTANCE_REMAINING_KEY, (long) luckEffect.getDuration());
        } else {
            data.set(TEMPORARY_WATER_RESISTANCE_REMAINING_KEY, 0L);
        }

        savePlayerData(playerUUID, data);

        if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
            plugin.getLogger().fine("Saved effects for " + player.getName());
        }
    }

    /**
     * Clear all data for a player (optional cleanup)
     */
    public void clearPlayerData(Player player) {
        File playerFile = getPlayerDataFile(player.getUniqueId());
        if (playerFile.exists()) {
            playerFile.delete();
            plugin.getLogger().info("Deleted data file for " + player.getName());
        }
    }

    /**
     * Get the players data folder
     */
    public File getPlayersDataFolder() {
        return playersDataFolder;
    }
}
