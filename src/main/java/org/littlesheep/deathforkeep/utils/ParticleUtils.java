package org.littlesheep.deathforkeep.utils;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

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
} 