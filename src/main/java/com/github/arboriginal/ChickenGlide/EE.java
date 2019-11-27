/*@formatter:off*/package com.github.arboriginal.ChickenGlide;import static com.github.arboriginal.ChickenGlide.Plugin.
inst;import org.bukkit.Bukkit;import org.bukkit.Chunk;import org.bukkit.Color;import org.bukkit.FireworkEffect;import
org.bukkit.Location;import org.bukkit.Material;import org.bukkit.Sound;import org.bukkit.World;import org.bukkit.World.
Environment;import org.bukkit.block.Block;import org.bukkit.block.BlockFace;import org.bukkit.entity.Cow;import org.
bukkit.entity.EnderCrystal;import org.bukkit.entity.Entity;import org.bukkit.entity.EntityType;import org.bukkit.entity.
Firework;import org.bukkit.entity.Player;import org.bukkit.inventory.meta.FireworkMeta;import org.bukkit.metadata.
FixedMetadataValue;import org.bukkit.potion.PotionEffect;import org.bukkit.potion.PotionEffectType;import org.bukkit.
scheduler.BukkitRunnable;import org.bukkit.scheduler.BukkitTask;public class EE{static final String EEK="gcEE";static
final PotionEffectType EEP=PotionEffectType.LEVITATION;static void eeCleanup(){if(!inst.eeCs.isEmpty())for(String k:inst
.eeCs.keySet().toArray(new String[0]))eeCleanup(k);}static boolean eeEnabled(World w){return(inst.conf.EE&&w.
getEnvironment()==Environment.NORMAL);}static void eeGiveUp(Chunk c){String k=eeChunkKey(c);int[]i=inst.eeCs.get(k);if(i
!=null)eeGiveUp(c,i,k);}static void eeStart(Chunk c){String k=eeChunkKey(c);if(inst.eeCs.containsKey(k)||Math.random()>
inst.conf.EE_CHANCE)return;for(Entity e:c.getEntities()){if(!(e instanceof Cow)||!((Cow)e).isAdult())continue;Location l
=e.getLocation();if(!eeIsTop(l))continue;FixedMetadataValue s=new FixedMetadataValue(inst,1);World w=e.getWorld();
EnderCrystal o=(EnderCrystal)w.spawnEntity(new Location(w,l.getX(),128,l.getZ()),EntityType.ENDER_CRYSTAL);o.
setInvulnerable(true);o.setGlowing(true);o.setGravity(false);o.setShowingBottom(false);o.setBeamTarget(l);o.setMetadata(
EEK,s);e.setMetadata(EEK,s);((Cow)e).setAI(false);e.setGlowing(true);inst.eeCs.put(k,new int[]{eeTask(c,(Cow)e,o).
getTaskId(),e.getEntityId(),o.getEntityId()});if(inst.conf.EE_ALERT){String m=inst.conf.MSG_EE_ALERT.replace("{x}",""+l.
getBlockX()).replace("{y}",""+l.getBlockY()).replace("{z}",""+l.getBlockZ());for(Player p:l.getWorld().getPlayers())p.
sendMessage(m);}eeLog(c,"starts");return;}}static private Chunk eeChunkByKey(String k){String[]parts=k.split("_");try{
return Bukkit.getWorld(parts[0]).getChunkAt(Integer.parseInt(parts[1]),Integer.parseInt(parts[2]));}catch(Exception e){
return null;}}static private String eeChunkKey(Chunk c){return c.getWorld().getName()+"_"+c.getX()+"_"+c.getZ();}static
private void eeCleanup(String k){int[]i=inst.eeCs.get(k);if(i==null)return;Chunk c=eeChunkByKey(k);if(c==null)return;
eeGiveUp(c,i,k);}static private void eeGiveUp(Chunk c,int[]i,String k){eeStop(i[0],k);boolean l=c.isLoaded();if(!l&&!c.
load())return;for(Entity e:c.getEntities()){int id=e.getEntityId();if(id==i[1]){if(e.hasMetadata(EEK)){e.removeMetadata(
EEK,inst);e.setGlowing(false);((Cow)e).setAI(true);if(((Cow)e).hasPotionEffect(EEP))((Cow)e).removePotionEffect(EEP);}}
else if(id==i[2]&&e.hasMetadata(EEK))e.remove();}if(!l)c.unload();eeLog(c,"cancelled");}static private boolean eeIsTop(
Location l){int y=l.getBlockY();if(y<63)return false;Block b=l.getBlock();if(!b.getRelative(BlockFace.DOWN).getType().
equals(Material.GRASS_BLOCK))return false;for(int d=1;y+d<255;d++)if(b.getRelative(BlockFace.UP,d).getType()!=Material.
AIR)return false;return true;}static private void eeLog(Chunk c,String e){if(!inst.conf.EE_LOG)return;int x=c.getX()<<4,
z=c.getZ()<<4;Bukkit.getLogger().info(inst.conf.MSG_EE_LOG.replace("{event}",e).replace("{coords}","\tWorld: "+c.
getWorld().getName()+",chunk: "+c.getX()+"/"+c.getZ()+"("+x+" / "+z+" <->"+(x+15)+" / "+(z+15)+")"));}static private
boolean eePlayerAround(Location l){for(Entity e:l.getWorld().getNearbyEntities(l,20,20,20))if(e instanceof Player)return
true;return false;}static private void eeStop(int t,String c){Bukkit.getScheduler().cancelTask(t);inst.eeCs.remove(c);}
static private BukkitTask eeTask(Chunk c,Cow e,EnderCrystal o){return new BukkitRunnable(){public void run(){if(
isCancelled())return;if(!e.isValid()||!o.isValid()){eeGiveUp(c);return;}int s=e.getMetadata(EEK).get(0).asInt();if(s==40
){eeGiveUp(c);return;}if(s==15){e.setAI(true);e.addPotionEffect(new PotionEffect(EEP,3600,10),true);}Location l=e.
getLocation();if(s>1||eePlayerAround(l))e.setMetadata(EEK,new FixedMetadataValue(inst,++s));World w=e.getWorld();o.
setBeamTarget(new Location(w,l.getX(),l.getBlockY()-1,l.getZ()));w.playSound(l,Sound.BLOCK_CONDUIT_DEACTIVATE,5,1);if(s%
2==0){e.damage(0.01,o);w.playSound(l,Sound.ENTITY_COW_HURT,2,1);}if(l.getBlockY()>o.getLocation().getBlockY()-2){
Firework f=w.spawn(l,Firework.class);FireworkMeta m=f.getFireworkMeta();m.setPower(127);m.addEffect(FireworkEffect.
builder().with(FireworkEffect.Type.BALL_LARGE).withColor(Color.LIME).build());f.setFireworkMeta(m);f.detonate();eeStop(
getTaskId(),eeChunkKey(c));e.remove();o.remove();eeLog(c,"completed");}}}.runTaskTimer(inst,10,10);}}/*formatter:on
Maybe I've forgot one or two spaces... Please respect the indentation of this file if you make pull requests :) */