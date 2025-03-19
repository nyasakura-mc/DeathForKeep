/*
  死亡物品保护插件主类
  管理插件的启动、禁用和相关功能
 */
package org.littlesheep.deathforkeep;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.littlesheep.deathforkeep.commands.DeathKeepCommand;
import org.littlesheep.deathforkeep.data.DatabaseManager;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.littlesheep.deathforkeep.gui.GUIManager;
import org.littlesheep.deathforkeep.hooks.PlaceholderHook;
import org.littlesheep.deathforkeep.listeners.DeathListener;
import org.littlesheep.deathforkeep.listeners.JoinListener;
import org.littlesheep.deathforkeep.listeners.ChatListener;
import org.littlesheep.deathforkeep.service.ProtectionService;
import org.littlesheep.deathforkeep.tasks.ReminderTask;
import org.littlesheep.deathforkeep.utils.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

public final class DeathForKeep extends JavaPlugin {

    private Economy economy;
    private DatabaseManager databaseManager;
    private Messages messages;
    private ColorLogger colorLogger;
    private ConfigManager configManager;
    private GUIManager guiManager;
    private BossBarManager bossBarManager;
    private ShareRequestManager shareRequestManager;
    private ReminderTask reminderTask;
    private ProtectionService protectionService;

    private Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();

    @Override
    public void onEnable() {
        // 初始化彩色日志
        colorLogger = new ColorLogger(getLogger());
        colorLogger.logStartup();
        
        // 初始化配置管理器
        configManager = new ConfigManager(this, colorLogger);
        configManager.checkConfig();
        configManager.checkLanguageFiles();
        
        // 初始化消息系统
        messages = new Messages(this);
        
        // 初始化数据库
        databaseManager = new DatabaseManager(this);
        
        // 加载玩家数据
        playerDataMap = databaseManager.loadAllPlayerData();
        
        // 初始化保护服务
        protectionService = new ProtectionService(this, databaseManager, playerDataMap);
        
        // 设置经济系统
        if (!setupEconomy()) {
            colorLogger.error(messages.getMessage("economy.not-found"));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化GUI管理器
        guiManager = new GUIManager(this);
        bossBarManager = new BossBarManager(this);
        shareRequestManager = new ShareRequestManager(this);
        
        // 注册命令
        registerCommands();
        
        // 注册监听器
        DeathListener deathListener = new DeathListener(this);
        getServer().getPluginManager().registerEvents(deathListener, this);
        getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        
        // 启动提醒任务
        reminderTask = new ReminderTask(this);
        reminderTask.startTask();
        bossBarManager.startTask();
        
        // 注册 PlaceholderAPI 扩展
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderHook(this).register();
            colorLogger.info("已成功注册 PlaceholderAPI 扩展！");
        }
        
        // 注册bStats
        if (getConfig().getBoolean("bstats.enabled", true)) {
            int pluginId = 25068;
            new Metrics(this, pluginId);
            colorLogger.info("已启用 bStats 统计");
        }
        
        // 添加死亡保护的最终检查任务
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isDead() && hasActiveProtection(player.getUniqueId())) {
                    // 如果玩家已死亡且有保护，强制设置保持物品
                }
            }
        }, 1L, 1L);
        
        colorLogger.info(messages.getMessage("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        // 保存所有玩家数据
        try {
            for (UUID playerUUID : playerDataMap.keySet()) {
                savePlayerData(playerUUID);
            }
            colorLogger.info("所有玩家数据已保存");
        } catch (Exception e) {
            colorLogger.error("保存玩家数据时出错: " + e.getMessage());
        }
        
        // 取消任务
        try {
            if (reminderTask != null) {
                reminderTask.cancelTask();
                colorLogger.info("提醒任务已取消");
            }
            
            if (bossBarManager != null) {
                bossBarManager.stopTask();
                colorLogger.info("BossBar任务已取消");
            }
            
            // 取消所有可能遗留的任务
            Bukkit.getScheduler().cancelTasks(this);
        } catch (Exception e) {
            colorLogger.error("取消任务时出错: " + e.getMessage());
        }
        
        // 关闭数据库连接
        try {
            if (databaseManager != null) {
                databaseManager.closeConnection();
            }
        } catch (Exception e) {
            colorLogger.error("关闭数据库连接时出错: " + e.getMessage());
        }
        
        colorLogger.logShutdown();
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    private void registerCommands() {
        DeathKeepCommand commandExecutor = new DeathKeepCommand(this);
        PluginCommand command = getCommand("deathkeep");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
            // 注册别名
            command.setAliases(Arrays.asList("dk", "dkeep"));
        }
    }
    
    public void reload() {
        // 重新加载配置
        reloadConfig();
        configManager.checkLanguageFiles();
        messages.loadLanguage();
        
        colorLogger.logReload();
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public Messages getMessages() {
        return messages;
    }
    
    public ColorLogger getColorLogger() {
        return colorLogger;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public GUIManager getGuiManager() {
        return guiManager;
    }
    
    public BossBarManager getBossBarManager() {
        return bossBarManager;
    }
    
    public ShareRequestManager getShareRequestManager() {
        return shareRequestManager;
    }
    
    public Map<UUID, PlayerData> getPlayerDataMap() {
        return playerDataMap;
    }
    
    public boolean hasActiveProtection(UUID playerUUID) {
        return protectionService.hasActiveProtection(playerUUID);
    }
    
    public boolean addProtectionDays(UUID playerUUID, int days) {
        return protectionService.addProtectionDays(playerUUID, days);
    }
    
    public boolean removeProtection(UUID playerUUID) {
        return protectionService.removeProtection(playerUUID);
    }
    
    public long getProtectionTimeLeft(UUID playerUUID) {
        return protectionService.getProtectionTimeLeft(playerUUID);
    }
    
    public void setParticlesEnabled(UUID playerUUID, boolean enabled) {
        protectionService.setParticlesEnabled(playerUUID, enabled);
    }
    
    public boolean isParticlesEnabled(UUID playerUUID) {
        return protectionService.isParticlesEnabled(playerUUID);
    }
    
    public void shareProtection(UUID playerUUID, UUID targetUUID) {
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null) {
            data.setSharedWith(targetUUID);
            databaseManager.updateSharedWith(playerUUID, targetUUID);
        }
    }
    
    public double getWorldPrice(World world) {
        String worldName = world.getName();
        double defaultPrice = getConfig().getDouble("price-per-day", 1000);
        
        if (getConfig().contains("worlds." + worldName + ".price")) {
            return getConfig().getDouble("worlds." + worldName + ".price", defaultPrice);
        }
        
        return defaultPrice;
    }
    
    public void resetAllData() {
        playerDataMap.clear();
        databaseManager.resetAllData();
    }
    
    public String getRemainingTimeFormatted(UUID playerUUID) {
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null && data.isActive()) {
            long remainingSeconds = data.getExpiryTime() - (System.currentTimeMillis() / 1000);
            return TimeUtils.formatTime(remainingSeconds);
        }
        
        // 检查是否有其他玩家与此玩家共享保护
        for (PlayerData otherData : playerDataMap.values()) {
            if (otherData.isActive() && playerUUID.equals(otherData.getSharedWith())) {
                long remainingSeconds = otherData.getExpiryTime() - (System.currentTimeMillis() / 1000);
                return TimeUtils.formatTime(remainingSeconds);
            }
        }
        
        return "0";
    }
    
    public void batchAddProtection(Map<UUID, Long> playerDurations) {
        for (Map.Entry<UUID, Long> entry : playerDurations.entrySet()) {
            addProtection(entry.getKey(), entry.getValue());
        }
    }
    
    public void batchRemoveProtection(Iterable<UUID> playerUUIDs) {
        for (UUID uuid : playerUUIDs) {
            removeProtection(uuid);
        }
    }

    /**
     * 检查一个玩家是否正在与另一个玩家共享保护
     * 
     * @param ownerUUID 保护拥有者的UUID
     * @param targetUUID 目标玩家的UUID
     * @return 如果正在共享则返回true
     */
    public boolean isSharing(UUID ownerUUID, UUID targetUUID) {
        if (!hasActiveProtection(ownerUUID)) {
            return false;
        }
        
        PlayerData data = getPlayerData(ownerUUID);
        if (data == null) {
            return false;
        }
        
        UUID sharedWith = data.getSharedWith();
        return sharedWith != null && sharedWith.equals(targetUUID);
    }

    /**
     * 获取玩家数据
     * 
     * @param playerUUID 玩家UUID
     * @return 玩家数据，如果不存在则返回null
     */
    public PlayerData getPlayerData(UUID playerUUID) {
        return playerDataMap.get(playerUUID);
    }

    /**
     * 获取玩家保护的剩余秒数
     * 
     * @param playerUUID 玩家UUID
     * @return 剩余秒数，如果没有保护则返回0
     */
    public long getRemainingSeconds(UUID playerUUID) {
        PlayerData data = getPlayerData(playerUUID);
        if (data == null || !data.isActive()) {
            return 0;
        }
        
        long currentTime = System.currentTimeMillis() / 1000;
        long expiryTime = data.getExpiryTime();
        
        return Math.max(0, expiryTime - currentTime);
    }

    /**
     * 重新加载插件数据
     */
    public void reloadPluginData() {
        reloadConfig();
        configManager.checkLanguageFiles();
        messages.loadLanguage();
        playerDataMap = databaseManager.loadAllPlayerData();
        colorLogger.logReload();
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataMap.get(uuid);
        if (data != null) {
            databaseManager.savePlayerData(
                uuid, 
                data.getExpiryTime(), 
                data.isParticlesEnabled(), 
                data.getSharedWith()
            );
        }
    }

    /**
     * 添加保护（老方法，为兼容性保留）
     * 
     * @param playerUUID 玩家UUID
     * @param durationInSeconds 保护持续时间（秒）
     */
    public void addProtection(UUID playerUUID, long durationInSeconds) {
        int days = (int) (durationInSeconds / (24 * 60 * 60));
        if (days < 1) days = 1; // 确保至少1天
        addProtectionDays(playerUUID, days);
    }
    
    /**
     * 添加保护（老方法重载版本，为兼容性保留）
     * 
     * @param playerUUID 玩家UUID
     * @param days 保护天数
     */
    public void addProtection(UUID playerUUID, int days) {
        addProtectionDays(playerUUID, days);
    }

    /**
     * 检查玩家粒子效果是否启用（老方法，为兼容性保留）
     * 
     * @param playerUUID 玩家UUID
     * @return 是否启用
     */
    public boolean areParticlesEnabled(UUID playerUUID) {
        return isParticlesEnabled(playerUUID);
    }
}
