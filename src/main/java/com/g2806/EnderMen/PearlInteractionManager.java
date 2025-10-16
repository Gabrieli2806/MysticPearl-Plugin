package com.g2806.EnderMen;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages special interactions with the Mistic Enderpearl
 * Includes double-crouch teleportation and cooldown tracking
 */
public class PearlInteractionManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    // Track crouch state for each player
    private final Map<UUID, Long> lastCrouchTime = new HashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();

    public PearlInteractionManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Handle double crouch detection for random teleportation
     * Returns true if double crouch was detected
     */
    public boolean handleCrouch(Player player) {
        // Check if feature is enabled
        if (!configManager.isDoubleCrouchTeleportEnabled()) {
            return false;
        }

        UUID playerUUID = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        // Check if player is in cooldown
        if (isInTeleportCooldown(playerUUID)) {
            // Show cooldown as title (non-intrusive)
            int remainingTicks = getRemainingCooldown(playerUUID);
            int remainingSeconds = Math.max(1, (remainingTicks + 19) / 20); // Round up to nearest second
            player.sendTitle("", ChatColor.RED + "Cooldown: " + remainingSeconds + "s", 0, 20, 0);
            return false;
        }

        // Check if this is a double crouch
        if (lastCrouchTime.containsKey(playerUUID)) {
            long timeSinceLastCrouch = currentTime - lastCrouchTime.get(playerUUID);
            long doubleClickWindow = configManager.getDoubleCrouchWindowMs();

            // Double crouch detected!
            if (timeSinceLastCrouch < doubleClickWindow) {
                lastCrouchTime.remove(playerUUID);
                return true;
            }
        }

        // Store crouch time for next check
        lastCrouchTime.put(playerUUID, currentTime);
        return false;
    }

    /**
     * Perform random teleportation like a chorus fruit
     */
    public void performRandomTeleport(Player player) {
        try {
            Location currentLocation = player.getLocation();
            Location randomLocation = null;
            int attempts = 0;

            int minDistance = configManager.getRandomTeleportMinDistance();
            int maxDistance = configManager.getRandomTeleportMaxDistance();

            // Try to find a safe teleport location
            while (randomLocation == null && attempts < 10) {
                int distance = minDistance +
                               (int) (Math.random() * (maxDistance - minDistance));
                double angle = Math.random() * 2 * Math.PI;

                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;

                Location targetLocation = currentLocation.clone();
                targetLocation.add(offsetX, 0, offsetZ);

                // Try to find ground
                for (int y = targetLocation.getBlockY(); y >= targetLocation.getBlockY() - 20; y--) {
                    targetLocation.setY(y);
                    if (targetLocation.getBlock().getType().isSolid()) {
                        // Found ground, teleport above it
                        randomLocation = targetLocation.clone();
                        randomLocation.setY(y + 1);
                        randomLocation.setPitch(player.getLocation().getPitch());
                        randomLocation.setYaw(player.getLocation().getYaw());
                        break;
                    }
                }

                attempts++;
            }

            if (randomLocation != null) {
                player.teleport(randomLocation);
                player.setFallDistance(0); // Reset fall damage
                startTeleportCooldown(player);

                // Visual feedback
                player.getWorld().spawnParticle(
                    Particle.PORTAL,
                    currentLocation.add(0, 1, 0),
                    10, 0.5, 0.5, 0.5, 0
                );

                player.playSound(randomLocation, org.bukkit.Sound.ITEM_CHORUS_FRUIT_TELEPORT, 1.0f, 1.0f);

                if (plugin.getLogger().isLoggable(java.util.logging.Level.FINE)) {
                    plugin.getLogger().fine(player.getName() + " used random teleport with pearl");
                }
            }

        } catch (Exception e) {
            plugin.getLogger().warning("Error in random teleport: " + e.getMessage());
        }
    }

    /**
     * Start teleport cooldown for a player
     */
    private void startTeleportCooldown(Player player) {
        UUID playerUUID = player.getUniqueId();
        int cooldownTicks = configManager.getRandomTeleportCooldownTicks();
        long cooldownEndTime = System.currentTimeMillis() + (cooldownTicks * 50L);
        teleportCooldowns.put(playerUUID, cooldownEndTime);
    }

    /**
     * Check if player is in teleport cooldown
     */
    public boolean isInTeleportCooldown(UUID playerUUID) {
        if (!teleportCooldowns.containsKey(playerUUID)) {
            return false;
        }

        long endTime = teleportCooldowns.get(playerUUID);
        if (System.currentTimeMillis() >= endTime) {
            teleportCooldowns.remove(playerUUID);
            return false;
        }

        return true;
    }

    /**
     * Get remaining cooldown in ticks
     */
    public int getRemainingCooldown(UUID playerUUID) {
        if (!isInTeleportCooldown(playerUUID)) {
            return 0;
        }

        long endTime = teleportCooldowns.get(playerUUID);
        long remainingMs = endTime - System.currentTimeMillis();
        return (int) (remainingMs / 50); // Convert to ticks
    }

    /**
     * Clear crouch tracking for a player (when they logout)
     */
    public void clearPlayerData(UUID playerUUID) {
        lastCrouchTime.remove(playerUUID);
        teleportCooldowns.remove(playerUUID);
    }
}
