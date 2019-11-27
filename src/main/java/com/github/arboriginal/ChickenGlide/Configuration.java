package com.github.arboriginal.ChickenGlide;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class Configuration {
    final Enchantment ench = Enchantment.VANISHING_CURSE;

    ItemStack ITEM;
    Material  ITEM_TYPE;

    int     DAMAGES_FREQ, MAX_DURATION;
    double  DAMAGES_AMOUNT, EE_CHANCE, VEL_MAX, VEL_MIN;
    String  HEAD_DNAME, HEAD_PNAME, MSG_EE_ALERT, MSG_EE_LOG, MSG_RELOADED;
    boolean DAMAGES, EE, EE_ALERT, EE_LOG, EJECT_STOP, GLIDE, GRASS,
            LAUNCH_CLICK, LAUNCH_SLOT, LEAVE, LOCK_HAND, NO_BABY, SNEAK_PLACE;

    Configuration(FileConfiguration config) {
        NO_BABY        = !config.getBoolean("limitations.baby_chicken");
        DAMAGES        = config.getBoolean("behaviors.takes_damages.enabled");
        DAMAGES_AMOUNT = config.getDouble("behaviors.takes_damages.amount");
        DAMAGES_FREQ   = config.getInt("behaviors.takes_damages.frequency");
        EE             = config.getBoolean("easteregg.enabled");
        EE_ALERT       = config.getBoolean("easteregg_alert_players");
        EE_CHANCE      = Math.max(0.01, Math.min(config.getDouble("easteregg.chance"), 100)) / 1000;
        EE_LOG         = config.getBoolean("easteregg.log_events");
        EJECT_STOP     = config.getBoolean("limitations.stop_on_eject");
        GLIDE          = config.getBoolean("behaviors.glide_when_carrying");
        HEAD_DNAME     = config.getString("chicken_head.display_name");
        HEAD_PNAME     = config.getString("chicken_head.player_name");
        GRASS          = !config.getBoolean("behaviors.ignore_grass");
        LAUNCH_CLICK   = config.getBoolean("behaviors.right_click_launch");
        LAUNCH_SLOT    = config.getBoolean("behaviors.change_slot_launch");
        LEAVE          = config.getBoolean("behaviors.leave_by_itself");
        LOCK_HAND      = config.getBoolean("limitations.lock_hand");
        MAX_DURATION   = config.getInt("limitations.max_duration");
        MSG_EE_ALERT   = config.getString("messages.easteregg_alert");
        MSG_EE_LOG     = config.getString("messages.easteregg_log");
        MSG_RELOADED   = config.getString("messages.config_reloaded");
        SNEAK_PLACE    = config.getBoolean("behaviors.place_when_sneaking");

        String[] vel = config.getString("behaviors.eject_velocity").split(";");
        double   min = velocity(vel[0]), max = (vel.length == 2) ? velocity(vel[1]) : min;

        VEL_MIN = Math.min(min, max);
        VEL_MAX = Math.max(min, max);

        ITEM_TYPE = Material.getMaterial(config.getString("chicken_head.head_item"));
        if (ITEM_TYPE == null) {
            Bukkit.getLogger().warning(config.getString("messages.wrong_item_type"));
            ITEM_TYPE = Material.PLAYER_HEAD;
        }

        ITEM = new ItemStack(ITEM_TYPE);
        ItemMeta meta = ITEM.getItemMeta();
        meta.setDisplayName(HEAD_DNAME);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        setOwner(meta, HEAD_PNAME);
        ITEM.setItemMeta(meta); // This enchantment will prevent not to remove a head the player got elsewhere
        ITEM.addUnsafeEnchantment(ench, 1);
    }

    @SuppressWarnings("deprecation")
    private void setOwner(ItemMeta meta, String owner) {
        if (meta instanceof SkullMeta) ((SkullMeta) meta).setOwner(owner);
    }

    private double velocity(String v) {
        /* @formatter:off */try { return Double.parseDouble(v); } catch (Exception e) { return 0.5; } // @formatter:on
    }
}
