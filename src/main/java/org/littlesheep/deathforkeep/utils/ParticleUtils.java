/*
  粒子效果工具类
  提供粒子效果的播放方法
 */
package org.littlesheep.deathforkeep.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.littlesheep.deathforkeep.DeathForKeep;

public class ParticleUtils {

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
     * 播放获取保护时的持续粒子效果
     * 
     * @param plugin 插件实例
     * @param player 玩家
     * @param duration 持续时间（秒）
     */
    public static void playProtectionGainedEffect(DeathForKeep plugin, Player player, int duration) {
        // 决定使用哪个配置部分
        String configSection = "particles.on-protection-gained";
        
        // 检查调用堆栈以确定是在切换粒子还是获得保护时调用的
        // 这种方法不是最理想的，但可以工作
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (element.getMethodName().equals("toggleParticles") || 
                element.getMethodName().equals("handleParticles")) {
                configSection = "particles.on-toggle";
                break;
            }
        }
        
        if (!plugin.getConfig().getBoolean(configSection + ".enabled", true)) {
            return;
        }
        
        String particleType = plugin.getConfig().getString(configSection + ".type", "TOTEM");
        final int count = plugin.getConfig().getInt(configSection + ".count", 100);
        final double offsetX = plugin.getConfig().getDouble("particles.offset-x", 0.8);
        final double offsetY = plugin.getConfig().getDouble("particles.offset-y", 1.5);
        final double offsetZ = plugin.getConfig().getDouble("particles.offset-z", 0.8);
        final double speed = plugin.getConfig().getDouble("particles.speed", 0.2);
        
        // 确定粒子类型
        Particle particleToUse;
        try {
            particleToUse = Particle.valueOf(particleType);
        } catch (IllegalArgumentException e) {
            particleToUse = Particle.TOTEM;
        }
        
        // 使用final变量存储粒子类型，以便在匿名内部类中使用
        final Particle finalParticle = particleToUse;
        
        // 立即显示一次粒子爆发
        Location initialLoc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(finalParticle, initialLoc, count, offsetX, offsetY, offsetZ, speed);
        
        // 创建持续显示粒子的任务
        new BukkitRunnable() {
            int remainingTicks = duration * 20; // 转换为tick (20 tick = 1秒)
            
            @Override
            public void run() {
                if (remainingTicks <= 0 || !player.isOnline()) {
                    this.cancel();
                    return;
                }
                
                Location loc = player.getLocation().add(0, 1, 0);
                // 随着时间推移，逐渐减少粒子数量，制造渐隐效果
                int currentCount = (int)(count / 20.0 * ((double)remainingTicks / (duration * 20.0) + 0.5));
                player.getWorld().spawnParticle(finalParticle, loc, currentCount, offsetX, offsetY, offsetZ, speed);
                
                // 每5ticks产生一次环形粒子
                if (remainingTicks % 5 == 0) {
                    double radius = 1.0;
                    for (int i = 0; i < 12; i++) {
                        double angle = 2 * Math.PI * i / 12;
                        Location circleLoc = loc.clone().add(
                            radius * Math.cos(angle), 
                            0.2, 
                            radius * Math.sin(angle)
                        );
                        player.getWorld().spawnParticle(finalParticle, circleLoc, 2, 0.1, 0.1, 0.1, 0.05);
                    }
                }
                
                remainingTicks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 