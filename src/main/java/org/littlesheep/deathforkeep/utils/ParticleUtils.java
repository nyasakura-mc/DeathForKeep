/*
  粒子效果工具类
  提供粒子效果的播放方法
 */
package org.littlesheep.deathforkeep.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ParticleUtils {

    private static final Random random = new Random();
    private static final ConcurrentHashMap<String, Integer> activeEffects = new ConcurrentHashMap<>();

    /**
     * 播放共享保护成功的粒子效果
     * 
     * @param sender 发送者
     * @param target 接收者
     */
    public static void playShareEffect(Player sender, Player target) {
        Location senderLoc = sender.getLocation().add(0, 1, 0);
        Location targetLoc = target.getLocation().add(0, 1, 0);
        
        // 在发送者位置播放粒子
        sender.getWorld().spawnParticle(Particle.HEART, senderLoc, 20, 0.5, 0.5, 0.5, 0.1);
        
        // 在接收者位置播放粒子
        target.getWorld().spawnParticle(Particle.HEART, targetLoc, 20, 0.5, 0.5, 0.5, 0.1);
    }
    
    /**
     * 播放死亡保护激活的粒子效果
     * 
     * @param player 玩家
     * @param particleType 粒子类型
     * @param count 粒子数量
     */
    public static void playDeathProtectionEffect(Player player, String particleType, int count) {
        Location loc = player.getLocation().add(0, 1, 0);
        Particle particle;
        
        try {
            particle = Particle.valueOf(particleType);
        } catch (IllegalArgumentException e) {
            particle = Particle.TOTEM;
        }
        
        player.getWorld().spawnParticle(particle, loc, count, 0.5, 0.5, 0.5, 0.1);
    }
    
    /**
     * 播放保护获得时的粒子效果
     * @param plugin 插件实例
     * @param player 玩家
     * @param duration 持续时间（秒）
     */
    public static void playProtectionGainedEffect(DeathForKeep plugin, Player player, int duration) {
        if (player == null || !player.isOnline()) return;
        
        String effectId = player.getUniqueId().toString() + "-gain-" + System.currentTimeMillis();
        String particleType = plugin.getConfig().getString("particles.on-protection-gained.type", "TOTEM");
        int count = plugin.getConfig().getInt("particles.on-protection-gained.count", 200);
        
        try {
            Particle particle = Particle.valueOf(particleType);
            AtomicInteger timer = new AtomicInteger(0);
            
            // 取消可能存在的同类效果
            if (activeEffects.containsKey(player.getUniqueId().toString() + "-gain")) {
                Bukkit.getScheduler().cancelTask(activeEffects.get(player.getUniqueId().toString() + "-gain"));
            }
            
            // 创建新效果
            int taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline() || timer.incrementAndGet() > duration) {
                        this.cancel();
                        activeEffects.remove(effectId);
                        return;
                    }
                    
                    Location loc = player.getLocation().add(0, 1, 0);
                    
                    // 创建更加复杂的粒子效果
                    switch (particle) {
                        case TOTEM:
                            playTotemParticles(player, count);
                            break;
                        case FLAME:
                            playFlameParticles(player, count);
                            break;
                        case HEART:
                            playHeartParticles(player, count);
                            break;
                        case PORTAL:
                            playPortalParticles(player, count);
                            break;
                        default:
                            player.getWorld().spawnParticle(particle, loc, count, 0.5, 1.0, 0.5, 0.1);
                            break;
                    }
                }
            }.runTaskTimerAsynchronously(plugin, 0L, 10L).getTaskId();
            
            // 记录效果ID
            activeEffects.put(effectId, taskId);
            
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的粒子类型: " + particleType);
            Location loc = player.getLocation().add(0, 1, 0);
            player.getWorld().spawnParticle(Particle.TOTEM, loc, count, 0.5, 1.0, 0.5, 0.1);
        }
    }
    
    /**
     * 播放图腾粒子效果
     */
    private static void playTotemParticles(Player player, int count) {
        Location center = player.getLocation().add(0, 1, 0);
        
        // 螺旋上升效果
        for (int i = 0; i < count / 2; i++) {
            double angle = (i * (Math.PI * 2) / 20);
            double radius = 0.8;
            double height = (i % 20) * 0.1;
            
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + height;
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(Particle.TOTEM, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // 爆发效果
        center.getWorld().spawnParticle(Particle.TOTEM, center, count / 2, 0.5, 1.0, 0.5, 0.2);
    }
    
    /**
     * 播放火焰粒子效果
     */
    private static void playFlameParticles(Player player, int count) {
        Location center = player.getLocation().add(0, 1, 0);
        
        // 上升的火焰圆环
        for (int i = 0; i < count / 3; i++) {
            double angle = (i * (Math.PI * 2) / 20);
            double radius = 0.8 + (random.nextDouble() * 0.3);
            double height = (i % 10) * 0.2;
            
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + height;
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(Particle.FLAME, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // 中心火焰
        center.getWorld().spawnParticle(Particle.FLAME, center, count / 3, 0.2, 0.5, 0.2, 0.05);
        
        // 爆发火花
        center.getWorld().spawnParticle(Particle.LAVA, center, 10, 0.5, 0.5, 0.5, 0);
    }
    
    /**
     * 播放心形粒子效果
     */
    private static void playHeartParticles(Player player, int count) {
        Location center = player.getLocation().add(0, 1.5, 0);
        
        // 心形轮廓
        for (int i = 0; i < count / 2; i++) {
            double t = (i / 20.0) * Math.PI * 2;
            // 心形函数
            double x = 16 * Math.pow(Math.sin(t), 3);
            double y = 13 * Math.cos(t) - 5 * Math.cos(2*t) - 2 * Math.cos(3*t) - Math.cos(4*t);
            
            // 缩放和定位
            x = x * 0.03;
            y = -y * 0.03;
            
            Location particleLoc = center.clone().add(x, y, 0);
            center.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // 散布在玩家周围的心形粒子
        for (int i = 0; i < count / 2; i++) {
            double x = (random.nextDouble() - 0.5) * 2;
            double y = random.nextDouble() * 2;
            double z = (random.nextDouble() - 0.5) * 2;
            
            Location particleLoc = center.clone().add(x, y, z);
            center.getWorld().spawnParticle(Particle.HEART, particleLoc, 1, 0, 0, 0, 0);
        }
    }
    
    /**
     * 播放传送门粒子效果
     */
    private static void playPortalParticles(Player player, int count) {
        Location center = player.getLocation().add(0, 1, 0);
        
        // 垂直螺旋
        for (int i = 0; i < count / 2; i++) {
            double angle = (i * (Math.PI * 2) / 20);
            double radius = 1.2;
            double height = (i % 20) * 0.2 - 1;
            
            double x = center.getX() + radius * Math.cos(angle);
            double y = center.getY() + height;
            double z = center.getZ() + radius * Math.sin(angle);
            
            Location particleLoc = new Location(center.getWorld(), x, y, z);
            center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }
        
        // 传送门形状
        double portalHeight = 2.0;
        double portalWidth = 1.0;
        for (int i = 0; i < count / 2; i++) {
            double angle = (i / (double)(count / 2)) * Math.PI * 2;
            double x = Math.sin(angle) * portalWidth / 2;
            double y = Math.cos(angle) * portalHeight / 2;
            
            Location particleLoc = center.clone().add(x, y, 0);
            center.getWorld().spawnParticle(Particle.PORTAL, particleLoc, 1, 0, 0, 0, 0);
        }
    }
} 