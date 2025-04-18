package com.g2806.EnderMen;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Chest;
import org.bukkit.NamespacedKey;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class MysticEnderPlugin extends JavaPlugin implements Listener {

    private static final String MYSTIC_PEARL_NAME = "§5Mistic Enderpearl";
    private static final String MYSTIC_PEARL_METADATA = "MysticEnderPearl";
    private static final String POTION_NAME = "§bPotion of Water Resistance"; // Blue for Rare
    private static final String ELIXIR_NAME = "§dElixir of Eternal Water Resistance"; // Purple for Epic
    private static final String PERMANENT_WATER_RESISTANCE = "PermanentWaterResistance";
    private static final List<Material> PICKABLE_BLOCKS = Arrays.asList(
            Material.DIRT, Material.GRASS_BLOCK, Material.SAND, Material.POPPY, Material.DANDELION
    );
    private final Random random = new Random();

    @Override
    public void onEnable() {
        // Registrar los eventos
        getServer().getPluginManager().registerEvents(this, this);
        // Registrar el comando
        getCommand("mysticpearl").setExecutor(new MysticPearlCommand());
        // Registrar recetas
        registerPotionRecipe();
        registerElixirRecipe();
        // Iniciar tareas
        startEndermanBehaviorTask();
        startInvisibilityTask();
        getLogger().info("FriendlyEnder habilitado!");
    }

    @Override
    public void onDisable() {
        getLogger().info("FriendlyEnder deshabilitado!");
    }

    // Crear la Mistic Enderpearl
    private ItemStack createMysticEnderpearl() {
        ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = pearl.getItemMeta();
        meta.setDisplayName(MYSTIC_PEARL_NAME);
        meta.setLore(Arrays.asList(
                "§7A mystical pearl imbued with End magic",
                "§7Teleports its wielder, but fears water"
        ));
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        pearl.setItemMeta(meta);
        return pearl;
    }

    // Crear la poción de resistencia al agua (Rare)
    private ItemStack createWaterResistancePotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.setDisplayName(POTION_NAME);
        meta.setLore(Arrays.asList("§7Grants water resistance for 1 hour"));
        meta.setColor(Color.PURPLE); // Botella morada
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true); // Glint
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        potion.setItemMeta(meta);
        return potion;
    }

    // Crear el elixir de resistencia eterna al agua (Epic)
    private ItemStack createEternalWaterResistanceElixir() {
        ItemStack elixir = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) elixir.getItemMeta();
        meta.setDisplayName(ELIXIR_NAME);
        meta.setLore(Arrays.asList("§7Grants permanent water resistance"));
        meta.setColor(Color.PURPLE); // Botella morada
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true); // Glint
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
        elixir.setItemMeta(meta);
        return elixir;
    }

    // Verificar si un ítem es la Mistic Enderpearl
    private boolean isMysticEnderpearl(ItemStack item) {
        return item != null && item.getType() == Material.ENDER_PEARL
                && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(MYSTIC_PEARL_NAME);
    }

    // Verificar si un ítem es la poción de resistencia al agua
    private boolean isWaterResistancePotion(ItemStack item) {
        return item != null && item.getType() == Material.POTION
                && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(POTION_NAME);
    }

    // Verificar si un ítem es el elixir de resistencia eterna
    private boolean isEternalWaterResistanceElixir(ItemStack item) {
        return item != null && item.getType() == Material.POTION
                && item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                && item.getItemMeta().getDisplayName().equals(ELIXIR_NAME);
    }

    // Verificar si un jugador tiene la perla (incluyendo dentro de bundles)
    private boolean hasMysticPearl(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if (isMysticEnderpearl(item)) {
                return true;
            }
            // Verificar dentro de bundles
            if (item.getType() == Material.BUNDLE && item.hasItemMeta() && item.getItemMeta() instanceof BundleMeta) {
                BundleMeta bundleMeta = (BundleMeta) item.getItemMeta();
                if (bundleMeta.hasItems()) {
                    for (ItemStack bundledItem : bundleMeta.getItems()) {
                        if (isMysticEnderpearl(bundledItem)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // Verificar si una entidad tiene una calabaza en la cabeza
    private boolean hasPumpkin(Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            ItemStack helmet = living.getEquipment().getItem(EquipmentSlot.HEAD);
            return helmet != null && helmet.getType() == Material.CARVED_PUMPKIN;
        }
        return false;
    }

    // Registrar receta de poción (crafting table)
    private void registerPotionRecipe() {
        ItemStack result = createWaterResistancePotion();
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(this, "water_resistance_potion"), result);
        recipe.addIngredient(Material.POTION);
        recipe.addIngredient(Material.CHORUS_FRUIT);
        getServer().addRecipe(recipe);
    }

    // Registrar receta de elixir (crafting table)
    private void registerElixirRecipe() {
        ItemStack result = createEternalWaterResistanceElixir();
        ShapelessRecipe recipe = new ShapelessRecipe(new NamespacedKey(this, "eternal_water_resistance_elixir"), result);
        recipe.addIngredient(Material.POTION);
        recipe.addIngredient(Material.ENCHANTED_GOLDEN_APPLE);
        getServer().addRecipe(recipe);
    }

    // Manejar consumo de poción o elixir
    @EventHandler
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isWaterResistancePotion(item)) {
            // Poción temporal (Rare)
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.LUCK,
                    72000, // 1 hora
                    0,
                    false, // Sin partículas
                    true,  // Mostrar en inventario
                    false  // Sin ícono en HUD
            ));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You feel resistant to water!");
            player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0);
            // Notificar cuando termine
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.hasPotionEffect(PotionEffectType.LUCK)) {
                        player.sendMessage(ChatColor.LIGHT_PURPLE + "Your water resistance has worn off.");
                    }
                }
            }.runTaskLater(this, 72001L);
        } else if (isEternalWaterResistanceElixir(item)) {
            // Elixir permanente (Epic)
            player.setMetadata(PERMANENT_WATER_RESISTANCE, new FixedMetadataValue(this, true));
            player.sendMessage(ChatColor.LIGHT_PURPLE + "You are now permanently resistant to water!");
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0);
            player.getWorld().spawnParticle(Particle.SPLASH, player.getLocation().add(0, 1, 0), 20, 0.5, 0.5, 0.5, 0);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }
    }

    // Impedir que la Mistic Enderpearl se coloque en un bundle
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (event.getRecipe() != null && event.getRecipe().getResult().getType() == Material.BUNDLE) {
            for (ItemStack item : event.getInventory().getMatrix()) {
                if (item != null && isMysticEnderpearl(item)) {
                    event.getInventory().setResult(null);
                    return;
                }
            }
        }
    }

    // Hacer que los Enderman no ataquen a jugadores con la perla o con calabaza
    @EventHandler
    public void onEndermanTarget(EntityTargetEvent event) {
        if (event.getEntity() instanceof Enderman && event.getTarget() instanceof LivingEntity target) {
            // Ignorar si el objetivo tiene una calabaza
            if (hasPumpkin(target)) {
                event.setCancelled(true);
                return;
            }
            // Ignorar si un jugador cercano tiene la perla
            if (target instanceof Player player && hasMysticPearl(player)) {
                event.setCancelled(true);
                return;
            }
            // Verificar jugadores cercanos con la perla
            for (Player nearby : event.getEntity().getWorld().getPlayers()) {
                if (hasMysticPearl(nearby) && nearby.getLocation().distance(event.getEntity().getLocation()) <= 16) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    // Teletransporte con la Mistic Enderpearl (con cooldown predeterminado)
    @EventHandler
    public void onPlayerUsePearl(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND && event.getHand() != EquipmentSlot.OFF_HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (isMysticEnderpearl(item)) {
            // Verificar cooldown
            if (!player.hasCooldown(Material.ENDER_PEARL)) {
                EnderPearl pearl = (EnderPearl) player.getWorld().spawnEntity(
                        player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.5)),
                        EntityType.ENDER_PEARL
                );
                pearl.setShooter(player);
                pearl.setVelocity(player.getLocation().getDirection().multiply(1.5));
                pearl.setMetadata(MYSTIC_PEARL_METADATA, new FixedMetadataValue(this, true));
                // Aplicar cooldown predeterminado (20 ticks = 1 segundo)
                player.setCooldown(Material.ENDER_PEARL, 20);
                event.setCancelled(true);
            }
        }
    }

    // Añadir Mistic Enderpearl a cofres de botín
    @EventHandler
    public void onLootGenerate(LootGenerateEvent event) {
        if (event.getInventoryHolder() instanceof Chest) {
            // Probabilidad entre 5% y 15%
            double chance = random.nextDouble() * (0.15 - 0.05) + 0.05;
            if (random.nextDouble() < chance) {
                ItemStack mysticPearl = createMysticEnderpearl();
                event.getLoot().add(mysticPearl);
            }
        }
    }

    // Cancelar daño por teletransporte y manejar defensa
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        // Cancelar daño por perla
        if (event.getEntity() instanceof Player && event.getDamager() instanceof EnderPearl) {
            EnderPearl pearl = (EnderPearl) event.getDamager();
            if (pearl.hasMetadata(MYSTIC_PEARL_METADATA)) {
                event.setCancelled(true);
            }
        }
        // Defensa de Enderman amistoso
        if (event.getEntity() instanceof Player player && hasMysticPearl(player)) {
            LivingEntity attacker = null;
            if (event.getDamager() instanceof LivingEntity && !(event.getDamager() instanceof Enderman)) {
                attacker = (LivingEntity) event.getDamager();
            }
            else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity shooter) {
                if (!(shooter instanceof Enderman)) {
                    attacker = (LivingEntity) shooter;
                }
            }
            if (attacker != null) {
                if (random.nextDouble() < 0.5) {
                    for (Enderman enderman : player.getWorld().getEntitiesByClass(Enderman.class)) {
                        if (enderman.getLocation().distance(player.getLocation()) <= 16) {
                            enderman.setTarget(attacker);
                        }
                    }
                }
            }
        }
    }

    // Aplicar daño por agua y lluvia con excepciones
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!hasMysticPearl(player)) return;

        // Verificar excepciones
        boolean hasConduitPower = player.hasPotionEffect(PotionEffectType.CONDUIT_POWER);
        boolean hasTemporaryWaterResistance = player.hasPotionEffect(PotionEffectType.LUCK);
        boolean hasPermanentWaterResistance = player.hasMetadata(PERMANENT_WATER_RESISTANCE);

        if (hasConduitPower || hasTemporaryWaterResistance || hasPermanentWaterResistance) {
            return; // No daño
        }

        // Daño por agua
        if (player.getLocation().getBlock().getType().name().contains("WATER")) {
            player.damage(1.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
        }
        // Daño por lluvia
        else if (player.getWorld().hasStorm() && player.getWorld().getHighestBlockYAt(player.getLocation()) <= player.getLocation().getBlockY()) {
            player.damage(1.0);
            player.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 100, 1));
        }
    }

    // Prevenir que los Enderman amistosos se teletransporten
    @EventHandler
    public void onEntityTeleport(EntityTeleportEvent event) {
        if (event.getEntity() instanceof Enderman enderman) {
            for (Player player : enderman.getWorld().getPlayers()) {
                if (hasMysticPearl(player) && enderman.getLocation().distance(player.getLocation()) <= 16) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    // Tarea para comportamientos de Enderman (flores y bloques)
    private void startEndermanBehaviorTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : getServer().getOnlinePlayers()) {
                    if (!hasMysticPearl(player)) continue;
                    for (Enderman enderman : player.getWorld().getEntitiesByClass(Enderman.class)) {
                        if (enderman.getLocation().distance(player.getLocation()) <= 16) {
                            if (random.nextDouble() < 0.2) { // 20% cada 2 segundos
                                if (random.nextDouble() < 0.75) { // 75% flores
                                    enderman.setCarriedBlock(null);
                                    ItemStack poppy = new ItemStack(Material.POPPY);
                                    enderman.getEquipment().setItemInMainHand(poppy);
                                } else {
                                    enderman.getEquipment().setItemInMainHand(null);
                                    Material block = PICKABLE_BLOCKS.get(random.nextInt(PICKABLE_BLOCKS.size()));
                                    enderman.setCarriedBlock(block.createBlockData());
                                }
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 40L); // Cada 2 segundos
    }

    // Tarea para manejar la invisibilidad de jugadores/entidades con calabaza
    private void startInvisibilityTask() {
        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player pearlHolder : getServer().getOnlinePlayers()) {
                    if (!hasMysticPearl(pearlHolder)) {
                        // Mostrar a todos si no tiene la perla
                        for (Player other : getServer().getOnlinePlayers()) {
                            pearlHolder.showPlayer(MysticEnderPlugin.this, other);
                        }
                        for (Entity entity : pearlHolder.getWorld().getEntities()) {
                            pearlHolder.showEntity(MysticEnderPlugin.this, entity);
                        }
                        continue;
                    }
                    // Ocultar jugadores y entidades con calabaza en un radio de 16 bloques
                    for (Entity entity : pearlHolder.getNearbyEntities(16, 16, 16)) {
                        if (hasPumpkin(entity)) {
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
                    // También verificar jugadores en el mismo mundo
                    for (Player target : pearlHolder.getWorld().getPlayers()) {
                        if (target != pearlHolder && pearlHolder.getLocation().distance(target.getLocation()) <= 16) {
                            if (hasPumpkin(target)) {
                                pearlHolder.hidePlayer(MysticEnderPlugin.this, target);
                            } else {
                                pearlHolder.showPlayer(MysticEnderPlugin.this, target);
                            }
                        }
                    }
                }
            }
        }.runTaskTimer(this, 0L, 20L); // Cada segundo
    }

    // Clase interna para manejar el comando /mysticpearl (solo OPs)
    private class MysticPearlCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Este comando solo puede ser usado por jugadores.");
                return true;
            }

            Player player = (Player) sender;
            if (!player.isOp()) {
                player.sendMessage(ChatColor.RED + "You must be an operator to use this command.");
                return true;
            }

            player.getInventory().addItem(createMysticEnderpearl());
            player.sendMessage(ChatColor.LIGHT_PURPLE + "¡Recibiste una Mistic Enderpearl!");
            return true;
        }
    }
}