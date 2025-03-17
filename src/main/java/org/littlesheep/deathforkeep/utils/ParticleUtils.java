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
        if (!plugin.getConfig().getBoolean("particles.on-protection-gained.enabled", true)) {
            return;
        }
        
        String particleType = plugin.getConfig().getString("particles.on-protection-gained.type", "TOTEM");
        final int count = plugin.getConfig().getInt("particles.on-protection-gained.count", 100);
        
        // 确定粒子类型
        Particle particleToUse;
        try {
            particleToUse = Particle.valueOf(particleType);
        } catch (IllegalArgumentException e) {
            particleToUse = Particle.TOTEM;
        }
        
        // 使用final变量存储粒子类型，以便在匿名内部类中使用
        final Particle finalParticle = particleToUse;
        
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
                player.getWorld().spawnParticle(finalParticle, loc, count / 20, 0.5, 0.5, 0.5, 0.1);
                remainingTicks--;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }
} 