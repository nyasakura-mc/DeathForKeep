/*
 * 死亡保护服务类
 * 负责处理玩家死亡保护相关的业务逻辑
 */
package org.littlesheep.deathforkeep.service;

import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.DatabaseManager;
import org.littlesheep.deathforkeep.data.PlayerData;

import java.util.Map;
import java.util.UUID;

public class ProtectionService {
    private final DeathForKeep plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerData> playerDataMap;

    public ProtectionService(DeathForKeep plugin, DatabaseManager databaseManager, Map<UUID, PlayerData> playerDataMap) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.playerDataMap = playerDataMap;
    }

    /**
     * 检查玩家是否拥有有效的保护
     * @param uuid 玩家UUID
     * @return 是否有效保护
     */
    public boolean hasActiveProtection(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return false;
        
        long expiryTime = data.getExpiryTime();
        return expiryTime > System.currentTimeMillis();
    }

    /**
     * 为玩家添加保护天数
     * @param uuid 玩家UUID
     * @param days 天数
     * @return 是否成功
     */
    public boolean addProtectionDays(UUID uuid, int days) {
        if (days <= 0) return false;
        
        PlayerData data = playerDataMap.get(uuid);
        long currentTime = System.currentTimeMillis();
        long expiryTime;
        boolean particlesEnabled = true;
        
        if (data == null) {
            // 新建数据
            expiryTime = currentTime + (days * 24L * 60L * 60L * 1000L);
            data = new PlayerData(uuid, expiryTime, particlesEnabled, null);
        } else {
            // 获取当前设置
            particlesEnabled = data.isParticlesEnabled();
            // 如果已经有保护，延长时间，否则从当前时间开始计算
            expiryTime = Math.max(data.getExpiryTime(), currentTime) + (days * 24L * 60L * 60L * 1000L);
            data.setExpiryTime(expiryTime);
        }
        
        // 更新数据
        playerDataMap.put(uuid, data);
        
        // 异步保存到数据库
        databaseManager.savePlayerDataAsync(uuid, expiryTime, particlesEnabled, data.getSharedWith());
        
        return true;
    }
    
    /**
     * 移除玩家的保护
     * @param uuid 玩家UUID
     * @return 是否成功
     */
    public boolean removeProtection(UUID uuid) {
        if (!playerDataMap.containsKey(uuid)) return false;
        
        PlayerData data = playerDataMap.get(uuid);
        data.setExpiryTime(0); // 设置为过期
        
        // 异步保存到数据库
        databaseManager.savePlayerDataAsync(uuid, 0, data.isParticlesEnabled(), data.getSharedWith());
        
        return true;
    }
    
    /**
     * 获取玩家剩余保护时间（毫秒）
     * @param uuid 玩家UUID
     * @return 剩余时间，无保护则返回0
     */
    public long getProtectionTimeLeft(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) return 0;
        
        long expiryTime = data.getExpiryTime();
        long currentTime = System.currentTimeMillis();
        
        return Math.max(0, expiryTime - currentTime);
    }
    
    /**
     * 设置玩家粒子效果开关
     * @param uuid 玩家UUID
     * @param enabled 是否启用
     */
    public void setParticlesEnabled(UUID uuid, boolean enabled) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) {
            data = new PlayerData(uuid, 0, enabled, null);
            playerDataMap.put(uuid, data);
        } else {
            data.setParticlesEnabled(enabled);
        }
        
        // 异步保存到数据库
        databaseManager.updateParticlesEnabled(uuid, enabled);
    }
    
    /**
     * 检查玩家粒子效果是否启用
     * @param uuid 玩家UUID
     * @return 是否启用
     */
    public boolean isParticlesEnabled(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data == null) {
            // 默认启用
            return plugin.getConfig().getBoolean("particles.enabled-by-default", true);
        }
        
        return data.isParticlesEnabled();
    }
} 