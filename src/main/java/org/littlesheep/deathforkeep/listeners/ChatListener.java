package org.littlesheep.deathforkeep.listeners;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.MetadataValue;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.PlayerData;

import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {
    
    private final DeathForKeep plugin;
    
    public ChatListener(DeathForKeep plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        
        // 检查玩家是否在等待输入时长
        if (player.hasMetadata("dk_bulk_mode")) {
            event.setCancelled(true); // 取消消息广播
            
            String message = event.getMessage().trim();
            
            // 如果输入"cancel"或"取消"，终止操作
            if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("取消")) {
                player.removeMetadata("dk_bulk_mode", plugin);
                player.removeMetadata("dk_bulk_players", plugin);
                player.sendMessage(plugin.getMessages().getMessage("command.cancelled"));
                return;
            }
            
            // 解析时长输入
            int days;
            try {
                days = Integer.parseInt(message);
                if (days <= 0) {
                    player.sendMessage(plugin.getMessages().getMessage("command.invalid-duration"));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessages().getMessage("command.invalid-duration"));
                return;
            }
            
            // 获取操作类型和玩家列表
            final String operation;
            List<MetadataValue> modeValues = player.getMetadata("dk_bulk_mode");
            if (!modeValues.isEmpty()) {
                operation = modeValues.get(0).asString();
            } else {
                operation = null;
            }
            
            final String playersStr;
            List<MetadataValue> playersValues = player.getMetadata("dk_bulk_players");
            if (!playersValues.isEmpty()) {
                playersStr = playersValues.get(0).asString();
            } else {
                playersStr = null;
            }
            
            if (operation == null || playersStr == null) {
                player.sendMessage(plugin.getMessages().getMessage("command.error"));
                player.removeMetadata("dk_bulk_mode", plugin);
                player.removeMetadata("dk_bulk_players", plugin);
                return;
            }
            
            // 将变量声明为final
            final String finalOperation = operation;
            final String finalPlayersStr = playersStr;
            
            // 异步处理，然后在主线程执行操作
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.performCommand("dk bulk " + finalOperation + " " + days + "d " + finalPlayersStr);
                
                // 移除元数据
                player.removeMetadata("dk_bulk_mode", plugin);
                player.removeMetadata("dk_bulk_players", plugin);
            });
        }
        
        // 检查管理员是否在等待输入添加时长
        else if (player.hasMetadata("dk_admin_add_target")) {
            event.setCancelled(true);
            
            String message = event.getMessage().trim();
            
            // 如果输入"cancel"或"取消"，终止操作
            if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("取消")) {
                player.removeMetadata("dk_admin_add_target", plugin);
                player.sendMessage(plugin.getMessages().getMessage("command.cancelled"));
                return;
            }
            
            // 解析时长输入
            int days;
            try {
                days = Integer.parseInt(message);
                if (days <= 0) {
                    player.sendMessage(plugin.getMessages().getMessage("command.invalid-duration"));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessages().getMessage("command.invalid-duration"));
                return;
            }
            
            // 获取目标玩家UUID
            String targetUUIDStr = player.getMetadata("dk_admin_add_target").get(0).asString();
            UUID targetUUID;
            try {
                targetUUID = UUID.fromString(targetUUIDStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessages().getMessage("command.error"));
                player.removeMetadata("dk_admin_add_target", plugin);
                return;
            }
            
            // 在主线程执行操作
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 添加保护时长
                int seconds = days * 86400; // 天数转为秒
                plugin.addProtection(targetUUID, seconds);
                
                // 发送成功消息
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
                String targetName = target.getName() != null ? target.getName() : targetUUID.toString();
                player.sendMessage(plugin.getMessages().getMessage("command.admin.protection-added", 
                        "player", targetName, 
                        "days", String.valueOf(days)));
                
                // 如果目标玩家在线，给他们发送通知
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(plugin.getMessages().getMessage("command.admin.received-protection", 
                            "days", String.valueOf(days)));
                    
                    // 显示获得保护的粒子效果
                    if (target.getPlayer() != null) {
                        plugin.getGuiManager().showProtectionEffects(target.getPlayer(), true);
                    }
                }
                
                // 移除元数据
                player.removeMetadata("dk_admin_add_target", plugin);
            });
        }
        
        // 检查管理员是否在等待输入移除时长
        else if (player.hasMetadata("dk_admin_remove_target")) {
            event.setCancelled(true);
            
            String message = event.getMessage().trim();
            
            // 如果输入"cancel"或"取消"，终止操作
            if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("取消")) {
                player.removeMetadata("dk_admin_remove_target", plugin);
                player.sendMessage(plugin.getMessages().getMessage("command.cancelled"));
                return;
            }
            
            // 解析时长输入
            int days;
            try {
                days = Integer.parseInt(message);
                if (days <= 0) {
                    player.sendMessage(plugin.getMessages().getMessage("command.invalid-duration"));
                    return;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(plugin.getMessages().getMessage("command.invalid-duration"));
                return;
            }
            
            // 获取目标玩家UUID
            String targetUUIDStr = player.getMetadata("dk_admin_remove_target").get(0).asString();
            UUID targetUUID;
            try {
                targetUUID = UUID.fromString(targetUUIDStr);
            } catch (IllegalArgumentException e) {
                player.sendMessage(plugin.getMessages().getMessage("command.error"));
                player.removeMetadata("dk_admin_remove_target", plugin);
                return;
            }
            
            // 在主线程执行操作
            Bukkit.getScheduler().runTask(plugin, () -> {
                // 获取玩家数据
                PlayerData data = plugin.getPlayerData(targetUUID);
                if (data == null || !data.isActive()) {
                    player.sendMessage(plugin.getMessages().getMessage("command.admin.no-protection"));
                    player.removeMetadata("dk_admin_remove_target", plugin);
                    return;
                }
                
                // 移除保护时长
                int seconds = days * 86400; // 天数转为秒
                long currentTime = System.currentTimeMillis() / 1000;
                long newExpiry = Math.max(currentTime, data.getExpiryTime() - seconds);
                data.setExpiryTime(newExpiry);
                plugin.savePlayerData(targetUUID);
                
                // 发送成功消息
                OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
                String targetName = target.getName() != null ? target.getName() : targetUUID.toString();
                player.sendMessage(plugin.getMessages().getMessage("command.admin.protection-removed", 
                        "player", targetName, 
                        "days", String.valueOf(days)));
                
                // 如果目标玩家在线，给他们发送通知
                if (target.isOnline() && target.getPlayer() != null) {
                    target.getPlayer().sendMessage(plugin.getMessages().getMessage("command.admin.lost-protection", 
                            "days", String.valueOf(days)));
                    
                    // 显示失去保护的粒子效果
                    if (data.getExpiryTime() <= currentTime && target.getPlayer() != null) {
                        plugin.getGuiManager().showProtectionEffects(target.getPlayer(), false);
                    }
                }
                
                // 移除元数据
                player.removeMetadata("dk_admin_remove_target", plugin);
            });
        }
    }
} 