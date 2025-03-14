package org.littlesheep.deathforkeep.data;

import java.util.UUID;

public class PlayerData {
    private final UUID uuid;
    private long expiryTime;
    private boolean particlesEnabled;
    private UUID sharedWith;
    
    public PlayerData(UUID uuid, long expiryTime, boolean particlesEnabled, UUID sharedWith) {
        this.uuid = uuid;
        this.expiryTime = expiryTime;
        this.particlesEnabled = particlesEnabled;
        this.sharedWith = sharedWith;
    }
    
    public UUID getUuid() {
        return uuid;
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
        return expiryTime > System.currentTimeMillis() / 1000;
    }
} 