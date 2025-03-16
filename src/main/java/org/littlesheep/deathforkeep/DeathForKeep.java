package org.littlesheep.deathforkeep;

import net.milkbowl.vault.economy.Economy;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
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
import org.littlesheep.deathforkeep.tasks.ReminderTask;
import org.littlesheep.deathforkeep.utils.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

    private Map<UUID, PlayerData> playerDataMap = new HashMap<>();

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
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
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
        
        colorLogger.info(messages.getMessage("plugin.enabled"));
    }

    @Override
    public void onDisable() {
        // 关闭数据库连接
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        // 取消任务
        if (reminderTask != null) {
            reminderTask.cancelTask();
        }
        
        if (bossBarManager != null) {
            bossBarManager.stopTask();
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
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null && data.isActive()) {
            colorLogger.info("玩家 " + playerUUID + " 有活跃的保护");
            return true;
        }
        
        // 检查是否有其他玩家与此玩家共享保护
        for (PlayerData otherData : playerDataMap.values()) {
            if (otherData.isActive() && playerUUID.equals(otherData.getSharedWith())) {
                colorLogger.info("玩家 " + playerUUID + " 有来自其他玩家的共享保护");
                return true;
            }
        }
        
        colorLogger.info("玩家 " + playerUUID + " 没有保护");
        return false;
    }
    
    public void addProtection(UUID playerUUID, long durationInSeconds) {
        long currentTime = System.currentTimeMillis() / 1000;
        PlayerData data = playerDataMap.get(playerUUID);
        
        if (data == null) {
            data = new PlayerData(playerUUID, 0, true, null);
            playerDataMap.put(playerUUID, data);
            colorLogger.info("为玩家 " + playerUUID + " 创建了新的保护数据");
        }
        
        long expiryTime = data.getExpiryTime();
        // 如果已经过期，从当前时间开始计算
        if (expiryTime < currentTime) {
            expiryTime = currentTime;
            colorLogger.info("玩家 " + playerUUID + " 的保护已过期，重新从当前时间计算");
        }
        
        // 添加新的持续时间
        expiryTime += durationInSeconds;
        data.setExpiryTime(expiryTime);
        colorLogger.info("玩家 " + playerUUID + " 的保护到期时间设置为: " + expiryTime + "，当前时间: " + currentTime);
        
        // 保存到数据库
        databaseManager.savePlayerData(playerUUID, expiryTime, data.isParticlesEnabled(), data.getSharedWith());
        colorLogger.info("已将玩家 " + playerUUID + " 的保护数据保存到数据库");
    }
    
    public void removeProtection(UUID playerUUID) {
        playerDataMap.remove(playerUUID);
        databaseManager.removePlayerData(playerUUID);
    }
    
    public void setParticlesEnabled(UUID playerUUID, boolean enabled) {
        PlayerData data = playerDataMap.get(playerUUID);
        if (data != null) {
            data.setParticlesEnabled(enabled);
            databaseManager.updateParticlesEnabled(playerUUID, enabled);
        }
    }
    
    public boolean areParticlesEnabled(UUID playerUUID) {
        PlayerData data = playerDataMap.get(playerUUID);
        return data != null && data.isParticlesEnabled();
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
}
