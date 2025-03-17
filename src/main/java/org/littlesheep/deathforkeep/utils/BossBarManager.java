package org.littlesheep.deathforkeep.utils;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BossBarManager {
    
    private final DeathForKeep plugin;
    private final Map<UUID, BossBar> playerBossBars = new HashMap<>();
    private BukkitTask updateTask;
    
    // BossBar 显示时间（秒）
    private static final long HOUR_REMINDER_DURATION = 60;
    private static final long TEN_MINUTE_REMINDER_DURATION = 60;
    private static final long EXPIRY_REMINDER_DURATION = 120;
    
    public BossBarManager(DeathForKeep plugin) {
        this.plugin = plugin;
    }
    
    public void startTask() {
        // 每秒更新一次 BossBar 进度
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateBossBars, 20L, 20L);
    }
    
    public void stopTask() {
        try {
            if (updateTask != null && !updateTask.isCancelled()) {
                updateTask.cancel();
                updateTask = null;
            }
            
            // 移除所有 BossBar
            for (BossBar bossBar : playerBossBars.values()) {
                try {
                    bossBar.removeAll();
                } catch (Exception e) {
                    plugin.getLogger().warning("移除BossBar时出错: " + e.getMessage());
                }
            }
            playerBossBars.clear();
            plugin.getLogger().info("所有BossBar已清理");
        } catch (Exception e) {
            plugin.getLogger().severe("停止BossBar任务时发生错误: " + e.getMessage());
        }
    }
    
    /**
     * 显示获取保护时的BossBar
     * 
     * @param player 玩家
     * @param expiryTime 保护到期时间
     */
    public void showProtectionGainedMessage(Player player, long expiryTime) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true) || 
            !plugin.getConfig().getBoolean("bossbar.protection-gained.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String formattedTime = TimeUtils.formatDateTime(expiryTime * 1000);
        String message = messages.getMessage("bossbar.protection-gained", "time", formattedTime);
        
        BarColor color = getBarColor("bossbar.protection-gained.color", BarColor.GREEN);
        int duration = plugin.getConfig().getInt("bossbar.protection-gained.duration", 10);
        
        showBossBar(player, message, color, duration);
    }
    
    /**
     * 显示被共享保护时的BossBar
     * 
     * @param player 接收共享的玩家
     * @param sharerName 共享者名称
     * @param expiryTime 保护到期时间
     */
    public void showProtectionSharedMessage(Player player, String sharerName, long expiryTime) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true) || 
            !plugin.getConfig().getBoolean("bossbar.protection-gained.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String formattedTime = TimeUtils.formatDateTime(expiryTime * 1000);
        String message = messages.getMessage("bossbar.protection-shared", 
                                             "player", sharerName,
                                             "time", formattedTime);
        
        BarColor color = getBarColor("bossbar.protection-gained.color", BarColor.GREEN);
        int duration = plugin.getConfig().getInt("bossbar.protection-gained.duration", 10);
        
        showBossBar(player, message, color, duration);
    }
    
    public void showHourReminder(Player player) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("bossbar.hour");
        BarColor color = getBarColor("bossbar.hour-color", BarColor.YELLOW);
        
        showBossBar(player, message, color, HOUR_REMINDER_DURATION);
    }
    
    public void showTenMinuteReminder(Player player) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("bossbar.ten-minutes");
        BarColor color = getBarColor("bossbar.ten-minute-color", BarColor.RED);
        
        showBossBar(player, message, color, TEN_MINUTE_REMINDER_DURATION);
    }
    
    public void showExpiryReminder(Player player) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("bossbar.expired");
        BarColor color = getBarColor("bossbar.expiry-color", BarColor.RED);
        
        showBossBar(player, message, color, EXPIRY_REMINDER_DURATION);
    }
    
    public void showSharedHourReminder(Player player, String sharer) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("bossbar.shared.hour", "player", sharer);
        
        showBossBar(player, message, BarColor.YELLOW, HOUR_REMINDER_DURATION);
    }
    
    public void showSharedTenMinuteReminder(Player player, String sharer) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("bossbar.shared.ten-minutes", "player", sharer);
        
        showBossBar(player, message, BarColor.RED, TEN_MINUTE_REMINDER_DURATION);
    }
    
    public void showSharedExpiryReminder(Player player, String sharer) {
        if (!plugin.getConfig().getBoolean("bossbar.enabled", true)) {
            return;
        }
        
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("bossbar.shared.expired", "player", sharer);
        
        showBossBar(player, message, BarColor.RED, EXPIRY_REMINDER_DURATION);
    }
    
    private BarColor getBarColor(String configPath, BarColor defaultColor) {
        String colorName = plugin.getConfig().getString(configPath);
        if (colorName == null) {
            return defaultColor;
        }
        
        try {
            return BarColor.valueOf(colorName.toUpperCase());
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("无效的 BossBar 颜色: " + colorName + "，使用默认值");
            return defaultColor;
        }
    }
    
    private void showBossBar(Player player, String message, BarColor color, long durationSeconds) {
        UUID playerUUID = player.getUniqueId();
        
        // 移除现有的 BossBar
        if (playerBossBars.containsKey(playerUUID)) {
            BossBar oldBar = playerBossBars.get(playerUUID);
            oldBar.removeAll();
        }
        
        // 创建新的 BossBar
        BossBar bossBar = Bukkit.createBossBar(message, color, BarStyle.SOLID);
        bossBar.addPlayer(player);
        bossBar.setProgress(1.0);
        
        // 存储 BossBar 和结束时间
        playerBossBars.put(playerUUID, bossBar);
        
        // 设置 BossBar 的元数据（结束时间和总持续时间）
        bossBar.setProgress(1.0);
        player.setMetadata("deathkeep_bossbar_end", new org.bukkit.metadata.FixedMetadataValue(
                plugin, System.currentTimeMillis() + (durationSeconds * 1000)));
        player.setMetadata("deathkeep_bossbar_duration", new org.bukkit.metadata.FixedMetadataValue(
                plugin, durationSeconds * 1000));
    }
    
    private void updateBossBars() {
        long currentTime = System.currentTimeMillis();
        
        for (UUID playerUUID : new HashMap<>(playerBossBars).keySet()) {
            Player player = Bukkit.getPlayer(playerUUID);
            
            if (player == null || !player.isOnline() || !player.hasMetadata("deathkeep_bossbar_end")) {
                // 玩家离线或元数据丢失，移除 BossBar
                BossBar bossBar = playerBossBars.remove(playerUUID);
                if (bossBar != null) {
                    bossBar.removeAll();
                }
                continue;
            }
            
            long endTime = player.getMetadata("deathkeep_bossbar_end").get(0).asLong();
            long duration = player.getMetadata("deathkeep_bossbar_duration").get(0).asLong();
            
            if (currentTime >= endTime) {
                // BossBar 显示时间结束，移除
                BossBar bossBar = playerBossBars.remove(playerUUID);
                bossBar.removeAll();
                player.removeMetadata("deathkeep_bossbar_end", plugin);
                player.removeMetadata("deathkeep_bossbar_duration", plugin);
            } else {
                // 更新 BossBar 进度
                BossBar bossBar = playerBossBars.get(playerUUID);
                double progress = (double) (endTime - currentTime) / duration;
                bossBar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
            }
        }
    }
} 