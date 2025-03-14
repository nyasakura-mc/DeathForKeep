package org.littlesheep.deathforkeep.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.littlesheep.deathforkeep.utils.Messages;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class JoinListener implements Listener {

    private final DeathForKeep plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public JoinListener(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataMap().get(playerUUID);
        Messages messages = plugin.getMessages();
        
        if (data != null) {
            long expiryTime = data.getExpiryTime();
            long currentTime = System.currentTimeMillis() / 1000;
            
            if (expiryTime > currentTime) {
                // 保护仍然有效
                Date date = new Date(expiryTime * 1000);
                player.sendMessage(messages.getMessage("join.protection-active", 
                        "time", dateFormat.format(date)));
            } else {
                // 保护已过期
                player.sendMessage(messages.getMessage("join.protection-expired"));
            }
        }
        
        // 检查是否有其他玩家与此玩家共享保护
        for (PlayerData otherData : plugin.getPlayerDataMap().values()) {
            if (playerUUID.equals(otherData.getSharedWith()) && otherData.isActive()) {
                player.sendMessage(messages.getMessage("join.shared-protection", 
                        "player", plugin.getServer().getOfflinePlayer(otherData.getUuid()).getName()));
                break;
            }
        }
    }
} 