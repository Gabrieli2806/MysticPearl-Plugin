package com.g2806.EnderMen;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * Manages creation and identification of custom potions
 */
public class PotionManager {
    private final JavaPlugin plugin;
    private final ConfigManager configManager;

    public PotionManager(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    /**
     * Create the Water Resistance Potion (Rare - Blue name)
     */
    public ItemStack createWaterResistancePotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();

        meta.setDisplayName(configManager.getWaterResistancePotionName());
        meta.setLore(configManager.getWaterResistancePotionLore());
        meta.setColor(Color.PURPLE);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        potion.setItemMeta(meta);
        return potion;
    }

    /**
     * Create the Eternal Water Resistance Elixir (Epic - Purple name)
     */
    public ItemStack createEternalWaterResistanceElixir() {
        ItemStack elixir = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) elixir.getItemMeta();

        meta.setDisplayName(configManager.getElixirName());
        meta.setLore(configManager.getElixirLore());
        meta.setColor(Color.PURPLE);
        meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ADDITIONAL_TOOLTIP);

        elixir.setItemMeta(meta);
        return elixir;
    }

    /**
     * Check if an item is the Water Resistance Potion
     */
    public boolean isWaterResistancePotion(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(configManager.getWaterResistancePotionName());
    }

    /**
     * Check if an item is the Eternal Water Resistance Elixir
     */
    public boolean isEternalWaterResistanceElixir(ItemStack item) {
        if (item == null || item.getType() != Material.POTION || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.hasDisplayName()
                && meta.getDisplayName().equals(configManager.getElixirName());
    }

    /**
     * Check if an item is any custom potion
     */
    public boolean isCustomPotion(ItemStack item) {
        return isWaterResistancePotion(item) || isEternalWaterResistanceElixir(item);
    }
}
