package org.littlesheep.deathforkeep.tasks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.littlesheep.deathforkeep.utils.Messages;

import java.util.Map;
import java.util.UUID;

public class ReminderTask {

    private final DeathForKeep plugin;
    private BukkitTask task;
    
    // 提醒时间（秒）
    private static final long HOUR_REMINDER = 3600;
    private static final long TEN_MINUTE_REMINDER = 600;
    
    // 已提醒的玩家记录
    private final Map<UUID, Long> hourReminderSent = new java.util.HashMap<>();
    private final Map<UUID, Long> tenMinuteReminderSent = new java.util.HashMap<>();

    public ReminderTask(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        // 每分钟检查一次
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkExpirations, 20L * 60L, 20L * 60L);
    }

    public void cancelTask() {
        if (task != null && !task.isCancelled()) {
            task.cancel();
            task = null;
        }
    }

    private void checkExpirations() {
        long currentTime = System.currentTimeMillis() / 1000;
        Map<UUID, PlayerData> playerDataMap = plugin.getPlayerDataMap();
        Messages messages = plugin.getMessages();

        for (Map.Entry<UUID, PlayerData> entry : playerDataMap.entrySet()) {
            UUID playerUUID = entry.getKey();
            PlayerData data = entry.getValue();
            
            if (!data.isActive()) {
                continue;
            }
            
            long expiryTime = data.getExpiryTime();
            long timeLeft = expiryTime - currentTime;
            
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null && player.isOnline()) {
                // 1小时提醒
                if (timeLeft <= HOUR_REMINDER && timeLeft > TEN_MINUTE_REMINDER) {
                    Long lastReminder = hourReminderSent.get(playerUUID);
                    if (lastReminder == null || currentTime - lastReminder > HOUR_REMINDER) {
                        player.sendMessage(messages.getMessage("reminder.hour"));
                        hourReminderSent.put(playerUUID, currentTime);
                    }
                }
                
                // 10分钟提醒
                if (timeLeft <= TEN_MINUTE_REMINDER && timeLeft > 0) {
                    Long lastReminder = tenMinuteReminderSent.get(playerUUID);
                    if (lastReminder == null || currentTime - lastReminder > TEN_MINUTE_REMINDER) {
                        player.sendMessage(messages.getMessage("reminder.ten-minutes"));
                        tenMinuteReminderSent.put(playerUUID, currentTime);
                    }
                }
                
                // 已过期提醒
                if (timeLeft <= 0) {
                    player.sendMessage(messages.getMessage("reminder.expired"));
                    // 清除提醒记录
                    hourReminderSent.remove(playerUUID);
                    tenMinuteReminderSent.remove(playerUUID);
                }
            }
            
            // 检查共享保护
            UUID sharedWith = data.getSharedWith();
            if (sharedWith != null) {
                Player sharedPlayer = Bukkit.getPlayer(sharedWith);
                if (sharedPlayer != null && sharedPlayer.isOnline()) {
                    // 1小时提醒
                    if (timeLeft <= HOUR_REMINDER && timeLeft > TEN_MINUTE_REMINDER) {
                        Long lastReminder = hourReminderSent.get(sharedWith);
                        if (lastReminder == null || currentTime - lastReminder > HOUR_REMINDER) {
                            sharedPlayer.sendMessage(messages.getMessage("reminder.shared.hour", 
                                    "player", Bukkit.getOfflinePlayer(playerUUID).getName()));
                            hourReminderSent.put(sharedWith, currentTime);
                        }
                    }
                    
                    // 10分钟提醒
                    if (timeLeft <= TEN_MINUTE_REMINDER && timeLeft > 0) {
                        Long lastReminder = tenMinuteReminderSent.get(sharedWith);
                        if (lastReminder == null || currentTime - lastReminder > TEN_MINUTE_REMINDER) {
                            sharedPlayer.sendMessage(messages.getMessage("reminder.shared.ten-minutes", 
                                    "player", Bukkit.getOfflinePlayer(playerUUID).getName()));
                            tenMinuteReminderSent.put(sharedWith, currentTime);
                        }
                    }
                    
                    // 已过期提醒
                    if (timeLeft <= 0) {
                        sharedPlayer.sendMessage(messages.getMessage("reminder.shared.expired", 
                                "player", Bukkit.getOfflinePlayer(playerUUID).getName()));
                        // 清除提醒记录
                        hourReminderSent.remove(sharedWith);
                        tenMinuteReminderSent.remove(sharedWith);
                    }
                }
            }
        }
    }
} 