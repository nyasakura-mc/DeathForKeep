/*
  更新检查器
  检查插件更新并通知管理员
 */
package org.littlesheep.deathforkeep.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker implements Listener {
    
    private final DeathForKeep plugin;
    private final String owner;
    private final String repo;
    private String latestVersion;
    private String downloadUrl;
    private boolean updateAvailable = false;
    
    private static final Pattern VERSION_PATTERN = Pattern.compile("\"tag_name\":\\s*\"v?([0-9.]+)\"");
    private static final Pattern DOWNLOAD_PATTERN = Pattern.compile("\"browser_download_url\":\\s*\"([^\"]+\\.jar)\"");
    
    public UpdateChecker(DeathForKeep plugin, String owner, String repo) {
        this.plugin = plugin;
        this.owner = owner;
        this.repo = repo;
        
        if (plugin.getConfig().getBoolean("update-checker.enabled", true)) {
            checkForUpdates();
        }
    }
    
    public void checkForUpdates() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                // 获取当前版本
                String currentVersion = plugin.getDescription().getVersion();
                
                // 获取最新版本
                String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/releases/latest";
                String jsonResponse = fetchUrl(apiUrl);
                
                if (jsonResponse != null) {
                    // 解析版本号
                    Matcher versionMatcher = VERSION_PATTERN.matcher(jsonResponse);
                    if (versionMatcher.find()) {
                        latestVersion = versionMatcher.group(1);
                        
                        // 解析下载链接
                        Matcher downloadMatcher = DOWNLOAD_PATTERN.matcher(jsonResponse);
                        if (downloadMatcher.find()) {
                            downloadUrl = downloadMatcher.group(1);
                        }
                        
                        // 比较版本
                        if (isNewerVersion(currentVersion, latestVersion)) {
                            updateAvailable = true;
                            
                            // 在控制台显示更新信息
                            plugin.getLogger().info(ColorLogger.format("&a[更新] &f检测到新版本: &e" + latestVersion));
                            plugin.getLogger().info(ColorLogger.format("&a[更新] &f当前版本: &e" + currentVersion));
                            plugin.getLogger().info(ColorLogger.format("&a[更新] &f下载链接: &e" + downloadUrl));
                        } else {
                            plugin.getLogger().info(ColorLogger.format("&a[更新] &f插件已是最新版本"));
                        }
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning(ColorLogger.format("&c[错误] &f检查更新时出错: " + e.getMessage()));
            }
        });
    }
    
    private String fetchUrl(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
            connection.setRequestProperty("User-Agent", "DeathForKeep UpdateChecker");
            
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            }
        } catch (IOException e) {
            plugin.getLogger().warning(ColorLogger.format("&c[错误] &f连接到 GitHub API 时出错: " + e.getMessage()));
        }
        
        return null;
    }
    
    private boolean isNewerVersion(String currentVersion, String latestVersion) {
        String[] current = currentVersion.split("\\.");
        String[] latest = latestVersion.split("\\.");
        
        int length = Math.max(current.length, latest.length);
        
        for (int i = 0; i < length; i++) {
            int currentPart = i < current.length ? Integer.parseInt(current[i]) : 0;
            int latestPart = i < latest.length ? Integer.parseInt(latest[i]) : 0;
            
            if (latestPart > currentPart) {
                return true;
            } else if (latestPart < currentPart) {
                return false;
            }
        }
        
        return false;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (updateAvailable && player.hasPermission("deathkeep.admin") && 
                plugin.getConfig().getBoolean("update-checker.notify-admins", true)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Messages messages = plugin.getMessages();
                player.sendMessage(messages.getMessage("update.available", 
                        "version", latestVersion, 
                        "download", downloadUrl));
            }, 60L); // 3秒后通知，避免与其他插件消息冲突
        }
    }
    
    public boolean isUpdateAvailable() {
        return updateAvailable;
    }
    
    public String getLatestVersion() {
        return latestVersion;
    }
    
    public String getDownloadUrl() {
        return downloadUrl;
    }
} 