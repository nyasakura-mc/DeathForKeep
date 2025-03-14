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
import org.bukkit.metadata.FixedMetadataValue;

public class GUIManager implements Listener {
    
    private final DeathForKeep plugin;
    private final Map<UUID, GUIType> openInventories = new HashMap<>();
    private final Map<UUID, Integer> adminPageMap = new HashMap<>();
    private final Map<UUID, List<UUID>> selectedPlayers = new HashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final Map<UUID, Long> lastClickTime = new HashMap<>();
    
    private static final int PLAYERS_PER_PAGE = 45;
    private static final long CLICK_COOLDOWN = 500;
    
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
        // 确保移除之前的状态
        openInventories.remove(player.getUniqueId());
        
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 27, 
                ChatColor.translateAlternateColorCodes('&', 
                messages.getMessage("gui.main.title")));
        
        // 购买保护按钮
        double price1d = plugin.getConfig().getDouble("prices.1d");
        String priceStr = String.format("%.2f", price1d); // 格式化价格为两位小数
        ItemStack buyItem = createItem(player, Material.EMERALD, 
                messages.getMessage("gui.main.buy"), 
                Arrays.asList(messages.getMessage("gui.main.buy-lore")
                        .replace("%price%", priceStr)
                        .split("\n")));
        inventory.setItem(11, buyItem);
        
        // 添加保护状态信息
        PlayerData playerData = plugin.getPlayerData(player.getUniqueId());
        boolean hasProtection = playerData != null && playerData.isActive();
        String timeLeft = "";
        
        if (hasProtection) {
            long currentTime = System.currentTimeMillis() / 1000;
            @SuppressWarnings("null")
            long expiryTime = playerData.getExpiryTime();
            long secondsLeft = expiryTime - currentTime;
            
            if (secondsLeft > 0) {
                timeLeft = formatTime(secondsLeft);
            } else {
                timeLeft = messages.getMessage("gui.main.status-expired");
            }
        }
        
        Material statusMaterial = hasProtection ? Material.TOTEM_OF_UNDYING : Material.BARRIER;
        String statusTitle = messages.getMessage("gui.main.status");
        List<String> statusLore = Arrays.asList(
                messages.getMessage("gui.main.status-lore")
                        .replace("%status%", hasProtection ? 
                                messages.getMessage("gui.main.status-active") : 
                                messages.getMessage("gui.main.status-inactive"))
                        .replace("%time%", timeLeft)
                        .split("\n"));
        
        ItemStack statusItem = createItem(player, statusMaterial, statusTitle, statusLore);
        inventory.setItem(4, statusItem);
        
        // 粒子效果按钮
        boolean particlesEnabled = playerData != null && playerData.isParticlesEnabled();
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
        // 使用延迟任务打开菜单，避免可能的事件冲突
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 确保移除之前的状态
            openInventories.remove(player.getUniqueId());
            
            Messages messages = plugin.getMessages();
            Inventory inventory = Bukkit.createInventory(null, 27, 
                    ChatColor.translateAlternateColorCodes('&', 
                    messages.getMessage("gui.buy.title")));
            
            // 1天选项
            double price1d = plugin.getConfig().getDouble("prices.1d");
            ItemStack item1d = createItem(player, Material.CLOCK, 
                    messages.getMessage("gui.buy.one-day"), 
                    Arrays.asList(messages.getMessage("gui.buy.one-day-lore")
                            .replace("%price%", String.format("%.2f", price1d))
                            .split("\n")));
            inventory.setItem(11, item1d);
            
            // 7天选项
            double price7d = plugin.getConfig().getDouble("prices.7d");
            ItemStack item7d = createItem(player, Material.SUNFLOWER, 
                    messages.getMessage("gui.buy.seven-days"), 
                    Arrays.asList(messages.getMessage("gui.buy.seven-days-lore")
                            .replace("%price%", String.format("%.2f", price7d))
                            .split("\n")));
            inventory.setItem(13, item7d);
            
            // 30天选项
            double price30d = plugin.getConfig().getDouble("prices.30d");
            ItemStack item30d = createItem(player, Material.EMERALD, 
                    messages.getMessage("gui.buy.thirty-days"), 
                    Arrays.asList(messages.getMessage("gui.buy.thirty-days-lore")
                            .replace("%price%", String.format("%.2f", price30d))
                            .split("\n")));
            inventory.setItem(15, item30d);
            
            // 返回按钮
            ItemStack backItem = createItem(player, Material.BARRIER, 
                    messages.getMessage("gui.common.back"), 
                    Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
            inventory.setItem(22, backItem);
            
            player.openInventory(inventory);
            // 在打开后设置状态
            openInventories.put(player.getUniqueId(), GUIType.DURATION_MENU);
        }, 1L); // 1 tick 延迟
    }
    
    public void openAdminMenu(Player player) {
        Messages messages = plugin.getMessages();
        Inventory inventory = Bukkit.createInventory(null, 36, 
                ChatColor.translateAlternateColorCodes('&', 
                messages.getMessage("gui.admin-menu.title")));
        
        // 玩家列表按钮
        ItemStack listItem = createItem(player, Material.PLAYER_HEAD, 
                messages.getMessage("gui.admin-menu.player-list"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.player-list-lore").split("\n")));
        inventory.setItem(11, listItem);
        
        // 批量增加按钮
        ItemStack addBatchItem = createItem(player, Material.EMERALD, 
                messages.getMessage("gui.admin-menu.batch-add"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.batch-add-lore").split("\n")));
        inventory.setItem(13, addBatchItem);
        
        // 批量减少按钮
        ItemStack removeBatchItem = createItem(player, Material.REDSTONE, 
                messages.getMessage("gui.admin-menu.batch-remove"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.batch-remove-lore").split("\n")));
        inventory.setItem(15, removeBatchItem);
        
        // 重置数据按钮
        ItemStack resetItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.admin-menu.reset-all"), 
                Arrays.asList(messages.getMessage("gui.admin-menu.reset-all-lore").split("\n")));
        inventory.setItem(22, resetItem);
        
        // 返回按钮
        ItemStack backItem = createItem(player, Material.BARRIER, 
                messages.getMessage("gui.common.back"), 
                Arrays.asList(messages.getMessage("gui.common.back-lore").split("\n")));
        inventory.setItem(31, backItem);
        
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
    
    public void openBatchActionsMenu(Player player, boolean isAddMode) {
        Messages messages = plugin.getMessages();
        String title = isAddMode ? 
                messages.getMessage("gui.batch-actions.add-title") : 
                messages.getMessage("gui.batch-actions.remove-title");
        
        Inventory inventory = Bukkit.createInventory(null, 54, 
                ChatColor.translateAlternateColorCodes('&', title));
        
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
        
        // 操作按钮 (添加或移除)
        Material actionMaterial = isAddMode ? Material.EMERALD : Material.REDSTONE;
        String actionText = isAddMode ? 
                messages.getMessage("gui.batch-actions.add-protection") : 
                messages.getMessage("gui.batch-actions.remove-protection");
        String actionLore = isAddMode ? 
                messages.getMessage("gui.batch-actions.add-protection-lore") : 
                messages.getMessage("gui.batch-actions.remove-protection-lore");
        
        ItemStack actionItem = createItem(player, actionMaterial, 
                actionText, Arrays.asList(actionLore.split("\n")));
        inventory.setItem(51, actionItem);
        
        // 存储当前操作模式
        player.setMetadata("dk_batch_mode", new FixedMetadataValue(plugin, isAddMode));
        
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
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        UUID uuid = player.getUniqueId();
        
        if (!openInventories.containsKey(uuid)) {
            return;
        }
        
        event.setCancelled(true);
        
        // 防止快速点击
        long now = System.currentTimeMillis();
        if (lastClickTime.containsKey(uuid) && now - lastClickTime.get(uuid) < CLICK_COOLDOWN) {
            return;
        }
        lastClickTime.put(uuid, now);
        
        int slot = event.getRawSlot();
        if (slot < 0) {
            return;
        }
        
        GUIType type = openInventories.get(uuid);
        
        switch (type) {
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
        if (slot == 11) { // 购买保护
            openDurationMenu(player);
        } else if (slot == 13) { // 粒子效果
            toggleParticles(player);
            openMainMenu(player);
        } else if (slot == 15) { // 帮助
            player.closeInventory();
            player.performCommand("dk help");
        } else if (slot == 22 && player.hasPermission("deathkeep.admin")) { // 管理员
            openAdminMenu(player);
        }
    }
    
    private void handleDurationMenuClick(Player player, int slot) {
        if (slot == 11) { // 1天
            confirmPurchase(player, 1);
        } else if (slot == 13) { // 7天
            confirmPurchase(player, 7);
        } else if (slot == 15) { // 30天
            confirmPurchase(player, 30);
        } else if (slot == 22) { // 返回
            openMainMenu(player);
        }
    }
    
    private void handleAdminMenuClick(Player player, int slot) {
        switch (slot) {
            case 11: // 玩家列表
                openPlayerListMenu(player, 0);
                break;
                
            case 13: // 批量增加
                openBatchActionsMenu(player, true);
                break;
                
            case 15: // 批量减少
                openBatchActionsMenu(player, false);
                break;
                
            case 22: // 重置所有数据
                player.closeInventory();
                player.performCommand("dk resetall");
                break;
                
            case 31: // 返回
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
        boolean isAddMode = player.hasMetadata("dk_batch_mode") && player.getMetadata("dk_batch_mode").get(0).asBoolean();
        
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
                
                openBatchActionsMenu(player, isAddMode);
            }
        } else {
            switch (slot) {
                case 46: // 全选
                    selected.clear();
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        selected.add(online.getUniqueId());
                    }
                    openBatchActionsMenu(player, isAddMode);
                    break;
                    
                case 47: // 取消全选
                    selected.clear();
                    openBatchActionsMenu(player, isAddMode);
                    break;
                    
                case 51: // 执行批量操作
                    if (selected.isEmpty()) {
                        player.sendMessage(messages.getMessage("gui.batch-actions.no-selection"));
                        return;
                    }
                    
                    player.closeInventory();
                    
                    // 构建玩家列表字符串
                    StringBuilder playerList = new StringBuilder();
                    for (UUID uuid : selected) {
                        Player target = Bukkit.getPlayer(uuid);
                        if (target != null) {
                            if (playerList.length() > 0) {
                                playerList.append(",");
                            }
                            playerList.append(target.getName());
                        }
                    }
                    
                    // 要求输入时长
                    player.sendMessage(messages.getMessage("gui.batch-actions.enter-days"));
                    
                    // 设置元数据以便其他插件可以处理输入
                    player.setMetadata("dk_bulk_mode", new FixedMetadataValue(plugin, isAddMode ? "add" : "remove"));
                    player.setMetadata("dk_bulk_players", new FixedMetadataValue(plugin, playerList.toString()));
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
    
    private String formatTime(long seconds) {
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
} 