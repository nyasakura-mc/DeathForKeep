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
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.utils.Messages;

import java.util.UUID;

public class DeathListener implements Listener {

    private final DeathForKeep plugin;

    public DeathListener(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        Messages messages = plugin.getMessages();
        
        plugin.getLogger().info("玩家 " + player.getName() + " (" + playerUUID + ") 死亡，检查保护状态");
        
        boolean hasProtection = plugin.hasActiveProtection(playerUUID);
        plugin.getLogger().info("玩家 " + player.getName() + " 的保护状态: " + hasProtection);
        
        if (hasProtection) {
            // 阻止物品掉落
            event.setKeepInventory(true);
            // 阻止经验掉落
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            
            plugin.getLogger().info("玩家 " + player.getName() + " 的物品和经验保护生效");
            
            // 发送消息给玩家
            player.sendMessage(messages.getMessage("death.protected"));
            
            // 播放粒子效果
            if (plugin.getConfig().getBoolean("particles.on-protection-used", true)) {
                spawnProtectionParticles(player.getLocation());
                plugin.getLogger().info("为玩家 " + player.getName() + " 播放了保护粒子效果");
            }
            
            // 广播消息
            broadcastDeathProtection(player);
            plugin.getLogger().info("广播了玩家 " + player.getName() + " 的死亡保护消息");
        } else {
            plugin.getLogger().info("玩家 " + player.getName() + " 没有活跃的保护，物品将掉落");
        }
    }
    
    private void spawnProtectionParticles(Location location) {
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