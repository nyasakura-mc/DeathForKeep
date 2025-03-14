package org.littlesheep.deathforkeep.utils;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ConfigManager {
    
    private final DeathForKeep plugin;
    private final ColorLogger logger;
    
    // 配置文件版本
    private static final int CONFIG_VERSION = 3;
    private static final int LANG_VERSION = 2;
    
    public ConfigManager(DeathForKeep plugin, ColorLogger logger) {
        this.plugin = plugin;
        this.logger = logger;
    }
    
    /**
     * 加载并检查配置文件
     */
    public void checkConfig() {
        FileConfiguration config = plugin.getConfig();
        int currentVersion = config.getInt("config-version", 1);
        
        if (currentVersion < CONFIG_VERSION) {
            logger.info("配置文件版本过低 (v" + currentVersion + ")，正在更新到 v" + CONFIG_VERSION);
            backupAndUpdateConfig();
        }
    }
    
    private void backupAndUpdateConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        
        // 备份旧配置
        backupFile(configFile, "config_backup_");
        
        // 保存新配置
        plugin.saveResource("config.yml", true);
        
        // 重新加载配置
        plugin.reloadConfig();
        
        logger.info("配置文件已更新并备份旧配置");
    }
    
    /**
     * 检查语言文件
     */
    public void checkLanguageFiles() {
        // 确保语言文件夹存在
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        // 保存默认语言文件
        saveDefaultLanguageFile("zh-CN.yml");
        saveDefaultLanguageFile("en.yml");
        
        // 检查语言文件版本
        String language = plugin.getConfig().getString("language", "zh-CN");
        File langFile = new File(langFolder, language + ".yml");
        
        if (langFile.exists()) {
            YamlConfiguration langConfig = YamlConfiguration.loadConfiguration(langFile);
            int currentVersion = langConfig.getInt("version", 1);
            
            if (currentVersion < LANG_VERSION) {
                logger.info("检测到语言文件 " + language + " 版本过低 (v" + currentVersion + ")，正在更新到 v" + LANG_VERSION);
                updateLanguageFile(language);
            }
        }
    }
    
    /**
     * 保存默认语言文件
     */
    private void saveDefaultLanguageFile(String fileName) {
        File langFile = new File(plugin.getDataFolder() + "/lang", fileName);
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }
    
    /**
     * 更新语言文件
     */
    private void updateLanguageFile(String language) {
        File langFile = new File(plugin.getDataFolder() + "/lang", language + ".yml");
        
        // 备份旧语言文件
        backupFile(langFile, "lang_" + language + "_backup_");
        
        // 保存新语言文件
        plugin.saveResource("lang/" + language + ".yml", true);
        
        logger.info("语言文件 " + language + " 已更新并备份旧文件");
    }
    
    /**
     * 备份文件
     */
    private void backupFile(File file, String prefix) {
        if (file.exists()) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd_HHmmss");
                String timestamp = format.format(new Date());
                
                File backupFile = new File(plugin.getDataFolder(), prefix + timestamp + ".yml");
                Files.copy(file.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                
                logger.info("已创建备份: " + backupFile.getName());
            } catch (IOException e) {
                logger.error("创建备份失败: " + e.getMessage());
            }
        }
    }
} 