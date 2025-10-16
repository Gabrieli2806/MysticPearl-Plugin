package com.g2806.EnderMen;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Manages creation and identification of the Mistic Enderpearl
 */
public class MysticPearlManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public MysticPearlManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Create the Mistic Enderpearl item
     */
    public ItemStack createMysticEnderpearl() {
        ItemStack pearl = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = pearl.getItemMeta();

        meta.setDisplayName(configManager.getMysticPearlName());
        meta.setLore(configManager.getMysticPearlLore());
        meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        pearl.setItemMeta(meta);
        return pearl;
    }

    /**
     * Check if an item is the Mistic Enderpearl
     */
    public boolean isMysticEnderpearl(ItemStack item) {
        if (item == null || item.getType() != Material.ENDER_PEARL || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(configManager.getMysticPearlName());
    }

    /**
     * Check if a player has the Mistic Enderpearl (including in bundles)
     */
    public boolean hasMysticPearl(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            // Check main inventory
            if (isMysticEnderpearl(item)) {
                return true;
            }

            // Check inside bundles
            if (item.getType() == Material.BUNDLE && item.hasItemMeta()
                    && item.getItemMeta() instanceof BundleMeta) {
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

    /**
     * Check if an entity has a pumpkin on its head
     */
    public boolean hasPumpkin(org.bukkit.entity.Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            ItemStack helmet = living.getEquipment().getItem(EquipmentSlot.HEAD);
            return helmet != null && helmet.getType() == Material.CARVED_PUMPKIN;
        }
        return false;
    }

    /**
     * Get the detection range for friendly Endermen
     */
    public int getDetectionRange() {
        return configManager.getFriendlyEndermanDetectionRange();
    }
}
