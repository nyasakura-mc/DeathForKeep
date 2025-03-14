package org.littlesheep.deathforkeep.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.metadata.MetadataValue;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.utils.Messages;

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
    }
} 