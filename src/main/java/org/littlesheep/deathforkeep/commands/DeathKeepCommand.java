package org.littlesheep.deathforkeep.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.littlesheep.deathforkeep.utils.Messages;
import org.bukkit.ChatColor;

import java.util.*;

public class DeathKeepCommand implements CommandExecutor, TabCompleter {

    private final DeathForKeep plugin;
    private final Map<UUID, Long> resetConfirmations = new HashMap<>();

    public DeathKeepCommand(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (args.length == 0) {
            return handleHelp(sender);
        }
        
        switch (args[0].toLowerCase()) {
            case "help":
                return handleHelp(sender);
            case "bulk":
                if (args.length >= 2 && ("add".equals(args[1]) || "remove".equals(args[1]))) {
                    return handleBulk(sender, args);
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        messages.getMessage("command.bulk.usage")));
                    return true;
                }
            case "buy":
                return handleBuy(sender, args);
            case "check":
                return handleCheck(sender, args);
            case "add":
                return handleAdd(sender, args);
            case "remove":
                return handleRemove(sender, args);
            case "reload":
                return handleReload(sender);
            case "particles":
                return handleParticles(sender);
            case "share":
                return handleShare(sender, args);
            case "find":
                return handleFind(sender, args);
            case "resetall":
                return handleResetAll(sender);
            case "gui":
                return handleGui(sender);    
            default:
                sender.sendMessage(messages.getMessage("command.unknown"));
                return true;
        }
    }

    private boolean handleHelp(CommandSender sender) {
        Messages messages = plugin.getMessages();
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', messages.getMessage("commands.help.title")));
        
        List<String> helpLines = messages.getMessageList("commands.help.lines");
        for (String line : helpLines) {
            // 检查是否是管理员命令行，如果不是管理员则不显示
            if (line.contains("/dk bulk") && !sender.hasPermission("deathkeep.admin")) {
                continue;
            }
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', line));
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private boolean handleBulk(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!sender.hasPermission("deathkeep.admin")) {
            sender.sendMessage(messages.getMessage("command.no-permission"));
            return true;
        }
        
        if (args.length < 4) {
            sender.sendMessage(messages.getMessage("command.bulk.usage"));
            return true;
        }
        
        String operation = args[1].toLowerCase();
        String durationStr = args[2];
        String playersStr = args[3];
        
        // 解析时长
        int seconds;
        try {
            seconds = parseDuration(durationStr);
        } catch (IllegalArgumentException e) {
            sender.sendMessage(messages.getMessage("command.invalid-duration"));
            return true;
        }
        
        // 解析玩家列表
        String[] playerNames = playersStr.split(",");
        List<UUID> affectedPlayers = new ArrayList<>();
        List<String> failedPlayers = new ArrayList<>();
        
        for (String name : playerNames) {
            UUID uuid = null;
            // 尝试直接获取在线玩家
            Player target = Bukkit.getPlayer(name);
            if (target != null) {
                uuid = target.getUniqueId();
            } else {
                // 尝试获取离线玩家
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                if (offlinePlayer.hasPlayedBefore()) {
                    uuid = offlinePlayer.getUniqueId();
                }
            }
            
            if (uuid != null) {
                affectedPlayers.add(uuid);
            } else {
                failedPlayers.add(name);
            }
        }
        
        if (affectedPlayers.isEmpty()) {
            sender.sendMessage(messages.getMessage("command.bulk.no-players-found"));
            return true;
        }
        
        // 执行批量操作
        if ("add".equals(operation)) {
            for (UUID uuid : affectedPlayers) {
                addProtectionDuration(uuid, seconds);
            }
            sender.sendMessage(messages.getMessage("command.bulk.add-success")
                    .replace("%count%", String.valueOf(affectedPlayers.size()))
                    .replace("%duration%", formatDuration(seconds)));
        } else if ("remove".equals(operation)) {
            for (UUID uuid : affectedPlayers) {
                removeProtectionDuration(uuid, seconds);
            }
            sender.sendMessage(messages.getMessage("command.bulk.remove-success")
                    .replace("%count%", String.valueOf(affectedPlayers.size()))
                    .replace("%duration%", formatDuration(seconds)));
        } else {
            sender.sendMessage(messages.getMessage("command.bulk.invalid-operation"));
            return true;
        }
        
        // 报告未找到的玩家
        if (!failedPlayers.isEmpty()) {
            sender.sendMessage(messages.getMessage("command.bulk.failed-players")
                    .replace("%players%", String.join(", ", failedPlayers)));
        }
        
        return true;
    }

    private void addProtectionDuration(UUID uuid, int seconds) {
        PlayerData data = plugin.getPlayerData(uuid);
        long currentTime = System.currentTimeMillis() / 1000;
        long newExpiry;
        
        if (data == null) {
            // 如果玩家数据不存在，创建新的数据
            data = new PlayerData(uuid, currentTime + seconds, true, null);
            plugin.getPlayerDataMap().put(uuid, data);
        } else if (data.isActive()) {
            // 如果当前有保护，增加时长
            newExpiry = data.getExpiryTime() + seconds;
            data.setExpiryTime(newExpiry);
        } else {
            // 如果当前无保护，从现在开始计时
            newExpiry = currentTime + seconds;
            data.setExpiryTime(newExpiry);
        }
        
        plugin.savePlayerData(uuid);
    }

    private void removeProtectionDuration(UUID uuid, int seconds) {
        PlayerData data = plugin.getPlayerData(uuid);
        long currentTime = System.currentTimeMillis() / 1000;
        
        if (data == null) {
            // 如果玩家数据不存在，跳过
            return;
        }
        
        if (data.isActive()) {
            // 只有当前有保护时才减少
            long newExpiry = Math.max(currentTime, data.getExpiryTime() - seconds);
            data.setExpiryTime(newExpiry);
            plugin.savePlayerData(uuid);
        }
    }

    private int parseDuration(String duration) {
        // 示例: 1d, 7h, 30m, 60s
        if (duration.isEmpty()) {
            throw new IllegalArgumentException("持续时间不能为空");
        }
        
        char unit = duration.charAt(duration.length() - 1);
        String numStr = duration.substring(0, duration.length() - 1);
        
        try {
            int value = Integer.parseInt(numStr);
            switch (unit) {
                case 'd': return value * 86400;
                case 'h': return value * 3600;
                case 'm': return value * 60;
                case 's': return value;
                default:
                    // 尝试直接解析数字（秒）
                    return Integer.parseInt(duration);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("无效的持续时间格式");
        }
    }

    private String formatDuration(int seconds) {
        if (seconds >= 86400 && seconds % 86400 == 0) {
            return (seconds / 86400) + "天";
        } else if (seconds >= 3600 && seconds % 3600 == 0) {
            return (seconds / 3600) + "小时";
        } else if (seconds >= 60 && seconds % 60 == 0) {
            return (seconds / 60) + "分钟";
        } else {
            return seconds + "秒";
        }
    }

    private boolean handleBuy(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("command.player-only"));
            return true;
        }

        Player player = (Player) sender;
        if (!player.hasPermission("deathkeep.buy")) {
            player.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }

        int days;
        if (args.length > 1) {
            try {
                days = Integer.parseInt(args[1]);
                if (days <= 0) {
                    player.sendMessage(messages.getMessage("command.buy.invalid-days"));
                    return true;
                }
            } catch (NumberFormatException e) {
                player.sendMessage(messages.getMessage("command.buy.invalid-days"));
                return true;
            }
        } else {
            days = 1; // 默认1天
        }

        int seconds = days * 86400;
        double pricePerDay = plugin.getWorldPrice(player.getWorld());
        double totalPrice = pricePerDay * days;
        Economy economy = plugin.getEconomy();

        if (economy.getBalance(player) < totalPrice) {
            player.sendMessage(messages.getMessage("command.buy.not-enough-money", 
                    "price", String.format("%.2f", totalPrice)));
            return true;
        }

        economy.withdrawPlayer(player, totalPrice);
        plugin.addProtection(player.getUniqueId(), seconds);
        player.sendMessage(messages.getMessage("command.buy.success", 
                "days", String.valueOf(days),
                "price", String.format("%.2f", totalPrice)));
        return true;
    }

    private boolean handleCheck(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        // 默认检查自己
        UUID uuid;
        String playerName;
        
        if (args.length > 1) {
            // 检查其他玩家
            String name = args[1];
            Player target = Bukkit.getPlayer(name);
            
            if (target != null) {
                uuid = target.getUniqueId();
                playerName = target.getName();
            } else {
                @SuppressWarnings("deprecation")
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(name);
                
                if (offlinePlayer.hasPlayedBefore()) {
                    uuid = offlinePlayer.getUniqueId();
                    playerName = offlinePlayer.getName();
                } else {
                    sender.sendMessage(messages.getMessage("command.player-not-found", "player", name));
                    return true;
                }
            }
        } else if (sender instanceof Player) {
            // 检查自己
            Player player = (Player) sender;
            uuid = player.getUniqueId();
            playerName = player.getName();
        } else {
            sender.sendMessage(messages.getMessage("command.console-specify-player"));
            return true;
        }
        
        // 检查直接拥有的保护
        PlayerData data = plugin.getPlayerData(uuid);
        boolean hasDirectProtection = data != null && data.isActive();
        
        // 检查共享获得的保护
        boolean hasSharedProtection = false;
        String sharerName = "";
        
        for (Map.Entry<UUID, PlayerData> entry : plugin.getPlayerDataMap().entrySet()) {
            PlayerData sharerData = entry.getValue();
            if (sharerData.isActive() && uuid.equals(sharerData.getSharedWith())) {
                hasSharedProtection = true;
                // 获取分享者的名字
                OfflinePlayer sharerPlayer = Bukkit.getOfflinePlayer(entry.getKey());
                sharerName = sharerPlayer.getName() != null ? sharerPlayer.getName() : "未知玩家";
                break;
            }
        }
        
        if (hasDirectProtection) {
            @SuppressWarnings("null")
            long timeLeft = data.getExpiryTime() - System.currentTimeMillis() / 1000;
            sender.sendMessage(messages.getMessage("command.check.has-protection", 
                    "player", playerName, 
                    "time", formatTime((int)timeLeft)));
            return true;
        } else if (hasSharedProtection) {
            sender.sendMessage(messages.getMessage("command.check.has-shared-protection", 
                    "player", playerName, 
                    "sharer", sharerName));
            return true;
        } else {
            sender.sendMessage(messages.getMessage("command.check.no-protection", 
                    "player", playerName));
            return true;
        }
    }

    private boolean handleAdd(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!sender.hasPermission("deathkeep.admin")) {
            sender.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(messages.getMessage("command.add.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(messages.getMessage("command.player-not-found", "player", args[1]));
            return true;
        }

        int days;
        try {
            days = Integer.parseInt(args[2]);
            if (days <= 0) {
                sender.sendMessage(messages.getMessage("command.add.invalid-days"));
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(messages.getMessage("command.add.invalid-days"));
            return true;
        }

        plugin.addProtection(target.getUniqueId(), days * 86400);
        sender.sendMessage(messages.getMessage("command.add.success", 
                "player", target.getName(),
                "days", String.valueOf(days)));
        target.sendMessage(messages.getMessage("command.add.notify", "days", String.valueOf(days)));
        return true;
    }

    private boolean handleRemove(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!sender.hasPermission("deathkeep.admin")) {
            sender.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(messages.getMessage("command.remove.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(messages.getMessage("command.player-not-found", "player", args[1]));
            return true;
        }

        plugin.removeProtection(target.getUniqueId());
        sender.sendMessage(messages.getMessage("command.remove.success", "player", target.getName()));
        target.sendMessage(messages.getMessage("command.remove.notify"));
        return true;
    }

    private boolean handleReload(CommandSender sender) {
        Messages messages = plugin.getMessages();
        
        if (!sender.hasPermission("deathkeep.admin")) {
            sender.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }

        plugin.reloadPluginData();
        sender.sendMessage(messages.getMessage("command.reload.success"));
        return true;
    }
    
    private boolean handleParticles(CommandSender sender) {
        Messages messages = plugin.getMessages();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("command.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("deathkeep.particles")) {
            player.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }
        
        UUID playerUUID = player.getUniqueId();
        boolean currentStatus = plugin.areParticlesEnabled(playerUUID);
        boolean newStatus = !currentStatus;
        
        plugin.setParticlesEnabled(playerUUID, newStatus);
        
        if (newStatus) {
            player.sendMessage(messages.getMessage("command.particles.enabled"));
        } else {
            player.sendMessage(messages.getMessage("command.particles.disabled"));
        }
        
        return true;
    }
    
    private boolean handleShare(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("command.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        if (!player.hasPermission("deathkeep.share")) {
            player.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            player.sendMessage(messages.getMessage("command.share.usage"));
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            player.sendMessage(messages.getMessage("command.player-not-found", "player", args[1]));
            return true;
        }
        
        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(messages.getMessage("command.share.self"));
            return true;
        }
        
        UUID playerUUID = player.getUniqueId();
        PlayerData data = plugin.getPlayerDataMap().get(playerUUID);
        
        if (data == null || !data.isActive()) {
            player.sendMessage(messages.getMessage("command.share.no-protection"));
            return true;
        }
        
        // 检查是否已经有共享
        if (data.getSharedWith() != null) {
            String currentSharedName = Bukkit.getOfflinePlayer(data.getSharedWith()).getName();
            player.sendMessage(messages.getMessage("command.share.already-shared", 
                    "player", currentSharedName != null ? currentSharedName : "未知玩家"));
            return true;
        }
        
        // 设置共享
        plugin.shareProtection(playerUUID, target.getUniqueId());
        
        player.sendMessage(messages.getMessage("command.share.success", "player", target.getName()));
        target.sendMessage(messages.getMessage("command.share.notify", "player", player.getName()));
        
        return true;
    }
    
    private boolean handleFind(CommandSender sender, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!sender.hasPermission("deathkeep.admin")) {
            sender.sendMessage(messages.getMessage("command.no-permission"));
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(messages.getMessage("command.find.usage"));
            return true;
        }
        
        String playerName = args[1];
        Player target = Bukkit.getPlayer(playerName);
        UUID targetUUID = null;
        
        if (target != null) {
            targetUUID = target.getUniqueId();
        } else {
            // 尝试查找离线玩家
            @SuppressWarnings("deprecation")
            UUID offlineUUID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
            if (offlineUUID != null) {
                targetUUID = offlineUUID;
            }
        }
        
        if (targetUUID == null) {
            sender.sendMessage(messages.getMessage("command.player-not-found", "player", playerName));
            return true;
        }
        
        // 检查玩家自己的保护
        checkProtection(sender, targetUUID, playerName);
        
        // 检查是否有其他玩家与此玩家共享保护
        boolean foundSharing = false;
        for (PlayerData otherData : plugin.getPlayerDataMap().values()) {
            if (targetUUID.equals(otherData.getSharedWith()) && otherData.isActive()) {
                String sharerName = Bukkit.getOfflinePlayer(otherData.getUuid()).getName();
                if (sharerName != null) {
                    sender.sendMessage(messages.getMessage("command.find.shared-by", "player", sharerName));
                    foundSharing = true;
                }
            }
        }
        
        if (!foundSharing) {
            sender.sendMessage(messages.getMessage("command.find.no-sharing"));
        }
        
        return true;
    }
    
    private boolean handleResetAll(CommandSender sender) {
        Messages messages = plugin.getMessages();
        
        if (!sender.hasPermission("deathkeep.admin")) {
            sender.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
            return true;
        }
        
        if (!(sender instanceof Player)) {
            // 控制台直接重置
            plugin.resetAllData();
            sender.sendMessage(messages.getMessage("command.resetall.success"));
            return true;
        }
        
        Player player = (Player) sender;
        UUID playerUUID = player.getUniqueId();
        
        // 检查是否需要确认
        if (resetConfirmations.containsKey(playerUUID) && 
                resetConfirmations.get(playerUUID) > System.currentTimeMillis() - 30000) { // 30秒内确认
            
            // 执行重置
            plugin.resetAllData();
            resetConfirmations.remove(playerUUID);
            sender.sendMessage(messages.getMessage("command.resetall.success"));
            
        } else {
            // 需要确认
            resetConfirmations.put(playerUUID, System.currentTimeMillis());
            sender.sendMessage(messages.getMessage("command.resetall.confirm"));
        }
        
        return true;
    }

    private boolean handleGui(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessages().getMessage("command.player-only"));
            return true;
        }
        
        Player player = (Player) sender;
        plugin.getGuiManager().openMainMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = new ArrayList<>();
            if (sender.hasPermission("deathkeep.buy")) subCommands.add("buy");
            if (sender.hasPermission("deathkeep.check")) subCommands.add("check");
            if (sender.hasPermission("deathkeep.particles")) subCommands.add("particles");
            if (sender.hasPermission("deathkeep.share")) subCommands.add("share");
            if (sender.hasPermission("deathkeep.admin")) {
                subCommands.add("add");
                subCommands.add("remove");
                subCommands.add("find");
                subCommands.add("resetall");
                subCommands.add("reload");
            }
            
            for (String subCommand : subCommands) {
                if (subCommand.startsWith(args[0].toLowerCase())) {
                    completions.add(subCommand);
                }
            }
        } else if (args.length == 2) {
            if ((args[0].equalsIgnoreCase("check") && sender.hasPermission("deathkeep.check.others")) ||
                (args[0].equalsIgnoreCase("add") && sender.hasPermission("deathkeep.admin")) ||
                (args[0].equalsIgnoreCase("remove") && sender.hasPermission("deathkeep.admin")) ||
                (args[0].equalsIgnoreCase("find") && sender.hasPermission("deathkeep.admin")) ||
                (args[0].equalsIgnoreCase("share") && sender.hasPermission("deathkeep.share"))) {
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(player.getName());
                    }
                }
            } else if (args[0].equalsIgnoreCase("buy") && sender.hasPermission("deathkeep.buy")) {
                completions.add("1");
                completions.add("7");
                completions.add("30");
            }
        }
        
        return completions;
    }

    // 添加格式化时间的方法
    private String formatTime(int seconds) {
        if (seconds < 60) {
            return seconds + "秒";
        }
        
        long minutes = seconds / 60;
        if (minutes < 60) {
            return minutes + "分钟";
        }
        
        long hours = minutes / 60;
        if (hours < 24) {
            return hours + "小时";
        }
        
        long days = hours / 24;
        hours = hours % 24;
        
        if (hours == 0) {
            return days + "天";
        } else {
            return days + "天" + hours + "小时";
        }
    }

    private void checkProtection(CommandSender sender, UUID uuid, String playerName) {
        PlayerData data = plugin.getPlayerData(uuid);
        Messages messages = plugin.getMessages();
        
        boolean hasDirectProtection = data != null && data.isActive();
        
        // 检查共享获得的保护
        boolean hasSharedProtection = false;
        String sharerName = "";
        
        for (Map.Entry<UUID, PlayerData> entry : plugin.getPlayerDataMap().entrySet()) {
            PlayerData sharerData = entry.getValue();
            if (sharerData.isActive() && uuid.equals(sharerData.getSharedWith())) {
                hasSharedProtection = true;
                OfflinePlayer sharerPlayer = Bukkit.getOfflinePlayer(entry.getKey());
                sharerName = sharerPlayer.getName() != null ? sharerPlayer.getName() : "未知玩家";
                break;
            }
        }
        
        if (hasDirectProtection) {
            @SuppressWarnings("null")
            long timeLeft = data.getExpiryTime() - System.currentTimeMillis() / 1000;
            sender.sendMessage(messages.getMessage("command.check.has-protection", 
                    "player", playerName, 
                    "time", formatTime((int)timeLeft)));
        } else if (hasSharedProtection) {
            sender.sendMessage(messages.getMessage("command.check.has-shared-protection", 
                    "player", playerName, 
                    "sharer", sharerName));
        } else {
            sender.sendMessage(messages.getMessage("command.check.no-protection", 
                    "player", playerName));
        }
    }
} 