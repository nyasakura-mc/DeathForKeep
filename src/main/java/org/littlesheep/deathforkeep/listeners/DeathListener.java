/*
  死亡监听器
  处理玩家死亡事件
 */
package org.littlesheep.deathforkeep.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.utils.Messages;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DeathListener implements Listener {

    private final DeathForKeep plugin;
    // 用于存储玩家的物品备份
    private final Map<UUID, ItemStack[]> inventoryBackups = new HashMap<>();
    private final Map<UUID, ItemStack[]> armorBackups = new HashMap<>();
    private final Map<UUID, Integer> expBackups = new HashMap<>();

    public DeathListener(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    // 在死亡事件的最早阶段备份物品
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathEarly(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        
        if (plugin.hasActiveProtection(playerUUID)) {
            // 备份玩家物品和经验(如果启用了备份功能)
            if (plugin.getConfig().getBoolean("use-inventory-backup", true)) {
                inventoryBackups.put(playerUUID, player.getInventory().getContents().clone());
                armorBackups.put(playerUUID, player.getInventory().getArmorContents().clone());
                expBackups.put(playerUUID, player.getTotalExperience());
            }
            
            // 尝试设置保持物品
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }
    
    // 监听玩家重生事件，恢复物品
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!plugin.getConfig().getBoolean("use-inventory-backup", true)) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (inventoryBackups.containsKey(playerUUID)) {
            // 延迟1tick恢复物品，以确保在所有插件处理后执行
            new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        if (player.isOnline()) {
                            // 恢复物品
                            if (inventoryBackups.get(playerUUID) != null) {
                                player.getInventory().setContents(inventoryBackups.get(playerUUID));
                            }
                            
                            if (armorBackups.get(playerUUID) != null) {
                                player.getInventory().setArmorContents(armorBackups.get(playerUUID));
                            }
                            
                            // 恢复经验
                            if (expBackups.containsKey(playerUUID)) {
                                player.setTotalExperience(0);
                                player.setLevel(0);
                                player.setExp(0);
                                player.giveExp(expBackups.get(playerUUID));
                            }
                            
                            // 更新物品栏
                            player.updateInventory();
                            
                            // 清理备份数据
                            inventoryBackups.remove(playerUUID);
                            armorBackups.remove(playerUUID);
                            expBackups.remove(playerUUID);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("恢复玩家物品时出错: " + e.getMessage());
                        // 尝试安全地清理备份数据
                        try {
                            inventoryBackups.remove(playerUUID);
                            armorBackups.remove(playerUUID);
                            expBackups.remove(playerUUID);
                        } catch (Exception ex) {
                            plugin.getLogger().severe("清理备份数据时出错: " + ex.getMessage());
                        }
                    }
                }
            }.runTaskLater(plugin, 1L);
        }
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        Messages messages = plugin.getMessages();
        
        boolean hasProtection = plugin.hasActiveProtection(playerUUID);
        
        if (hasProtection) {
            // 强制设置保持物品和经验
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            event.getDrops().clear();
            
            // 发送消息给玩家
            player.sendMessage(messages.getMessage("death.protected"));
            
            // 播放粒子效果
            if (plugin.getConfig().getBoolean("particles.on-protection-used", true)) {
                spawnProtectionParticles(player.getLocation());
            }
            
            // 广播消息
            broadcastDeathProtection(player);
            
            // 再次确认备份已存在
            if (plugin.getConfig().getBoolean("use-inventory-backup", true) && !inventoryBackups.containsKey(playerUUID)) {
                inventoryBackups.put(playerUUID, player.getInventory().getContents().clone());
                armorBackups.put(playerUUID, player.getInventory().getArmorContents().clone());
                expBackups.put(playerUUID, player.getTotalExperience());
            }
        }
    }
    
    private void spawnProtectionParticles(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("无法生成粒子效果: 位置或世界为null");
            return;
        }
        
        try {
            String particleType = plugin.getConfig().getString("particles.type", "TOTEM");
            int count = plugin.getConfig().getInt("particles.count", 50);
            double offsetX = plugin.getConfig().getDouble("particles.offset-x", 0.5);
            double offsetY = plugin.getConfig().getDouble("particles.offset-y", 1.0);
            double offsetZ = plugin.getConfig().getDouble("particles.offset-z", 0.5);
            double speed = plugin.getConfig().getDouble("particles.speed", 0.1);
            
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld().spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, speed);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的粒子类型: " + particleType + "，使用默认TOTEM");
                location.getWorld().spawnParticle(Particle.TOTEM, location, count, offsetX, offsetY, offsetZ, speed);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("生成粒子效果时出错: " + e.getMessage());
        }
    }
    
    private void broadcastDeathProtection(Player player) {
        String broadcastRange = plugin.getConfig().getString("broadcast-range", "world");
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("death.broadcast", "player", player.getName());
        
        if ("server".equalsIgnoreCase(broadcastRange)) {
            // 全服广播
            Bukkit.broadcastMessage(message);
        } else if ("world".equalsIgnoreCase(broadcastRange)) {
            // 同世界广播
            World world = player.getWorld();
            for (Player p : world.getPlayers()) {
                p.sendMessage(message);
            }
        } else if ("none".equalsIgnoreCase(broadcastRange)) {
            // 不广播
            return;
        } else {
            // 默认同世界广播
            World world = player.getWorld();
            for (Player p : world.getPlayers()) {
                p.sendMessage(message);
            }
        }
    }
} 