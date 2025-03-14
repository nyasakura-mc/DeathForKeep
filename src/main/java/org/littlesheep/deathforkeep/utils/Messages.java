package org.littlesheep.deathforkeep.utils;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Messages {
    
    private final DeathForKeep plugin;
    private FileConfiguration langConfig;
    private final Map<String, String> messageCache = new HashMap<>();
    private String prefix;
    
    public Messages(DeathForKeep plugin) {
        this.plugin = plugin;
        loadLanguage();
    }
    
    public void loadLanguage() {
        messageCache.clear();
        String language = plugin.getConfig().getString("language", "en");
        
        // 确保语言文件夹存在
        File langFolder = new File(plugin.getDataFolder(), "lang");
        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }
        
        // 保存默认语言文件
        saveDefaultLanguageFile("en.yml");
        saveDefaultLanguageFile("zh-CN.yml");
        
        // 加载指定的语言文件
        File langFile = new File(langFolder, language + ".yml");
        if (!langFile.exists()) {
            plugin.getLogger().warning("找不到语言文件: " + language + ".yml，使用默认英文");
            langFile = new File(langFolder, "en.yml");
        }
        
        langConfig = YamlConfiguration.loadConfiguration(langFile);
        
        // 加载默认语言文件作为备用
        InputStreamReader defaultLangStream = new InputStreamReader(
                plugin.getResource("lang/en.yml"), StandardCharsets.UTF_8);
        if (defaultLangStream != null) {
            YamlConfiguration defaultLang = YamlConfiguration.loadConfiguration(defaultLangStream);
            langConfig.setDefaults(defaultLang);
        }
        
        // 加载前缀
        prefix = ChatColor.translateAlternateColorCodes('&', 
                langConfig.getString("general.prefix", "&6[DeathForKeep] &r"));
    }
    
    private void saveDefaultLanguageFile(String fileName) {
        File langFile = new File(plugin.getDataFolder() + "/lang", fileName);
        if (!langFile.exists()) {
            plugin.saveResource("lang/" + fileName, false);
        }
    }
    
    public String getMessage(String key, Object... replacements) {
        // 检查缓存
        if (messageCache.containsKey(key)) {
            return formatMessage(messageCache.get(key), replacements);
        }
        
        // 从配置获取消息
        String message = langConfig.getString(key);
        if (message == null) {
            message = "Missing message: " + key;
            plugin.getLogger().warning("找不到语言键: " + key);
        }
        
        // 缓存消息
        messageCache.put(key, message);
        
        return formatMessage(message, replacements);
    }
    
    public String getMessageWithPrefix(String key, Object... replacements) {
        return prefix + getMessage(key, replacements);
    }
    
    private String formatMessage(String message, Object... replacements) {
        String formattedMessage = ChatColor.translateAlternateColorCodes('&', message);
        
        // 处理替换
        if (replacements != null && replacements.length > 0) {
            if (replacements.length % 2 == 0) {
                for (int i = 0; i < replacements.length; i += 2) {
                    if (replacements[i] != null && replacements[i + 1] != null) {
                        formattedMessage = formattedMessage.replace(
                                "%" + replacements[i] + "%", replacements[i + 1].toString());
                    }
                }
            } else {
                plugin.getLogger().warning("替换参数数量不正确: " + message);
            }
        }
        
        return formattedMessage;
    }
    
    public String getPrefix() {
        return prefix;
    }
} 