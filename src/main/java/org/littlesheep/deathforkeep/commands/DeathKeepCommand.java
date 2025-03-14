package org.littlesheep.deathforkeep.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.littlesheep.deathforkeep.utils.Messages;

import java.text.SimpleDateFormat;
import java.util.*;

public class DeathKeepCommand implements CommandExecutor, TabCompleter {

    private final DeathForKeep plugin;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Map<UUID, Long> resetConfirmations = new HashMap<>();

    public DeathKeepCommand(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Messages messages = plugin.getMessages();
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(messages.getMessage("command.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0 || args[0].equalsIgnoreCase("gui")) {
            // 打开主菜单
            plugin.getGuiManager().openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
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
            default:
                sender.sendMessage(messages.getMessage("command.unknown"));
                return true;
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
        
        if (args.length > 1 && sender.hasPermission("deathkeep.check.others")) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(messages.getMessage("command.player-not-found", "player", args[1]));
                return true;
            }
            checkProtection(sender, target.getUniqueId(), target.getName());
        } else if (sender instanceof Player) {
            Player player = (Player) sender;
            if (!player.hasPermission("deathkeep.check")) {
                player.sendMessage(messages.getMessageWithPrefix("command.no-permission"));
                return true;
            }
            checkProtection(sender, player.getUniqueId(), player.getName());
        } else {
            sender.sendMessage(messages.getMessage("command.check.specify-player"));
        }
        return true;
    }

    private void checkProtection(CommandSender sender, UUID playerUUID, String playerName) {
        Messages messages = plugin.getMessages();
        Map<UUID, PlayerData> playerDataMap = plugin.getPlayerDataMap();
        
        if (playerDataMap.containsKey(playerUUID)) {
            PlayerData data = playerDataMap.get(playerUUID);
            long expiryTime = data.getExpiryTime();
            long currentTime = System.currentTimeMillis() / 1000;
            
            if (expiryTime > currentTime) {
                Date date = new Date(expiryTime * 1000);
                sender.sendMessage(messages.getMessage("command.check.active", 
                        "player", playerName,
                        "time", dateFormat.format(date)));
                
                // 显示粒子效果状态
                String particlesStatus = data.isParticlesEnabled() ? 
                        messages.getMessage("command.check.particles-enabled") : 
                        messages.getMessage("command.check.particles-disabled");
                sender.sendMessage(particlesStatus);
                
                // 显示共享状态
                if (data.getSharedWith() != null) {
                    String sharedName = Bukkit.getOfflinePlayer(data.getSharedWith()).getName();
                    if (sharedName != null) {
                        sender.sendMessage(messages.getMessage("command.check.shared-with", "player", sharedName));
                    }
                }
            } else {
                sender.sendMessage(messages.getMessage("command.check.expired", "player", playerName));
            }
        } else {
            sender.sendMessage(messages.getMessage("command.check.no-protection", "player", playerName));
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
} 