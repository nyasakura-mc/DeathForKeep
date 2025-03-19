/*
  数据库管理器
  处理数据库的初始化、连接和数据操作
 */
package org.littlesheep.deathforkeep.data;

import org.littlesheep.deathforkeep.DeathForKeep;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class DatabaseManager {
    private final DeathForKeep plugin;
    private Connection connection;
    private final Map<String, Connection> connectionPool = new ConcurrentHashMap<>();
    private final int MAX_POOL_SIZE = 10;
    
    public DatabaseManager(DeathForKeep plugin) {
        this.plugin = plugin;
        initializeDatabase();
    }
    
    private void initializeDatabase() {
        File dataFolder = new File(plugin.getDataFolder(), "database");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
        
        String url = "jdbc:sqlite:" + new File(dataFolder, "deathkeep.db").getAbsolutePath();
        
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);
            
            // 初始化连接池
            for (int i = 0; i < MAX_POOL_SIZE; i++) {
                Connection conn = DriverManager.getConnection(url);
                connectionPool.put("conn-" + i, conn);
            }
            
            try (Statement statement = connection.createStatement()) {
                // 创建玩家数据表
                statement.execute("CREATE TABLE IF NOT EXISTS player_data (" +
                        "uuid TEXT PRIMARY KEY, " +
                        "expiry_time BIGINT, " +
                        "particles_enabled BOOLEAN DEFAULT 1, " +
                        "shared_with TEXT DEFAULT NULL)");
            }
        } catch (SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "无法初始化数据库", e);
        }
    }
    
    /**
     * 从连接池获取一个连接
     */
    private Connection getConnection() throws SQLException {
        // 如果池中有连接，返回一个可用的连接
        for (Map.Entry<String, Connection> entry : connectionPool.entrySet()) {
            Connection conn = entry.getValue();
            if (conn != null && !conn.isClosed()) {
                return conn;
            }
        }
        
        // 如果没有可用连接，创建一个新的
        String url = "jdbc:sqlite:" + new File(plugin.getDataFolder(), "database/deathkeep.db").getAbsolutePath();
        return DriverManager.getConnection(url);
    }
    
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            
            // 关闭所有连接池中的连接
            for (Connection conn : connectionPool.values()) {
                if (conn != null && !conn.isClosed()) {
                    conn.close();
                }
            }
            connectionPool.clear();
            plugin.getLogger().info("数据库连接已成功关闭");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "关闭数据库连接时出错: " + e.getMessage(), e);
        } finally {
            connection = null;
        }
    }
    
    public void savePlayerData(UUID uuid, long expiryTime, boolean particlesEnabled, UUID sharedWith) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO player_data (uuid, expiry_time, particles_enabled, shared_with) VALUES (?, ?, ?, ?)")) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, expiryTime);
            ps.setBoolean(3, particlesEnabled);
            ps.setString(4, sharedWith != null ? sharedWith.toString() : null);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "保存玩家数据时出错: " + uuid, e);
        }
    }
    
    /**
     * 异步保存玩家数据
     */
    public CompletableFuture<Void> savePlayerDataAsync(UUID uuid, long expiryTime, boolean particlesEnabled, UUID sharedWith) {
        return CompletableFuture.runAsync(() -> {
            savePlayerData(uuid, expiryTime, particlesEnabled, sharedWith);
        }).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "异步保存玩家数据时出错: " + uuid, ex);
            return null;
        });
    }
    
    public Map<UUID, PlayerData> loadAllPlayerData() {
        Map<UUID, PlayerData> playerDataMap = new HashMap<>();
        
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT * FROM player_data")) {
            
            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                long expiryTime = rs.getLong("expiry_time");
                boolean particlesEnabled = rs.getBoolean("particles_enabled");
                String sharedWithStr = rs.getString("shared_with");
                UUID sharedWith = sharedWithStr != null ? UUID.fromString(sharedWithStr) : null;
                
                PlayerData data = new PlayerData(uuid, expiryTime, particlesEnabled, sharedWith);
                playerDataMap.put(uuid, data);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "加载玩家数据时出错", e);
        }
        
        return playerDataMap;
    }
    
    public void removePlayerData(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("DELETE FROM player_data WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "删除玩家数据时出错: " + uuid, e);
        }
    }
    
    public void resetAllData() {
        try (Statement statement = connection.createStatement()) {
            statement.execute("DELETE FROM player_data");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "重置所有数据时出错", e);
        }
    }
    
    public void updateParticlesEnabled(UUID uuid, boolean enabled) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET particles_enabled = ? WHERE uuid = ?")) {
            ps.setBoolean(1, enabled);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "更新粒子设置时出错: " + uuid, e);
        }
    }
    
    public void updateSharedWith(UUID uuid, UUID sharedWith) {
        try (PreparedStatement ps = connection.prepareStatement(
                "UPDATE player_data SET shared_with = ? WHERE uuid = ?")) {
            ps.setString(1, sharedWith != null ? sharedWith.toString() : null);
            ps.setString(2, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "更新共享设置时出错: " + uuid, e);
        }
    }
} 