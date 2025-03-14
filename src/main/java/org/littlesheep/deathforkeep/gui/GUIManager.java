package org.littlesheep.deathforkeep.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.littlesheep.deathforkeep.utils.Messages;
import me.clip.placeholderapi.PlaceholderAPI;
import java.text.SimpleDateFormat;
import java.util.*;

public class GUIManager implements Listener {
    
    private final DeathForKeep plugin;
    private final Map<UUID, GUIType> openInventories = new HashMap<>();
    private final Map<UUID, Integer> adminPageMap = new HashMap<>();
    private final Map<UUID, List<UUID>> selectedPlayers = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    private static final int PLAYERS_PER_PAGE = 45;
    
    public enum GUIType {
        MAIN_MENU,
        DURATION_MENU,
        ADMIN_MENU,
        ADMIN_PLAYER_LIST,
        ADMIN_BATCH_ACTIONS
    }
    
    public GUIManager(DeathForKeep plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void openMainMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 27, 
                ChatColor.translateAlternateColorCodes('&', 
                messages.getMessage("gui.main.title")));
        
        // 购买保护按钮
        ItemStack buyItem = createItem(player, Material.EMERALD, 
                messages.getMessage("gui.main.buy"), 
                Arrays.asList(messages.getMessage("gui.main.buy-lore").split("\n")));
        inventory.setItem(11, buyItem);
        
        // 粒子效果按钮
        boolean particlesEnabled = plugin.getPlayerData(player.getUniqueId()).isParticlesEnabled();
        ItemStack particlesItem = createItem(player, Material.BLAZE_POWDER, 
                messages.getMessage("gui.main.particles"), 
                Arrays.asList(messages.getMessage("gui.main.particles-lore")
                        .replace("%status%", particlesEnabled ? 
                                messages.getMessage("gui.common.enabled") : 
                                messages.getMessage("gui.common.disabled"))
                        .split("\n")));
        inventory.setItem(13, particlesItem);
        
        // 帮助按钮
        ItemStack helpItem = createItem(player, Material.BOOK, 
                messages.getMessage("gui.main.help"), 
                Arrays.asList(messages.getMessage("gui.main.help-lore").split("\n")));
        inventory.setItem(15, helpItem);
        
        // 管理员按钮
        if (player.hasPermission("deathkeep.admin")) {
            ItemStack adminItem = createItem(player, Material.COMMAND_BLOCK, 
                    messages.getMessage("gui.main.admin"), 
                    Arrays.asList(messages.getMessage("gui.main.admin-lore").split("\n")));
            inventory.setItem(22, adminItem);
        }
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.MAIN_MENU);
    }
    
    public void openDurationMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 27, 
                ChatColor.translateAlternateColorCodes('&', 
                messages.getMessage("gui.duration.title")));
        
        // 1天选项
        double price1d = plugin.getConfig().getDouble("prices.1d");
        ItemStack item1d = createItem(player, Material.CLOCK, 
                messages.getMessage("gui.duration.one-day"), 
                Arrays.asList(messages.getMessage("gui.duration.one-day-lore")
                        .replace("%price%", String.format("%.2f", price1d))
                        .split("\n")));
        inventory.setItem(11, item1d);
        
        // 7天选项
        double price7d = plugin.getConfig().getDouble("prices.7d");
        ItemStack item7d = createItem(player, Material.SUNFLOWER, 
                messages.getMessage("gui.duration.seven-days"), 
                Arrays.asList(messages.getMessage("gui.duration.seven-days-lore")
                        .replace("%price%", String.format("%.2f", price7d))
                        .split("\n")));
        inventory.setItem(13, item7d);
        
        // 30天选项
        double price30d = plugin.getConfig().getDouble("prices.30d");
        ItemStack item30d = createItem(player, Material.EMERALD, 
                messages.getMessage("gui.duration.thirty-days"), 
                Arrays.asList(messages.getMessage("gui.duration.thirty-days-lore")
                        .replace("%price%", String.format("%.2f", price30d))
                        .split("\n")));
        inventory.setItem(15, item30d);
        
        // 返回按钮
        ItemStack backItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.common.back"), 
                Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
        inventory.setItem(22, backItem);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.DURATION_MENU);
    }
    
    public void openAdminMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 27, 
                ChatColor.translateAlternateColorCodes('&', 
                messages.getMessage("gui.admin-menu.title")));
        
        // 玩家列表按钮
        ItemStack listItem = createItem(player, Material.PLAYER_HEAD, 
                messages.getMessage("gui.admin-menu.player-list"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.player-list-lore").split("\n")));
        inventory.setItem(11, listItem);
        
        // 批量操作按钮
        ItemStack batchItem = createItem(player, Material.CHEST, 
                messages.getMessage("gui.admin-menu.batch-actions"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.batch-actions-lore").split("\n")));
        inventory.setItem(13, batchItem);
        
        // 重置数据按钮
        ItemStack resetItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.admin-menu.reset-all"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.reset-all-lore").split("\n")));
        inventory.setItem(15, resetItem);
        
        // 返回按钮
        ItemStack backItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.common.back"), 
                Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
        inventory.setItem(22, backItem);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.ADMIN_MENU);
    }
    
    public void openPlayerListMenu(Player player, int page) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 54, 
                ChatColor.translateAlternateColorCodes('&', messages.getMessage("gui.player-list.title")
                        .replace("%page%", String.valueOf(page + 1))));
        
        // 获取所有玩家数据
        Map<UUID, PlayerData> playerDataMap = plugin.getPlayerDataMap();
        List<Map.Entry<UUID, PlayerData>> entries = new ArrayList<>(playerDataMap.entrySet());
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) entries.size() / PLAYERS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;
        
        // 确保页码有效
        if (page < 0) page = 0;
        if (page >= totalPages) page = totalPages - 1;
        
        // 保存当前页码
        adminPageMap.put(player.getUniqueId(), page);
        
        // 显示玩家列表
        int startIndex = page * PLAYERS_PER_PAGE;
        int endIndex = Math.min(startIndex + PLAYERS_PER_PAGE, entries.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<UUID, PlayerData> entry = entries.get(i);
            UUID uuid = entry.getKey();
            PlayerData data = entry.getValue();
            
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            String playerName = offlinePlayer.getName() != null ? offlinePlayer.getName() : uuid.toString();
            
            boolean isActive = data.isActive();
            String expiryTime = isActive ? 
                    dateFormat.format(new Date(data.getExpiryTime() * 1000)) : 
                    messages.getMessage("gui.player-list.expired");
            
            Material material = isActive ? Material.LIME_WOOL : Material.RED_WOOL;
            
            List<String> lore = new ArrayList<>();
            lore.add(messages.getMessage("gui.player-list.expires").replace("%time%", expiryTime));
            
            if (data.getSharedWith() != null) {
                OfflinePlayer sharedWith = Bukkit.getOfflinePlayer(data.getSharedWith());
                lore.add(messages.getMessage("gui.player-list.shared-with")
                        .replace("%player%", sharedWith.getName() != null ? sharedWith.getName() : "Unknown"));
            }
            
            lore.add(messages.getMessage("gui.player-list.particles")
                    .replace("%status%", data.isParticlesEnabled() ? 
                            messages.getMessage("gui.common.enabled") : 
                            messages.getMessage("gui.common.disabled")));
            
            ItemStack playerItem = createItem(player, material, 
                    messages.getMessage("gui.player-list.player-name")
                            .replace("%player%", playerName), 
                    lore);
            
            inventory.setItem(i - startIndex, playerItem);
        }
        
        // 上一页按钮
        if (page > 0) {
            ItemStack prevItem = createItem(player, Material.ARROW, 
                    messages.getMessage("gui.common.previous-page"), 
                    Arrays.asList(messages.getMessage("gui.common.previous-page-lore").split("\n")));
            inventory.setItem(45, prevItem);
        }
        
        // 下一页按钮
        if (page < totalPages - 1) {
            ItemStack nextItem = createItem(player, Material.ARROW, 
                    messages.getMessage("gui.common.next-page"), 
                    Arrays.asList(messages.getMessage("gui.common.next-page-lore").split("\n")));
            inventory.setItem(53, nextItem);
        }
        
        // 返回按钮
        ItemStack backItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.common.back"), 
                Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
        inventory.setItem(49, backItem);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.ADMIN_PLAYER_LIST);
    }
    
    public void openBatchActionsMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 54, 
                ChatColor.translateAlternateColorCodes('&', messages.getMessage("gui.batch-actions.title")));
        
        // 获取所有在线玩家
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        // 确保选择列表存在
        if (!selectedPlayers.containsKey(player.getUniqueId())) {
            selectedPlayers.put(player.getUniqueId(), new ArrayList<>());
        }
        
        List<UUID> selected = selectedPlayers.get(player.getUniqueId());
        
        // 显示所有在线玩家
        for (int i = 0; i < Math.min(45, onlinePlayers.size()); i++) {
            Player target = onlinePlayers.get(i);
            boolean isSelected = selected.contains(target.getUniqueId());
            
            Material material = isSelected ? Material.LIME_WOOL : Material.RED_WOOL;
            String statusText = isSelected ? 
                    messages.getMessage("gui.batch-actions.selected") : 
                    messages.getMessage("gui.batch-actions.not-selected");
            
            ItemStack playerItem = createItem(player, material, 
                    messages.getMessage("gui.batch-actions.player-name")
                            .replace("%player%", target.getName()), 
                    Arrays.asList(
                            statusText,
                            messages.getMessage("gui.batch-actions.click-to-toggle")
                    ));
            
            inventory.setItem(i, playerItem);
        }
        
        // 全选按钮
        ItemStack selectAllItem = createItem(player, Material.EMERALD_BLOCK, 
                messages.getMessage("gui.batch-actions.select-all"), 
                Arrays.asList(messages.getMessage("gui.batch-actions.select-all-lore").split("\n")));
        inventory.setItem(46, selectAllItem);
        
        // 取消全选按钮
        ItemStack deselectAllItem = createItem(player, Material.REDSTONE_BLOCK, 
                messages.getMessage("gui.batch-actions.deselect-all"), 
                Arrays.asList(messages.getMessage("gui.batch-actions.deselect-all-lore").split("\n")));
        inventory.setItem(47, deselectAllItem);
        
        // 批量添加保护按钮
        ItemStack addItem = createItem(player, Material.EMERALD, 
                messages.getMessage("gui.batch-actions.add-protection"), 
                Arrays.asList(messages.getMessage("gui.batch-actions.add-protection-lore").split("\n")));
        inventory.setItem(51, addItem);
        
        // 批量移除保护按钮
        ItemStack removeItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.batch-actions.remove-protection"), 
                Arrays.asList(messages.getMessage("gui.batch-actions.remove-protection-lore").split("\n")));
        inventory.setItem(52, removeItem);
        
        // 返回按钮
        ItemStack backItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.common.back"), 
                Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
        inventory.setItem(49, backItem);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.ADMIN_BATCH_ACTIONS);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        if (!openInventories.containsKey(playerUUID)) return;
        
        event.setCancelled(true);
        
        GUIType guiType = openInventories.get(playerUUID);
        int slot = event.getRawSlot();
        
        switch (guiType) {
            case MAIN_MENU:
                handleMainMenuClick(player, slot);
                break;
            case DURATION_MENU:
                handleDurationMenuClick(player, slot);
                break;
            case ADMIN_MENU:
                handleAdminMenuClick(player, slot);
                break;
            case ADMIN_PLAYER_LIST:
                handlePlayerListClick(player, slot);
                break;
            case ADMIN_BATCH_ACTIONS:
                handleBatchActionsClick(player, slot);
                break;
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            openInventories.remove(event.getPlayer().getUniqueId());
        }
    }
    
    private void handleMainMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // 购买保护
                openDurationMenu(player);
                break;
            case 13: // 粒子效果
                toggleParticles(player);
                openMainMenu(player);
                break;
            case 15: // 帮助
                player.closeInventory();
                player.performCommand("dk help");
                break;
            case 22: // 管理员
                if (player.hasPermission("deathkeep.admin")) {
                    openAdminMenu(player);
                }
                break;
        }
    }
    
    private void handleDurationMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // 1天
                confirmPurchase(player, 1);
                break;
            case 13: // 7天
                confirmPurchase(player, 7);
                break;
            case 15: // 30天
                confirmPurchase(player, 30);
                break;
            case 22: // 返回
                openMainMenu(player);
                break;
        }
    }
    
    private void handleAdminMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // 玩家列表
                openPlayerListMenu(player, 0);
                break;
                
            case 13: // 批量操作
                openBatchActionsMenu(player);
                break;
                
            case 15: // 重置所有数据
                player.closeInventory();
                player.performCommand("deathkeep resetall");
                break;
                
            case 22: // 返回
                openMainMenu(player);
                break;
        }
    }
    
    private void handlePlayerListClick(Player player, int slot) {
        if (slot >= 0 && slot < 45) {
            // 玩家项目点击
            Map<UUID, PlayerData> playerDataMap = plugin.getPlayerDataMap();
            List<Map.Entry<UUID, PlayerData>> entries = new ArrayList<>(playerDataMap.entrySet());
            
            int page = adminPageMap.getOrDefault(player.getUniqueId(), 0);
            int startIndex = page * PLAYERS_PER_PAGE;
            
            if (startIndex + slot < entries.size()) {
                Map.Entry<UUID, PlayerData> entry = entries.get(startIndex + slot);
                UUID targetUUID = entry.getKey();
                
                // 打开玩家管理菜单或执行操作
                player.closeInventory();
                player.performCommand("deathkeep check " + Bukkit.getOfflinePlayer(targetUUID).getName());
            }
        } else if (slot == 45) {
            // 上一页
            int page = adminPageMap.getOrDefault(player.getUniqueId(), 0);
            if (page > 0) {
                openPlayerListMenu(player, page - 1);
            }
        } else if (slot == 53) {
            // 下一页
            int page = adminPageMap.getOrDefault(player.getUniqueId(), 0);
            Map<UUID, PlayerData> playerDataMap = plugin.getPlayerDataMap();
            int totalPages = (int) Math.ceil((double) playerDataMap.size() / PLAYERS_PER_PAGE);
            
            if (page < totalPages - 1) {
                openPlayerListMenu(player, page + 1);
            }
        } else if (slot == 49) {
            // 返回
            openAdminMenu(player);
        }
    }
    
    private void handleBatchActionsClick(Player player, int slot) {
        Messages messages = plugin.getMessages();
        List<UUID> selected = selectedPlayers.getOrDefault(player.getUniqueId(), new ArrayList<>());
        
        if (slot >= 0 && slot < 45) {
            // 玩家选择
            List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
            if (slot < onlinePlayers.size()) {
                Player target = onlinePlayers.get(slot);
                UUID targetUUID = target.getUniqueId();
                
                if (selected.contains(targetUUID)) {
                    selected.remove(targetUUID);
                } else {
                    selected.add(targetUUID);
                }
                
                openBatchActionsMenu(player);
            }
        } else {
            switch (slot) {
                case 46: // 全选
                    selected.clear();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        selected.add(online.getUniqueId());
                    }
                    openBatchActionsMenu(player);
                    break;
                    
                case 47: // 取消全选
                    selected.clear();
                    openBatchActionsMenu(player);
                    break;
                    
                case 51: // 批量添加保护
                    if (selected.isEmpty()) {
                        player.sendMessage(messages.getMessage("gui.batch-actions.no-selection"));
                        return;
                    }
                    player.closeInventory();
                    player.sendMessage(messages.getMessage("gui.batch-actions.enter-days"));
                    break;
                    
                case 52: // 批量移除保护
                    if (selected.isEmpty()) {
                        player.sendMessage(messages.getMessage("gui.batch-actions.no-selection"));
                        return;
                    }
                    for (UUID uuid : selected) {
                        Player target = Bukkit.getPlayer(uuid);
                        if (target != null) {
                            plugin.removeProtection(target.getUniqueId());
                        }
                    }
                    player.sendMessage(messages.getMessage("gui.batch-actions.removed")
                            .replace("%count%", String.valueOf(selected.size())));
                    selected.clear();
                    openBatchActionsMenu(player);
                    break;
                    
                case 49: // 返回
                    openAdminMenu(player);
                    break;
            }
        }
    }
    
    private void toggleParticles(Player player) {
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        boolean newState = !playerData.isParticlesEnabled();
        playerData.setParticlesEnabled(newState);
        
        Messages messages = plugin.getMessages();
        player.sendMessage(messages.getMessage("command.particles." + (newState ? "enabled" : "disabled")));
    }
    
    private void confirmPurchase(Player player, int days) {
        double price = plugin.getConfig().getDouble("prices." + days + "d");
        if (plugin.getEconomy().has(player, price)) {
            plugin.getEconomy().withdrawPlayer(player, price);
            plugin.addProtection(player.getUniqueId(), days * 86400);
            player.sendMessage(plugin.getMessages().getMessage("command.buy.success")
                    .replace("%days%", String.valueOf(days))
                    .replace("%price%", String.format("%.2f", price)));
            openMainMenu(player);
        } else {
            player.sendMessage(plugin.getMessages().getMessage("command.buy.not-enough-money")
                    .replace("%price%", String.format("%.2f", price)));
        }
    }
    
    private String parsePlaceholders(Player player, String text) {
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            return PlaceholderAPI.setPlaceholders(player, text);
        }
        return ChatColor.translateAlternateColorCodes('&', text);
    }
    
    private ItemStack createItem(Player player, Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', 
                parsePlaceholders(player, name)));
            
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) {
                coloredLore.add(ChatColor.translateAlternateColorCodes('&', 
                    parsePlaceholders(player, line)));
            }
            meta.setLore(coloredLore);
            
            item.setItemMeta(meta);
        }
        return item;
    }
} 