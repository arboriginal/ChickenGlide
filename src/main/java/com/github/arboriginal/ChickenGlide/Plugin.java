package com.github.arboriginal.ChickenGlide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class Plugin extends JavaPlugin {
    static Plugin          inst;
    ArrayList<UUID>        gliders;
    BukkitTask             task;
    Configuration          conf;
    HashMap<String, int[]> eeCs;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("cg-reload")) return super.onCommand(sender, command, label, args);
        reloadConfig();
        sender.sendMessage(conf.MSG_RELOADED);
        return true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        HandlerList.unregisterAll(this);
        Utils.taskStop();
        EE.eeCleanup();
    }

    @Override
    public void onEnable() {
        inst = this;
        eeCs = new HashMap<String, int[]>();
        super.onEnable();
        reloadConfig();
        getServer().getPluginManager().registerEvents(new Listeners(), this);
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();

        Utils.taskStop();
        saveDefaultConfig();

        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);
        saveConfig();

        conf    = new Configuration(config);
        gliders = new ArrayList<UUID>();

        if (conf.DAMAGES) {
            for (Player p : Bukkit.getOnlinePlayers()) if (Utils.carry(p) != null) gliders.add(p.getUniqueId());
            Utils.taskStart();
        }
    }
}
