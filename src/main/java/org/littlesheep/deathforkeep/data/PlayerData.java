/*
  玩家数据类
  存储玩家保护相关数据
 */
package org.littlesheep.deathforkeep.data;

import java.util.UUID;

public class PlayerData {
    private final UUID playerUUID;
    private long expiryTime;
    private UUID sharedWith;
    private boolean particlesEnabled = true;
    private String protectionLevel;
    private boolean keepExp;
    private String particleEffect;
    private boolean noDeathPenalty;
    private boolean forcedActive;
    
    public PlayerData(UUID playerUUID, long expiryTime, boolean active, UUID sharedWith) {
        this.playerUUID = playerUUID;
        this.expiryTime = expiryTime;
        this.sharedWith = sharedWith;
        this.forcedActive = active;
    }
    
    public UUID getUuid() {
        return playerUUID;
    }
    
    public long getExpiryTime() {
        return expiryTime;
    }
    
    public void setExpiryTime(long expiryTime) {
        this.expiryTime = expiryTime;
    }
    
    public boolean isParticlesEnabled() {
        return particlesEnabled;
    }
    
    public void setParticlesEnabled(boolean particlesEnabled) {
        this.particlesEnabled = particlesEnabled;
    }
    
    public UUID getSharedWith() {
        return sharedWith;
    }
    
    public void setSharedWith(UUID sharedWith) {
        this.sharedWith = sharedWith;
    }
    
    public boolean isActive() {
        if (forcedActive) {
            return true;
        }
        
        long currentTime = System.currentTimeMillis() / 1000;
        return expiryTime > currentTime;
    }

    public String getProtectionLevel() {
        return protectionLevel;
    }

    public void setProtectionLevel(String protectionLevel) {
        this.protectionLevel = protectionLevel;
    }

    public boolean isKeepExp() {
        return keepExp;
    }

    public void setKeepExp(boolean keepExp) {
        this.keepExp = keepExp;
    }

    public String getParticleEffect() {
        return particleEffect;
    }

    public void setParticleEffect(String particleEffect) {
        this.particleEffect = particleEffect;
    }

    public boolean isNoDeathPenalty() {
        return noDeathPenalty;
    }

    public void setNoDeathPenalty(boolean noDeathPenalty) {
        this.noDeathPenalty = noDeathPenalty;
    }

    public void setForcedActive(boolean forcedActive) {
        this.forcedActive = forcedActive;
    }
    
    public boolean getForcedActive() {
        return this.forcedActive;
    }
} 