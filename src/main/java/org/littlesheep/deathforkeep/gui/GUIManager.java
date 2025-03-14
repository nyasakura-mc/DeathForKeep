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
        BUY_MENU,
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
        Inventory inventory = Bukkit.createInventory(null, 36, 
                ChatColor.translateAlternateColorCodes('&', 
                parsePlaceholders(player, messages.getMessage("gui.main.title"))));
        
        // 状态按钮
        List<String> statusLore = new ArrayList<>();
        String statusLoreText = messages.getMessage("gui.main.status-lore")
                .replace("%status%", "%deathkeep_status%")
                .replace("%time%", "%deathkeep_time%")
                .replace("%particles%", "%deathkeep_particles%")
                .replace("%share_status%", "%deathkeep_share_status%");
                
        for (String line : statusLoreText.split("\n")) {
            statusLore.add(parsePlaceholders(player, line));
        }
        
        ItemStack statusItem = createItem(player, Material.COMPASS, 
                messages.getMessage("gui.main.status"), 
                statusLore);
        inventory.setItem(11, statusItem);
        
        // 购买按钮
        double price = plugin.getConfig().getDouble("prices.1d");
        String formattedPrice = formatPrice(price);
        ItemStack buyItem = createItem(player, Material.EMERALD, 
                messages.getMessage("gui.main.buy"), 
                Arrays.asList(messages.getMessage("gui.main.buy-lore")
                        .replace("%price%", formattedPrice)
                        .split("\n")));
        inventory.setItem(13, buyItem);
        
        // 帮助按钮
        ItemStack helpItem = createItem(player, Material.BOOK, 
                messages.getMessage("gui.main.help"), 
                Arrays.asList(messages.getMessage("gui.main.help-lore").split("\n")));
        inventory.setItem(15, helpItem);
        
        // 设置按钮
        ItemStack settingsItem = createItem(player, Material.COMPARATOR, 
                messages.getMessage("gui.main.settings"), 
                Arrays.asList(messages.getMessage("gui.main.settings-lore").split("\n")));
        inventory.setItem(31, settingsItem);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.MAIN_MENU);
        
        // 启动动态更新任务
        startGuiUpdateTask(player);
    }
    
    public void openBuyMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 36, 
                ChatColor.translateAlternateColorCodes('&', messages.getMessage("gui.buy.title")));
        
        // 1天
        double price1d = plugin.getConfig().getDouble("prices.1d");
        ItemStack item1d = createItem(player, Material.CLOCK, 
                messages.getMessage("gui.buy.one-day"), 
                Arrays.asList(messages.getMessage("gui.buy.one-day-lore")
                        .replace("%price%", formatPrice(price1d))
                        .split("\n")));
        inventory.setItem(10, item1d);
        
        // 7天
        double price7d = plugin.getConfig().getDouble("prices.7d");
        ItemStack item7d = createItem(player, Material.CLOCK, 
                messages.getMessage("gui.buy.seven-days"), 
                Arrays.asList(messages.getMessage("gui.buy.seven-days-lore")
                        .replace("%price%", formatPrice(price7d))
                        .split("\n")));
        inventory.setItem(13, item7d);
        
        // 30天
        double price30d = plugin.getConfig().getDouble("prices.30d");
        ItemStack item30d = createItem(player, Material.CLOCK, 
                messages.getMessage("gui.buy.thirty-days"), 
                Arrays.asList(messages.getMessage("gui.buy.thirty-days-lore")
                        .replace("%price%", formatPrice(price30d))
                        .split("\n")));
        inventory.setItem(16, item30d);
        
        // 返回按钮
        ItemStack backItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.common.back"), 
                Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
        inventory.setItem(31, backItem);
        
        player.openInventory(inventory);
        openInventories.put(player.getUniqueId(), GUIType.BUY_MENU);
    }
    
    public void openAdminMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 27, 
                ChatColor.translateAlternateColorCodes('&', messages.getMessage("gui.admin-menu.title")));
        
        // 玩家列表按钮
        ItemStack playerListItem = createItem(player, Material.PLAYER_HEAD, 
                messages.getMessage("gui.admin-menu.player-list"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.player-list-lore").split("\n")));
        inventory.setItem(11, playerListItem);
        
        // 批量操作按钮
        ItemStack batchActionsItem = createItem(player, Material.COMMAND_BLOCK, 
                messages.getMessage("gui.admin-menu.batch-actions"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.batch-actions-lore").split("\n")));
        inventory.setItem(13, batchActionsItem);
        
        // 重置所有数据按钮
        ItemStack resetAllItem = createItem(player, Material.TNT, 
                messages.getMessage("gui.admin-menu.reset-all"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.reset-all-lore").split("\n")));
        inventory.setItem(15, resetAllItem);
        
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
                
            case BUY_MENU:
                handleBuyMenuClick(player, slot);
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
            case 11: // 检查状态
                player.closeInventory();
                player.performCommand("deathkeep check");
                break;
                
            case 13: // 购买保护
                openBuyMenu(player);
                break;
                
            case 15: // 粒子设置
                player.closeInventory();
                player.performCommand("deathkeep particles");
                break;
                
            case 31: // 管理员面板
                if (player.hasPermission("deathkeep.admin")) {
                    openAdminMenu(player);
                }
                break;
        }
    }
    
    private void handleBuyMenuClick(Player player, int slot) {
        switch (slot) {
            case 10: // 1天
                buyProtection(player, 1);
                break;
                
            case 13: // 7天
                buyProtection(player, 7);
                break;
                
            case 16: // 30天
                buyProtection(player, 30);
                break;
                
            case 31: // 返回
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
    
    private void buyProtection(Player player, int days) {
        player.closeInventory();
        player.performCommand("deathkeep buy " + days);
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
    
    private void startGuiUpdateTask(Player player) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline() || !openInventories.containsKey(player.getUniqueId())) {
                return;
            }
            
            Inventory inv = player.getOpenInventory().getTopInventory();
            if (inv == null) return;
            
            // 更新状态按钮
            ItemStack statusItem = inv.getItem(11);
            if (statusItem != null && statusItem.getType() == Material.COMPASS) {
                List<String> statusLore = new ArrayList<>();
                String statusLoreText = plugin.getMessages().getMessage("gui.main.status-lore")
                        .replace("%status%", "%deathkeep_status%")
                        .replace("%time%", "%deathkeep_time%")
                        .replace("%particles%", "%deathkeep_particles%")
                        .replace("%share_status%", "%deathkeep_share_status%");
                        
                for (String line : statusLoreText.split("\n")) {
                    statusLore.add(parsePlaceholders(player, line));
                }
                
                ItemMeta meta = statusItem.getItemMeta();
                if (meta != null) {
                    meta.setLore(statusLore);
                    statusItem.setItemMeta(meta);
                }
            }
        }, 20L, 20L); // 每秒更新一次
    }
    
    private String formatPrice(double price) {
        if (price == (int) price) {
            return String.valueOf((int) price);
        }
        return String.format("%.2f", price);
    }
} 