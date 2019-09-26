package me.arboriginal.ChickenGlide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.spigotmc.event.entity.EntityDismountEvent;

public class Plugin extends JavaPlugin implements Listener {
  private FileConfiguration      config;
  private Options                options;
  private ArrayList<UUID>        gliders;
  private BukkitTask             task;
  private HashMap<String, int[]> eeChunks;
  private final String           eeMDKey = "gcEE";

  // -- Private classes -----------------------------------------------------------------------------------------------

  private class Options {
    ItemStack chicken_head;

    int     behaviors__takes_damages__frequency, limitations__max_duration;
    double  behaviors__eject_velocity_min, behaviors__eject_velocity_max, behaviors__takes_damages__amount, ee__chance;
    String  chicken_head__display_name, chicken_head__player_name;
    boolean behaviors__change_slot_launch, behaviors__ignore_grass, behaviors__glide_when_carrying,
        behaviors__leave_by_itself, behaviors__place_when_sneaking, behaviors__right_click_launch,
        behaviors__takes_damages__enabled, limitations__baby_chicken, limitations__lock_hand,
        limitations__stop_on_eject, ee__alert_players, ee__enabled, ee__log_events;

    Options(FileConfiguration config) {
      behaviors__change_slot_launch       = config.getBoolean("behaviors.change_slot_launch");
      behaviors__glide_when_carrying      = config.getBoolean("behaviors.glide_when_carrying");
      behaviors__ignore_grass             = config.getBoolean("behaviors.ignore_grass");
      behaviors__leave_by_itself          = config.getBoolean("behaviors.leave_by_itself");
      behaviors__place_when_sneaking      = config.getBoolean("behaviors.place_when_sneaking");
      behaviors__right_click_launch       = config.getBoolean("behaviors.right_click_launch");
      behaviors__takes_damages__enabled   = config.getBoolean("behaviors.takes_damages.enabled");
      limitations__baby_chicken           = config.getBoolean("limitations.baby_chicken");
      limitations__lock_hand              = config.getBoolean("limitations.lock_hand");
      limitations__stop_on_eject          = config.getBoolean("limitations.stop_on_eject");
      behaviors__takes_damages__amount    = config.getDouble("behaviors.takes_damages.amount");
      behaviors__takes_damages__frequency = config.getInt("behaviors.takes_damages.frequency");
      limitations__max_duration           = config.getInt("limitations.max_duration");
      chicken_head__display_name          = config.getString("chicken_head.display_name");
      chicken_head__player_name           = config.getString("chicken_head.player_name");
      ee__alert_players                   = config.getBoolean("easteregg_alert_players");
      ee__enabled                         = config.getBoolean("easteregg.enabled");
      ee__log_events                      = config.getBoolean("easteregg.log_events");
      ee__chance                          = Math.max(0.01, Math.min(config.getDouble("easteregg.chance"), 100)) / 1000;

      String[] velocities = config.getString("behaviors.eject_velocity").split(";");
      double   min        = velocity(velocities[0]), max = (velocities.length == 2) ? velocity(velocities[1]) : min;

      behaviors__eject_velocity_min = Math.min(min, max);
      behaviors__eject_velocity_max = Math.max(min, max);

      chicken_head = getChickenHead();
    }

    @SuppressWarnings("deprecation")
    private ItemStack getChickenHead() {
      ItemStack head = new ItemStack(Material.PLAYER_HEAD);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      meta.setOwner(chicken_head__player_name);
      meta.setDisplayName(chicken_head__display_name);
      meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      head.setItemMeta(meta); // The enchantment on the head will prevent not to remove a head the player got elsewhere
      head.addEnchantment(Enchantment.VANISHING_CURSE, 1);
      return head;
    }

    @SuppressWarnings("deprecation")
    private boolean isChickenHead(ItemStack item) {
      if (item == null || !item.getType().equals(Material.PLAYER_HEAD)) return false;

      SkullMeta meta = (SkullMeta) item.getItemMeta();

      return meta.getOwner().equals(chicken_head__player_name)
          && meta.getDisplayName().equals(chicken_head__display_name)
          && meta.hasEnchant(Enchantment.VANISHING_CURSE);
    }

    private double velocity(String value) {
      try {
        return Double.parseDouble(value);
      }
      catch (Exception e) {
        return 0.5;
      }
    }
  }

  // -- JavaPlugin methods --------------------------------------------------------------------------------------------

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (command.getName().equalsIgnoreCase("cg-reload")) {
      reloadConfig();
      sender.sendMessage(config.getString("messages.config_reloaded"));
      return true;
    }

    return super.onCommand(sender, command, label, args);
  }

  @Override
  public void onDisable() {
    super.onDisable();
    HandlerList.unregisterAll((JavaPlugin) this);
    stopTask();
    eeCleanup();
  }

  @Override
  public void onEnable() {
    eeChunks = new HashMap<String, int[]>();
    super.onEnable();
    reloadConfig();
    getServer().getPluginManager().registerEvents(this, this);
  }

  @Override
  public void reloadConfig() {
    super.reloadConfig();

    stopTask();
    saveDefaultConfig();
    config = getConfig();
    config.options().copyDefaults(true);
    saveConfig();

    options = new Options(config);
    gliders = new ArrayList<UUID>();
    // Update gliders list (in case of a plugin reload via plugman or similar)
    if (options.behaviors__takes_damages__enabled) {
      for (Player player : getServer().getOnlinePlayers()) {
        Chicken chicken = getPlayerChicken(player);
        if (chicken != null) gliders.add(player.getUniqueId());
      }

      startTask();
    }
  }

  // -- Listener methods ----------------------------------------------------------------------------------------------

  @EventHandler
  private void onEntityDismount(EntityDismountEvent event) {
    if (event.isCancelled() || !(event.getEntity() instanceof Chicken)
        || !(event.getDismounted() instanceof Player))
      return;

    Player player = (Player) event.getDismounted();

    if (options.limitations__stop_on_eject && options.behaviors__glide_when_carrying) {
      player.removePotionEffect(PotionEffectType.SLOW_FALLING);
      if (options.behaviors__takes_damages__enabled) gliders.remove(player.getUniqueId());
    }

    Inventory playerInventory = player.getInventory();
    for (int i = 0; i < playerInventory.getSize(); i++)
      if (options.isChickenHead(playerInventory.getItem(i))) playerInventory.setItem(i, null);
  }

  @EventHandler
  private void onPlayerInteract(PlayerInteractEvent event) {
    if (!options.behaviors__ignore_grass // @formatter:off
     || event.getHand()   != EquipmentSlot.HAND
     || event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

    Block block = event.getClickedBlock();
    if (block == null || (
       !block.getType().equals(Material.GRASS)
    && !block.getType().equals(Material.TALL_GRASS))) return;

    Player player = event.getPlayer();
    if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) return;

    for (Entity entity : block.getWorld().getNearbyEntities(block.getLocation(), 1, 1, 1))
      if (entity instanceof Chicken) {
        Bukkit.getPluginManager().callEvent(
            new PlayerInteractEntityEvent(player, entity, EquipmentSlot.HAND)); break;
      } // @formatter:on
  }

  @EventHandler
  private void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
    if (event.isCancelled()) return;

    EquipmentSlot slot = event.getHand();
    if (slot == EquipmentSlot.OFF_HAND) return;

    Entity entity = event.getRightClicked();
    if (!(entity instanceof Chicken)) return;

    Player player = event.getPlayer();

    if (player.getPassengers().size() > 0) {
      if (!getPlayerChicken(player).equals(entity) || !options.isChickenHead(player.getInventory().getItemInMainHand()))
        return;

      if (options.behaviors__place_when_sneaking && player.isSneaking()) {
        player.eject();
        entity.teleport(player.getLocation());
        return;
      }

      if (options.behaviors__right_click_launch)
        launchChicken(player, entity);
      else
        player.eject();
      return;
    }

    if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)
        || !player.hasPermission("cg.glide") || player.isInsideVehicle()
        || (!((Ageable) entity).isAdult() && !options.limitations__baby_chicken))
      return;

    player.addPassenger(entity);

    if (options.behaviors__glide_when_carrying)
      player.addPotionEffect(new PotionEffect(
          PotionEffectType.SLOW_FALLING, options.limitations__max_duration * 20, 0, false, false, false));

    if (options.behaviors__takes_damages__enabled) {
      gliders.add(player.getUniqueId());
      startTask();
    }

    player.getInventory().setItemInMainHand(options.chicken_head);
  }

  @EventHandler
  private void onPlayerItemHeld(PlayerItemHeldEvent event) {
    if (event.isCancelled()) return;
    Player player  = event.getPlayer();
    Entity chicken = getPlayerChicken(player);
    if (chicken == null) return;

    if (options.limitations__lock_hand)
      event.setCancelled(true);
    else if (options.behaviors__change_slot_launch)
      launchChicken(player, chicken);
    else
      player.eject();
  }

  @EventHandler
  private void onInventoryClick(InventoryClickEvent event) {
    if (!event.isCancelled() && options.isChickenHead(event.getCurrentItem())) event.setCancelled(true);
  }

  @EventHandler
  private void onPlayerDropItem(PlayerDropItemEvent event) {
    if (event.isCancelled()) return;
    Item item = event.getItemDrop();
    if (!options.isChickenHead(item.getItemStack())) return;

    Player  player  = event.getPlayer();
    Chicken chicken = getPlayerChicken(player);

    if (chicken != null) launchChicken(player, chicken);
    item.remove();
  }

  @EventHandler
  private void onEntityPotionEffect(EntityPotionEffectEvent event) {
    // @formatter:off
    if ( event.isCancelled()
     || !event.getCause().equals(EntityPotionEffectEvent.Cause.EXPIRATION)
     || (event.getAction() != EntityPotionEffectEvent.Action.REMOVED
      && event.getAction() != EntityPotionEffectEvent.Action.CLEARED)
    ) return;
    
    Entity           entity = event.getEntity();
    PotionEffectType type   = event.getOldEffect().getType();

    if (options.behaviors__leave_by_itself && options.behaviors__glide_when_carrying && entity instanceof Player
      && type.equals(PotionEffectType.SLOW_FALLING) && getPlayerChicken((Player) entity) != null) entity.eject();
    
    if (entity instanceof Cow && type.equals(PotionEffectType.LEVITATION) && entity.hasMetadata(eeMDKey))
      eeGiveUp(entity.getLocation().getChunk()); // @formatter:on
  }

  // -- Listener methods... Why so serious??? -------------------------------------------------------------------------

  @EventHandler
  private void onChunkLoad(ChunkLoadEvent event) {
    if (eeIsActive(event.getWorld())) eeStart(event.getChunk());
  }

  @EventHandler
  private void onChunkUnload(ChunkUnloadEvent event) {
    if (eeIsActive(event.getWorld())) eeGiveUp(event.getChunk());
  }

  // -- Helper methods ------------------------------------------------------------------------------------------------

  private Chicken getPlayerChicken(Player player) {
    if (player.isOnline() && player.getPassengers().size() > 0 && player.getPassengers().get(0) instanceof Chicken)
      return (Chicken) player.getPassengers().get(0);

    return null;
  }

  private double getVelocity() {
    if (options.behaviors__eject_velocity_min == options.behaviors__eject_velocity_max)
      return options.behaviors__eject_velocity_min;

    return (Math.random() * (options.behaviors__eject_velocity_max - options.behaviors__eject_velocity_min))
        + options.behaviors__eject_velocity_min;
  }

  private void launchChicken(Player player, Entity chicken) {
    player.eject();
    double velocity = getVelocity();

    if (velocity > 0)
      chicken.setVelocity(player.getLocation().getDirection().normalize().multiply(velocity));
  }

  private void startTask() {
    if (task == null || task.isCancelled()) task = new BukkitRunnable() {
      @Override
      public void run() {
        if (isCancelled()) return;
        if (gliders.size() == 0) {
          cancel();
          return;
        }

        double damages = options.behaviors__takes_damages__amount;

        for (UUID uuid : gliders) {
          Player  player  = getServer().getPlayer(uuid);
          Chicken chicken = getPlayerChicken(player);
          if (chicken != null) chicken.damage(damages, player);
        }
      }
    }.runTaskTimer(this, 1, options.behaviors__takes_damages__frequency * 20);
  }

  private void stopTask() {
    if (task != null && !task.isCancelled()) task.cancel();
    task = null;
  }

  // -- EasterEgg methods ---------------------------------------------------------------------------------------------

  private Chunk eeChunkByKey(String key) {
    String[] parts = key.split("_");

    try {
      return getServer().getWorld(parts[0]).getChunkAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
    }
    catch (Exception e) {
      return null;
    }
  }

  private String eeChunkKey(Chunk chunk) {
    return chunk.getWorld().getName() + "_" + chunk.getX() + "_" + chunk.getZ();
  }

  private void eeCleanup() {
    if (eeChunks.isEmpty()) return;
    // Using an array here prevent concurrent modifications error
    String[] chunks = eeChunks.keySet().toArray(new String[0]);
    for (String chunkKey : chunks) {
      eeCleanup(chunkKey);
    }
  }

  private void eeCleanup(String chunkKey) {
    int[] IDs = eeChunks.get(chunkKey);
    if (IDs == null) return;

    Chunk chunk = eeChunkByKey(chunkKey);
    if (chunk == null) return;

    eeGiveUp(chunk, IDs, chunkKey);
  }

  private void eeGiveUp(Chunk chunk) {
    String chunkKey = eeChunkKey(chunk);

    int[] IDs = eeChunks.get(chunkKey);
    if (IDs == null) return;

    eeGiveUp(chunk, IDs, chunkKey);
  }

  private void eeGiveUp(Chunk chunk, int[] IDs, String chunkKey) {
    eeStop(IDs[0], chunkKey);

    boolean loaded = chunk.isLoaded();

    if (loaded || chunk.load()) {
      for (Entity entity : chunk.getEntities()) {
        int id = entity.getEntityId();

        if (id == IDs[1]) {
          if (entity.hasMetadata(eeMDKey)) {
            entity.removeMetadata(eeMDKey, this);
            entity.setGlowing(false);
            ((Cow) entity).setAI(true);

            if (((Cow) entity).hasPotionEffect(PotionEffectType.LEVITATION))
              ((Cow) entity).removePotionEffect(PotionEffectType.LEVITATION);
          }
        }
        else if (id == IDs[2] && entity.hasMetadata(eeMDKey)) entity.remove();
      }

      if (!loaded) chunk.unload();
      eeLog(chunk, "cancelled");
    }
  }

  private boolean eeIsActive(World world) {
    return (options.ee__enabled && world.getEnvironment().equals(Environment.NORMAL));
  }

  private boolean eeIsTop(Location loc) {
    int   y     = loc.getBlockY();
    Block block = loc.getBlock();

    if (y < 63 || !block.getRelative(BlockFace.DOWN).getType().equals(Material.GRASS_BLOCK)) return false;

    for (int distance = 1; y + distance < 255; distance++)
      if (block.getRelative(BlockFace.UP, distance).getType() != Material.AIR) return false;

    return true;
  }

  private void eeLog(Chunk chunk, String event) {
    if (!options.ee__log_events) return;
    int x = chunk.getX() << 4, z = chunk.getZ() << 4;

    getLogger().info(config.getString("messages.easteregg_log").replace("{event}", event)
        .replace("{coords}", "\tWorld: " + chunk.getWorld().getName() + ", chunk: " + chunk.getX() + "/" + chunk.getZ()
            + " (" + x + " / " + z + " <-> " + (x + 15) + " / " + (z + 15) + ")"));
  }

  private boolean eePlayerAround(Location loc) {
    for (Entity entity : loc.getWorld().getNearbyEntities(loc, 20, 20, 20))
      if (entity instanceof Player) return true;

    return false;
  }

  private void eeStart(Chunk chunk) {
    String chunkKey = eeChunkKey(chunk);
    if (eeChunks.containsKey(chunkKey) || Math.random() > options.ee__chance) return;

    for (Entity cow : chunk.getEntities()) {
      if (!(cow instanceof Cow) || !((Cow) cow).isAdult()) continue;

      Location loc = cow.getLocation();
      if (!eeIsTop(loc)) continue;

      FixedMetadataValue step    = new FixedMetadataValue(this, 1);
      World              world   = cow.getWorld();
      EnderCrystal       crystal = (EnderCrystal) world.spawnEntity(
          new Location(world, loc.getX(), 128, loc.getZ()), EntityType.ENDER_CRYSTAL);

      crystal.setInvulnerable(true);
      crystal.setGlowing(true);
      crystal.setGravity(false);
      crystal.setShowingBottom(false);
      crystal.setBeamTarget(loc);
      crystal.setMetadata(eeMDKey, step);
      cow.setMetadata(eeMDKey, step);
      ((Cow) cow).setAI(false);
      cow.setGlowing(true);

      eeChunks.put(chunkKey, new int[] {
          eeTask(chunk, (Cow) cow, crystal).getTaskId(),
          cow.getEntityId(), crystal.getEntityId() });

      if (options.ee__alert_players) {
        String message = config.getString("messages.easteregg_alert")
            .replace("{x}", "" + loc.getBlockX())
            .replace("{y}", "" + loc.getBlockY())
            .replace("{z}", "" + loc.getBlockZ());

        for (Player player : loc.getWorld().getPlayers()) player.sendMessage(message);
      }

      eeLog(chunk, "starts");
      return;
    }
  }

  private void eeStop(int taskID, String chunkKey) {
    Bukkit.getScheduler().cancelTask(taskID);
    eeChunks.remove(chunkKey);
  }

  private BukkitTask eeTask(Chunk chunk, Cow cow, EnderCrystal crystal) {
    Plugin plugin = this;

    return new BukkitRunnable() {
      @Override
      public void run() { // @formatter:off
        if (isCancelled()) return;
        if (!cow.isValid() || !crystal.isValid()) { eeGiveUp(chunk); return; }
        
        int step = cow.getMetadata(eeMDKey).get(0).asInt();
        if (step == 40) { eeGiveUp(chunk); return; }
        if (step == 15) {
          cow.setAI(true);
          cow.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 3600, 10), true);
        }
        
        Location loc = cow.getLocation();
        World  world = cow.getWorld();
        
        if (step > 1 || eePlayerAround(loc)) cow.setMetadata(eeMDKey, new FixedMetadataValue(plugin, ++step));
        
        crystal.setBeamTarget(new Location(world, loc.getX(), loc.getBlockY() - 1, loc.getZ())); 
        world.playSound(loc, Sound.BLOCK_CONDUIT_DEACTIVATE, 5, 1); 
        
        if (step % 2 == 0) {
          cow.damage(0.01, crystal);
          world.playSound(loc, Sound.ENTITY_COW_HURT, 2, 1);
        }
        
        if (loc.getBlockY() > crystal.getLocation().getBlockY() - 2) {
          Firework     firework = world.spawn(loc, Firework.class);
          FireworkMeta meta     = firework.getFireworkMeta();
          meta.addEffect(FireworkEffect.builder()
              .with(FireworkEffect.Type.BALL_LARGE).withColor(Color.LIME).build());
          meta.setPower(127); firework.setFireworkMeta(meta); firework.detonate();
          eeStop(getTaskId(), eeChunkKey(chunk)); cow.remove(); crystal.remove(); eeLog(chunk, "completed");
        }
      } // @formatter:on
    }.runTaskTimer(this, 10, 10);
  }
}
