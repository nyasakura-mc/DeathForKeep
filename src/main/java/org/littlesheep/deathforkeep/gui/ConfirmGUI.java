package org.littlesheep.deathforkeep.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ConfirmGUI implements Listener {
    
    private static final Map<UUID, ConfirmGUI> openGuis = new HashMap<>();
    
    private final DeathForKeep plugin;
    private final String title;
    private final String acceptText;
    private final String denyText;
    private final Runnable onAccept;
    private final Runnable onDeny;
    private final Inventory inventory;
    
    public ConfirmGUI(DeathForKeep plugin, String title, String acceptText, String denyText, 
                      Runnable onAccept, Runnable onDeny) {
        this.plugin = plugin;
        this.title = ChatColor.translateAlternateColorCodes('&', title);
        this.acceptText = ChatColor.translateAlternateColorCodes('&', acceptText);
        this.denyText = ChatColor.translateAlternateColorCodes('&', denyText);
        this.onAccept = onAccept;
        this.onDeny = onDeny;
        
        // 创建物品栏
        this.inventory = Bukkit.createInventory(null, 27, this.title);
        
        // 设置接受按钮
        ItemStack acceptItem = new ItemStack(Material.LIME_WOOL);
        ItemMeta acceptMeta = acceptItem.getItemMeta();
        acceptMeta.setDisplayName(this.acceptText);
        acceptItem.setItemMeta(acceptMeta);
        
        // 设置拒绝按钮
        ItemStack denyItem = new ItemStack(Material.RED_WOOL);
        ItemMeta denyMeta = denyItem.getItemMeta();
        denyMeta.setDisplayName(this.denyText);
        denyItem.setItemMeta(denyMeta);
        
        // 放置按钮
        inventory.setItem(11, acceptItem);
        inventory.setItem(15, denyItem);
        
        // 注册监听器
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
    
    public void open(Player player) {
        player.openInventory(inventory);
        openGuis.put(player.getUniqueId(), this);
    }
    
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();
        
        if (!openGuis.containsKey(playerUUID)) return;
        
        ConfirmGUI gui = openGuis.get(playerUUID);
        if (event.getInventory().equals(gui.inventory)) {
            event.setCancelled(true);
            
            int slot = event.getRawSlot();
            if (slot == 11) {
                // 接受按钮
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, gui.onAccept);
            } else if (slot == 15) {
                // 拒绝按钮
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, gui.onDeny);
            }
        }
    }
    
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;
        
        Player player = (Player) event.getPlayer();
        UUID playerUUID = player.getUniqueId();
        
        if (openGuis.containsKey(playerUUID)) {
            ConfirmGUI gui = openGuis.get(playerUUID);
            if (event.getInventory().equals(gui.inventory)) {
                openGuis.remove(playerUUID);
            }
        }
    }
    
    public static void cleanup() {
        openGuis.clear();
    }
} 