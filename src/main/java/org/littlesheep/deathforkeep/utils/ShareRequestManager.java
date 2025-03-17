/*
  共享请求管理器
  处理共享请求的创建、接受和拒绝
 */
package org.littlesheep.deathforkeep.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.gui.ConfirmGUI;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ShareRequestManager {
    
    private final DeathForKeep plugin;
    private final Map<UUID, ShareRequest> pendingRequests = new HashMap<>();
    
    public ShareRequestManager(DeathForKeep plugin) {
        this.plugin = plugin;
    }
    
    public void sendShareRequest(Player sender, Player target) {
        UUID senderUUID = sender.getUniqueId();
        UUID targetUUID = target.getUniqueId();
        Messages messages = plugin.getMessages();
        
        // 检查发送者是否有有效的保护
        if (!plugin.hasActiveProtection(senderUUID)) {
            sender.sendMessage(messages.getMessage("share.no-protection"));
            return;
        }
        
        // 检查是否已经在共享
        if (plugin.isSharing(senderUUID, targetUUID)) {
            sender.sendMessage(messages.getMessage("share.already-shared", 
                    "player", target.getName()));
            return;
        }
        
        // 计算手续费
        double fee = calculateShareFee(senderUUID);
        
        // 检查是否有足够的钱支付手续费
        if (fee > 0 && !plugin.getEconomy().has(sender, fee)) {
            sender.sendMessage(messages.getMessage("share.not-enough-money", 
                    "fee", String.valueOf(fee)));
            return;
        }
        
        // 创建共享请求
        ShareRequest request = new ShareRequest(senderUUID, targetUUID, fee);
        pendingRequests.put(targetUUID, request);
        
        // 发送请求消息
        sender.sendMessage(messages.getMessage("share.request-sent", 
                "player", target.getName(), 
                "fee", String.valueOf(fee)));
        
        target.sendMessage(messages.getMessage("share.request-received", 
                "player", sender.getName()));
        
        // 发送确认按钮
        sendConfirmationButtons(target, sender.getName());
        
        // 设置请求过期任务
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(targetUUID) && 
                    pendingRequests.get(targetUUID).equals(request)) {
                pendingRequests.remove(targetUUID);
                
                Player senderPlayer = Bukkit.getPlayer(senderUUID);
                Player targetPlayer = Bukkit.getPlayer(targetUUID);
                
                if (senderPlayer != null && senderPlayer.isOnline()) {
                    senderPlayer.sendMessage(messages.getMessage("share.request-expired", 
                            "player", target.getName()));
                }
                
                if (targetPlayer != null && targetPlayer.isOnline()) {
                    targetPlayer.sendMessage(messages.getMessage("share.request-expired-target", 
                            "player", sender.getName()));
                }
            }
        }, 20L * 60); // 60秒后过期
    }
    
    public void acceptRequest(Player target) {
        UUID targetUUID = target.getUniqueId();
        
        if (!pendingRequests.containsKey(targetUUID)) {
            target.sendMessage(plugin.getMessages().getMessage("share.no-pending-request"));
            return;
        }
        
        ShareRequest request = pendingRequests.remove(targetUUID);
        UUID senderUUID = request.getSenderUUID();
        double fee = request.getFee();
        
        Player sender = Bukkit.getPlayer(senderUUID);
        Messages messages = plugin.getMessages();
        
        // 检查发送者是否仍然在线
        if (sender == null || !sender.isOnline()) {
            target.sendMessage(messages.getMessage("share.sender-offline"));
            return;
        }
        
        // 检查发送者是否仍然有有效的保护
        if (!plugin.hasActiveProtection(senderUUID)) {
            target.sendMessage(messages.getMessage("share.protection-expired"));
            sender.sendMessage(messages.getMessage("share.protection-expired-sender"));
            return;
        }
        
        // 收取手续费
        if (fee > 0) {
            plugin.getEconomy().withdrawPlayer(sender, fee);
            sender.sendMessage(messages.getMessage("share.fee-paid", 
                    "fee", String.valueOf(fee)));
        }
        
        // 执行共享
        plugin.shareProtection(senderUUID, targetUUID);
        
        // 发送成功消息
        sender.sendMessage(messages.getMessage("share.success-sender", 
                "player", target.getName()));
        
        target.sendMessage(messages.getMessage("share.success-target", 
                "player", sender.getName()));
        
        // 获取保护到期时间
        long expiryTime = plugin.getPlayerData(senderUUID).getExpiryTime();
        
        // 显示BossBar
        plugin.getBossBarManager().showProtectionSharedMessage(target, sender.getName(), expiryTime);
        
        // 播放共享成功的粒子效果
        if (plugin.getConfig().getBoolean("particles.share-effect", true)) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                ParticleUtils.playShareEffect(sender, target);
            });
        }
    }
    
    public void denyRequest(Player target) {
        UUID targetUUID = target.getUniqueId();
        
        if (!pendingRequests.containsKey(targetUUID)) {
            target.sendMessage(plugin.getMessages().getMessage("share.no-pending-request"));
            return;
        }
        
        ShareRequest request = pendingRequests.remove(targetUUID);
        UUID senderUUID = request.getSenderUUID();
        
        Player sender = Bukkit.getPlayer(senderUUID);
        Messages messages = plugin.getMessages();
        
        // 发送拒绝消息
        target.sendMessage(messages.getMessage("share.denied-target"));
        
        if (sender != null && sender.isOnline()) {
            sender.sendMessage(messages.getMessage("share.denied-sender", 
                    "player", target.getName()));
        }
    }
    
    private double calculateShareFee(UUID playerUUID) {
        double feePercentage = plugin.getConfig().getDouble("share.fee-percentage", 10.0);
        
        if (feePercentage <= 0) {
            return 0;
        }
        
        // 获取玩家的保护价值
        long remainingSeconds = plugin.getRemainingSeconds(playerUUID);
        double pricePerDay = plugin.getConfig().getDouble("price-per-day", 1000);
        double protectionValue = (pricePerDay / 86400) * remainingSeconds;
        
        // 计算手续费
        return protectionValue * (feePercentage / 100.0);
    }
    
    private void sendConfirmationButtons(Player target, String senderName) {
        Messages messages = plugin.getMessages();
        
        // 创建确认GUI
        new ConfirmGUI(plugin, messages.getMessage("share.confirm-title", "player", senderName),
                messages.getMessage("share.confirm-accept"),
                messages.getMessage("share.confirm-deny"),
                () -> acceptRequest(target),
                () -> denyRequest(target)).open(target);
    }
    
    public boolean hasPendingRequest(UUID playerUUID) {
        return pendingRequests.containsKey(playerUUID);
    }
    
    public static class ShareRequest {
        private final UUID senderUUID;
        private final UUID targetUUID;
        private final double fee;
        private final long timestamp;
        
        public ShareRequest(UUID senderUUID, UUID targetUUID, double fee) {
            this.senderUUID = senderUUID;
            this.targetUUID = targetUUID;
            this.fee = fee;
            this.timestamp = System.currentTimeMillis();
        }
        
        public UUID getSenderUUID() {
            return senderUUID;
        }
        
        public UUID getTargetUUID() {
            return targetUUID;
        }
        
        public double getFee() {
            return fee;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            ShareRequest that = (ShareRequest) obj;
            return senderUUID.equals(that.senderUUID) && 
                   targetUUID.equals(that.targetUUID);
        }
        
        @Override
        public int hashCode() {
            return 31 * senderUUID.hashCode() + targetUUID.hashCode();
        }
    }
} 