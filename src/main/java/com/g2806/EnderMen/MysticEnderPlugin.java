package com.g2806.EnderMen;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.block.Block;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Random;

/**
 * Main plugin class for MysticEnder-Plugin
 * Handles Enderman interactions and special items
 */
public class MysticEnderPlugin extends JavaPlugin implements Listener {

    // Managers
    private ConfigManager configManager;
    private PlayerDataManager playerDataManager;
    private PotionManager potionManager;
    private MysticPearlManager pearlManager;
    private PearlInteractionManager pearlInteractionManager;

    // Constants
    private static final String MYSTIC_PEARL_METADATA = "MysticEnderPearl";
    private static final Random RANDOM = new Random();

    @Override
    public void onEnable() {
        try {
            // Initialize managers
            configManager = new ConfigManager(this);
            playerDataManager = new PlayerDataManager(this);
            potionManager = new PotionManager(this, configManager);
            pearlManager = new MysticPearlManager(this, configManager);
            pearlInteractionManager = new PearlInteractionManager(this, configManager);

            // Register events
            getServer().getPluginManager().registerEvents(this, this);

            // Register command
            getCommand("mysticpearl").setExecutor(new MysticPearlCommand(this));

            // Register recipes
            registerPotionRecipe();
            registerElixirRecipe();

            // Start scheduled tasks
            startEndermanBehaviorTask();
            startInvisibilityTask();

            getLogger().info("=== MysticPearl-Plugin v1.0 Enabled ===");
            getLogger().info("✓ Configuration loaded");
            getLogger().info("✓ Player data system initialized");
            getLogger().info("✓ Commands registered");
            getLogger().info("✓ Events registered");

        } catch (Exception e) {
            getLogger().severe("Failed to enable plugin: " + e.getMessage());
            e.printStackTrace();
            setEnabled(false);
        }
    }

    @Override
    public void onDisable() {
        // Save all online players' data on shutdown
        for (Player player : getServer().getOnlinePlayers()) {
            try {
                playerDataManager.savePlayerEffects(player);
            } catch (Exception e) {
                getLogger().warning("Failed to save data for " + player.getName() + ": " + e.getMessage());
            }
        }

        getLogger().info("=== MysticPearl-Plugin Disabled ===");
    }

    /**
     * Register the Water Resistance Potion recipe
     */
    private void registerPotionRecipe() {
        try {
            ItemStack result = potionManager.createWaterResistancePotion();
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "water_resistance_potion"), result);
            recipe.addIngredient(Material.POTION);
            recipe.addIngredient(Material.CHORUS_FRUIT);
            getServer().addRecipe(recipe);
            getLogger().fine("✓ Water Resistance Potion recipe registered");
        } catch (Exception e) {
            getLogger().warning("Failed to register Water Resistance Potion recipe: " + e.getMessage());
        }
    }

    /**
     * Register the Eternal Water Resistance Elixir recipe
     */
    private void registerElixirRecipe() {
        try {
            ItemStack result = potionManager.createEternalWaterResistanceElixir();
            ShapelessRecipe recipe = new ShapelessRecipe(
                    new NamespacedKey(this, "eternal_water_resistance_elixir"), result);
            recipe.addIngredient(Material.POTION);
            recipe.addIngredient(Material.ENCHANTED_GOLDEN_APPLE);
            getServer().addRecipe(recipe);
            getLogger().fine("✓ Eternal Water Resistance Elixir recipe registered");
        } catch (Exception e) {
            getLogger().warning("Failed to register Elixir recipe: " + e.getMessage());
        }
    }

    /**
     * Handle player join - load their saved effects
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            // Load player effects from file
            playerDataManager.loadPlayerEffects(player);
            getLogger().fine("Loaded effects for " + player.getName());
        } catch (Exception e) {
            getLogger().warning("Failed to load effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Handle player quit - save their current effects
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        try {
            // Save player effects to file
            playerDataManager.savePlayerEffects(player);
            getLogger().fine("Saved effects for " + player.getName());
        } catch (Exception e) {
            getLogger().warning("Failed to save effects for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Handle double crouch teleportation with pearl
     */
    @EventHandler
    public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
        try {
            Player player = event.getPlayer();
            ItemStack mainHandItem = player.getInventory().getItemInMainHand();
            ItemStack offHandItem = player.getInventory().getItemInOffHand();

            // Check if player is holding pearl in EITHER HAND and is crouching
            boolean hasPearlInMainHand = pearlManager.isMysticEnderpearl(mainHandItem);
            boolean hasPearlInOffHand = pearlManager.isMysticEnderpearl(offHandItem);

            if (player.isSneaking() && (hasPearlInMainHand || hasPearlInOffHand)) {
                // Check for double crouch
                if (pearlInteractionManager.handleCrouch(player)) {
                    // Prevent throwing pearl when crouching
                    event.setCancelled(true);

                    // Perform random teleport
                    pearlInteractionManager.performRandomTeleport(player);
                }
            }

        } catch (Exception e) {
            getLogger().warning("Error in toggle sneak event: " + e.getMessage());
        }
    }

    /**
     * Handle pearl interaction with utility blocks
     */
    @EventHandler
    public void onPlayerInteractBlock(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();
            Block block = event.getClickedBlock();

            // Check if player has pearl in hand
            if (!pearlManager.hasMysticPearl(player) || block == null) {
                return;
            }

            // Get the block type
            String blockType = block.getType().name();

            // Check for interactive/utility blocks
            boolean isDoor = blockType.contains("DOOR") || blockType.contains("GATE");
            boolean isChest = blockType.contains("CHEST") || blockType.contains("BARREL") || blockType.contains("SHULKER");
            boolean isButton = blockType.contains("BUTTON") || blockType.contains("LEVER");
            boolean isTrapdoor = blockType.contains("TRAPDOOR");
            boolean isCraftingTable = blockType.equals("CRAFTING_TABLE") || blockType.equals("WORKBENCH");
            boolean isSmithingTable = blockType.equals("SMITHING_TABLE");
            boolean isBrewingStand = blockType.equals("BREWING_STAND");
            boolean isHopper = blockType.equals("HOPPER");
            boolean isFurnace = blockType.contains("FURNACE");
            boolean isDispenser = blockType.equals("DISPENSER");
            boolean isDropper = blockType.equals("DROPPER");
            boolean isEnchantingTable = blockType.equals("ENCHANTING_TABLE");
            boolean isAnvil = blockType.contains("ANVIL");
            boolean isLoom = blockType.equals("LOOM");
            boolean isCartographyTable = blockType.equals("CARTOGRAPHY_TABLE");
            boolean isStonecutter = blockType.equals("STONECUTTER");
            boolean isBeacon = blockType.equals("BEACON");
            boolean isBed = blockType.contains("BED");

            // Allow interaction with these blocks
            if (isDoor || isChest || isButton || isTrapdoor || isCraftingTable || isSmithingTable ||
                isBrewingStand || isHopper || isFurnace || isDispenser || isDropper || isEnchantingTable ||
                isAnvil || isLoom || isCartographyTable || isStonecutter || isBeacon || isBed) {
                // Don't throw pearl, allow normal block interaction
                event.setCancelled(false);

                if (getLogger().isLoggable(java.util.logging.Level.FINE)) {
                    getLogger().fine(player.getName() + " interacted with " + blockType + " while holding pearl");
                }
            }

        } catch (Exception e) {
            getLogger().warning("Error in player interact block event: " + e.getMessage());
        }
    }

    /**
     * Clean up player data on logout
     */
    @EventHandler
    public void onPlayerQuitCleanup(PlayerQuitEvent event) {
        try {
            pearlInteractionManager.clearPlayerData(event.getPlayer().getUniqueId());
        } catch (Exception e) {
            getLogger().warning("Error cleaning up player interaction data: " + e.getMessage());
        }
    }

    /**
     * Handle potion and elixir consumption
     */
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        try {
            if (potionManager.isWaterResistancePotion(item)) {
                // Apply temporary water resistance
                long duration = configManager.getWaterResistancePotionDurationTicks();
                player.addPotionEffect(new PotionEffect(
                        PotionEffectType.LUCK,
                        (int) duration,
                        0,
                        false,
                        true,
                        false
                ));

                player.sendMessage(ChatColor.LIGHT_PURPLE + configManager.getMessagePotionConsumed());
                player.getWorld().spawnParticle(Particle.SPLASH,
                        player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);

                // Schedule expiration message
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (!player.hasPotionEffect(PotionEffectType.LUCK)) {
                            player.sendMessage(ChatColor.LIGHT_PURPLE + configManager.getMessagePotionExpired());
                        }
                    }
                }.runTaskLater(this, duration + 1);

            } else if (potionManager.isEternalWaterResistanceElixir(item)) {
                // Apply permanent water resistance
                playerDataManager.setPermanentWaterResistance(player, true);
                player.setMetadata("PermanentWaterResistance", new FixedMetadataValue(this, true));

                player.sendMessage(ChatColor.LIGHT_PURPLE + configManager.getMessageElixirConsumed());
                player.getWorld().spawnParticle(Particle.ENCHANT,
                        player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
                player.getWorld().spawnParticle(Particle.SPLASH,
                        player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            }

        } catch (Exception e) {
            getLogger().warning("Error handling potion consumption for " + player.getName() + ": " + e.getMessage());
        }
    }

    /**
     * Prevent Mistic Enderpearl from being placed in bundles
     */
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        try {
            if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.BUNDLE) {
                for (ItemStack item : event.getInventory().getMatrix()) {
                    if (item != null && pearlManager.isMysticEnderpearl(item)) {
                        event.getInventory().setResult(null);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error in craft event: " + e.getMessage());
        }
    }

    /**
     * Prevent Endermen from targeting pearl holders or pumpkin wearers
     */
    @EventHandler
    public void onEndermanTarget(EntityTargetEvent event) {
        try {
            if (event.getEntity() instanceof Enderman && event.getTarget() instanceof LivingEntity target) {
                // Check if target has pumpkin
                if (pearlManager.hasPumpkin(target)) {
                    event.setCancelled(true);
                    return;
                }

                // Check if player target has pearl
                if (target instanceof Player player && pearlManager.hasMysticPearl(player)) {
                    event.setCancelled(true);
                    return;
                }

                // Check nearby players with pearls
                int range = pearlManager.getDetectionRange();
                for (Player nearby : event.getEntity().getWorld().getPlayers()) {
                    if (pearlManager.hasMysticPearl(nearby)
                            && nearby.getLocation().distance(event.getEntity().getLocation()) <= range) {
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error in entity target event: " + e.getMessage());
        }
    }

    /**
     * Handle pearl teleportation with cooldown
     */
    @EventHandler
    public void onPlayerUsePearl(PlayerInteractEvent event) {
        try {
            if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) return;
            if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

            Player player = event.getPlayer();
            ItemStack item = event.getItem();

            if (pearlManager.isMysticEnderpearl(item)) {
                // If clicking on a block with special properties, don't throw pearl
                Block block = event.getClickedBlock();
                if (block != null) {
                    String blockType = block.getType().name();

                    // Check for interactive/utility blocks
                    boolean isDoor = blockType.contains("DOOR") || blockType.contains("GATE");
                    boolean isChest = blockType.contains("CHEST") || blockType.contains("BARREL") || blockType.contains("SHULKER");
                    boolean isButton = blockType.contains("BUTTON") || blockType.contains("LEVER");
                    boolean isTrapdoor = blockType.contains("TRAPDOOR");
                    boolean isCraftingTable = blockType.equals("CRAFTING_TABLE") || blockType.equals("WORKBENCH");
                    boolean isSmithingTable = blockType.equals("SMITHING_TABLE");
                    boolean isBrewingStand = blockType.equals("BREWING_STAND");
                    boolean isHopper = blockType.equals("HOPPER");
                    boolean isFurnace = blockType.contains("FURNACE");
                    boolean isDispenser = blockType.equals("DISPENSER");
                    boolean isDropper = blockType.equals("DROPPER");
                    boolean isEnchantingTable = blockType.equals("ENCHANTING_TABLE");
                    boolean isAnvil = blockType.contains("ANVIL");
                    boolean isLoom = blockType.equals("LOOM");
                    boolean isCartographyTable = blockType.equals("CARTOGRAPHY_TABLE");
                    boolean isStonecutter = blockType.equals("STONECUTTER");
                    boolean isBeacon = blockType.equals("BEACON");
                    boolean isBed = blockType.contains("BED");

                    // Allow interaction with these blocks, don't throw pearl
                    if (isDoor || isChest || isButton || isTrapdoor || isCraftingTable || isSmithingTable ||
                        isBrewingStand || isHopper || isFurnace || isDispenser || isDropper || isEnchantingTable ||
                        isAnvil || isLoom || isCartographyTable || isStonecutter || isBeacon || isBed) {
                        event.setCancelled(false);
                        return;
                    }
                }

                // Check cooldown for pearl throwing
                if (!player.hasCooldown(Material.ENDER_PEARL)) {
                    int cooldown = configManager.getPearlTeleportCooldownTicks();

                    EnderPearl pearl = (EnderPearl) player.getWorld().spawnEntity(
                            player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)),
                            EntityType.ENDER_PEARL
                    );

                    pearl.setShooter(player);
                    pearl.setVelocity(player.getLocation().getDirection().multiply(1.5));
                    pearl.setMetadata(MYSTIC_PEARL_METADATA, new FixedMetadataValue(this, true));

                    player.setCooldown(Material.ENDER_PEARL, cooldown);
                    event.setCancelled(true);
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error in player interact event: " + e.getMessage());
        }
    }

    /**
     * Add Mistic Enderpearl to loot chests
     */
    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        try {
            if (event.getInventoryHolder() instanceof Chest) {
                double minChance = configManager.getPearlSpawnChanceMin();
                double maxChance = configManager.getPearlSpawnChanceMax();
                double chance = RANDOM.nextDouble() * (maxChance - minChance) + minChance;

                if (RANDOM.nextDouble() < chance) {
                    ItemStack mysticPearl = pearlManager.createMysticEnderpearl();
                    event.getLoot().add(mysticPearl);
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error in loot generate event: " + e.getMessage());
        }
    }

    /**
     * Cancel pearl damage and handle Enderman defense
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        try {
            // Cancel damage from Mistic Enderpearl
            if (event.getEntity() instanceof Player && event.getDamager() instanceof EnderPearl) {
                EnderPearl pearl = (EnderPearl) event.getDamager();
                if (pearl.hasMetadata(MYSTIC_PEARL_METADATA)) {
                    event.setCancelled(true);
                }
            }

            // Friendly Enderman defense
            if (event.getEntity() instanceof Player player && pearlManager.hasMysticPearl(player)) {
                LivingEntity attacker = null;

                if (event.getDamager() instanceof LivingEntity
                        && !(event.getDamager() instanceof Enderman)) {
                    attacker = (LivingEntity) event.getDamager();
                } else if (event.getDamager() instanceof Projectile projectile
                        && projectile.getShooter() instanceof LivingEntity shooter) {
                    if (!(shooter instanceof Enderman)) {
                        attacker = (LivingEntity) shooter;
                    }
                }

                if (attacker != null) {
                    double defenseChance = configManager.getFriendlyEndermanDefenseChance();
                    if (RANDOM.nextDouble() < defenseChance) {
                        int range = pearlManager.getDetectionRange();
                        for (Enderman enderman : player.getWorld().getEntitiesByClass(Enderman.class)) {
                            if (enderman.getLocation().distance(player.getLocation()) <= range) {
                                enderman.setTarget(attacker);
                            }
                        }
                    }
                }
            }

        } catch (Exception e) {
            getLogger().warning("Error in entity damage event: " + e.getMessage());
        }
    }

    /**
     * Apply water/rain damage to pearl holders without protection
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        try {
            Player player = event.getPlayer();
            if (!pearlManager.hasMysticPearl(player)) return;

            // Check protection exceptions
            boolean hasConduitPower = player.hasPotionEffect(PotionEffectType.CONDUIT_POWER);
            boolean hasTemporaryWaterResistance = player.hasPotionEffect(PotionEffectType.LUCK);
            boolean hasPermanentWaterResistance = player.hasMetadata("PermanentWaterResistance");

            if (hasConduitPower || hasTemporaryWaterResistance || hasPermanentWaterResistance) {
                return;
            }

            // Water damage
            if (player.getLocation().getBlock().getType().name().contains("WATER")) {
                player.damage(1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
            }
            // Rain damage
            else if (player.getWorld().hasStorm()
                    && player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY()) {
                player.damage(1.0);
                player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
            }

        } catch (Exception e) {
            getLogger().warning("Error in player move event: " + e.getMessage());
        }
    }

    /**
     * Prevent friendly Endermen from teleporting away from pearl holders
     */
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        try {
            if (event.getEntity() instanceof Enderman enderman) {
                int range = pearlManager.getDetectionRange();
                for (Player player : enderman.getWorld().getPlayers()) {
                    if (pearlManager.hasMysticPearl(player)
                            && enderman.getLocation().distance(player.getLocation()) <= range) {
                        event.setCancelled(true);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error in entity teleport event: " + e.getMessage());
        }
    }

    /**
     * Task for Enderman flower/block pickup behavior
     */
    private void startEndermanBehaviorTask() {
        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        for (Player player : getServer().getOnlinePlayers()) {
                            if (!pearlManager.hasMysticPearl(player)) continue;

                            int range = pearlManager.getDetectionRange();
                            for (Enderman enderman : player.getWorld().getEntitiesByClass(Enderman.class)) {
                                if (enderman.getLocation().distance(player.getLocation()) <= range) {
                                    double pickupChance = configManager.getEndermanFlowerPickupChance();
                                    if (RANDOM.nextDouble() < pickupChance) {
                                        handleEndermanCarryingBehavior(enderman);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        getLogger().warning("Error in enderman behavior task: " + e.getMessage());
                    }
                }
            }.runTaskTimer(this, 0L, configManager.getEndermanBehaviorTaskIntervalTicks());
            getLogger().fine("✓ Enderman behavior task started");
        } catch (Exception e) {
            getLogger().warning("Failed to start enderman behavior task: " + e.getMessage());
        }
    }

    /**
     * Handle Enderman carrying flowers or blocks
     */
    private void handleEndermanCarryingBehavior(Enderman enderman) {
        try {
            double flowerChance = configManager.getEndermanFlowerChance();
            if (RANDOM.nextDouble() < flowerChance) {
                // 75% chance to hold flower
                enderman.setCarriedBlock(null);
                ItemStack poppy = new ItemStack(Material.POPPY);
                enderman.getEquipment().setItemInMainHand(poppy);
            } else {
                // 25% chance to hold block
                enderman.getEquipment().setItemInMainHand(null);
                List<String> pickableBlocks = configManager.getPickableBlocks();
                if (!pickableBlocks.isEmpty()) {
                    String blockName = pickableBlocks.get(RANDOM.nextInt(pickableBlocks.size()));
                    try {
                        Material block = Material.valueOf(blockName);
                        enderman.setCarriedBlock(block.createBlockData());
                    } catch (IllegalArgumentException e) {
                        getLogger().warning("Invalid material in config: " + blockName);
                    }
                }
            }
        } catch (Exception e) {
            getLogger().warning("Error in enderman carrying behavior: " + e.getMessage());
        }
    }

    /**
     * Task for pumpkin invisibility management
     */
    private void startInvisibilityTask() {
        try {
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        for (Player pearlHolder : getServer().getOnlinePlayers()) {
                            if (!pearlManager.hasMysticPearl(pearlHolder)) {
                                // Show all if no pearl
                                for (Player other : getServer().getOnlinePlayers()) {
                                    pearlHolder.showPlayer(MysticEnderPlugin.this, other);
                                }
                                for (Entity entity : pearlHolder.getWorld().getEntities()) {
                                    pearlHolder.showEntity(MysticEnderPlugin.this, entity);
                                }
                                continue;
                            }

                            // Hide pumpkin wearers in range
                            int range = pearlManager.getDetectionRange();
                            for (Entity entity : pearlHolder.getNearbyEntities(range, range, range)) {
                                if (pearlManager.hasPumpkin(entity)) {
                                    if (entity instanceof Player target) {
                                        pearlHolder.hidePlayer(MysticEnderPlugin.this, target);
                                    } else {
                                        pearlHolder.hideEntity(MysticEnderPlugin.this, entity);
                                    }
                                } else {
                                    if (entity instanceof Player target) {
                                        pearlHolder.showPlayer(MysticEnderPlugin.this, target);
                                    } else {
                                        pearlHolder.showEntity(MysticEnderPlugin.this, entity);
                                    }
                                }
                            }

                            // Check all players in world
                            for (Player target : pearlHolder.getWorld().getPlayers()) {
                                if (target != pearlHolder
                                        && pearlHolder.getLocation().distance(target.getLocation()) <= range) {
                                    if (pearlManager.hasPumpkin(target)) {
                                        pearlHolder.hidePlayer(MysticEnderPlugin.this, target);
                                    } else {
                                        pearlHolder.showPlayer(MysticEnderPlugin.this, target);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        getLogger().warning("Error in invisibility task: " + e.getMessage());
                    }
                }
            }.runTaskTimer(this, 0L, configManager.getPumpkinInvisibilityTaskIntervalTicks());
            getLogger().fine("✓ Pumpkin invisibility task started");
        } catch (Exception e) {
            getLogger().warning("Failed to start invisibility task: " + e.getMessage());
        }
    }

    /**
     * Command executor for /mysticpearl
     */
    private class MysticPearlCommand implements CommandExecutor {
        private final MysticEnderPlugin plugin;

        public MysticPearlCommand(MysticEnderPlugin plugin) {
            this.plugin = plugin;
        }

        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            try {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(configManager.getMessagePlayerOnly());
                    return true;
                }

                Player player = (Player) sender;
                if (!player.isOp()) {
                    player.sendMessage(ChatColor.RED + configManager.getMessageNoPermission());
                    return true;
                }

                player.getInventory().addItem(pearlManager.createMysticEnderpearl());
                player.sendMessage(ChatColor.LIGHT_PURPLE + configManager.getMessagePearlReceived());
                return true;

            } catch (Exception e) {
                sender.sendMessage(ChatColor.RED + "An error occurred: " + e.getMessage());
                plugin.getLogger().warning("Error in mysticpearl command: " + e.getMessage());
                return false;
            }
        }
    }
}
