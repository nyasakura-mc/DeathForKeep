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
        Map<UUID, PlayerData> playerDataMap = new ConcurrentHashMap<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement("SELECT * FROM player_data");
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                UUID playerUUID = UUID.fromString(resultSet.getString("uuid"));
                long expiryTime = resultSet.getLong("expiry_time");
                boolean active = resultSet.getBoolean("active");
                boolean particlesEnabled = resultSet.getBoolean("particles_enabled");
                String sharedWithStr = resultSet.getString("shared_with");
                UUID sharedWith = sharedWithStr != null ? UUID.fromString(sharedWithStr) : null;
                
                // 获取新增的保护等级相关数据
                String protectionLevel = resultSet.getString("protection_level");
                boolean keepExp = resultSet.getBoolean("keep_exp");
                String particleEffect = resultSet.getString("particle_effect");
                boolean noDeathPenalty = resultSet.getBoolean("no_death_penalty");
                
                PlayerData playerData = new PlayerData(playerUUID, expiryTime, active, sharedWith);
                playerData.setParticlesEnabled(particlesEnabled);
                playerData.setProtectionLevel(protectionLevel);
                playerData.setKeepExp(keepExp);
                playerData.setParticleEffect(particleEffect);
                playerData.setNoDeathPenalty(noDeathPenalty);
                
                playerDataMap.put(playerUUID, playerData);
            }
        } catch (SQLException e) {
            plugin.getColorLogger().error("加载所有玩家数据失败: " + e.getMessage());
        } finally {
            closeResources(connection, statement, resultSet);
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
    
    public boolean createPlayerData(UUID playerUUID, long expiryTime, boolean active, boolean particlesEnabled, 
                                  UUID sharedWith, String protectionLevel, boolean keepExp, 
                                  String particleEffect, boolean noDeathPenalty) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(
                    "INSERT INTO player_data (uuid, expiry_time, active, particles_enabled, shared_with, " +
                    "protection_level, keep_exp, particle_effect, no_death_penalty) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            
            statement.setString(1, playerUUID.toString());
            statement.setLong(2, expiryTime);
            statement.setBoolean(3, active);
            statement.setBoolean(4, particlesEnabled);
            statement.setString(5, sharedWith != null ? sharedWith.toString() : null);
            statement.setString(6, protectionLevel);
            statement.setBoolean(7, keepExp);
            statement.setString(8, particleEffect);
            statement.setBoolean(9, noDeathPenalty);
            
            int result = statement.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            plugin.getColorLogger().error("创建玩家数据失败: " + e.getMessage());
            return false;
        } finally {
            closeResources(connection, statement, null);
        }
    }
    
    public boolean updatePlayerData(UUID playerUUID, long expiryTime, boolean active, boolean particlesEnabled, 
                                  UUID sharedWith, String protectionLevel, boolean keepExp, 
                                  String particleEffect, boolean noDeathPenalty) {
        Connection connection = null;
        PreparedStatement statement = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(
                    "UPDATE player_data SET expiry_time = ?, active = ?, particles_enabled = ?, " +
                    "shared_with = ?, protection_level = ?, keep_exp = ?, particle_effect = ?, " +
                    "no_death_penalty = ? WHERE uuid = ?");
            
            statement.setLong(1, expiryTime);
            statement.setBoolean(2, active);
            statement.setBoolean(3, particlesEnabled);
            statement.setString(4, sharedWith != null ? sharedWith.toString() : null);
            statement.setString(5, protectionLevel);
            statement.setBoolean(6, keepExp);
            statement.setString(7, particleEffect);
            statement.setBoolean(8, noDeathPenalty);
            statement.setString(9, playerUUID.toString());
            
            int result = statement.executeUpdate();
            return result > 0;
        } catch (SQLException e) {
            plugin.getColorLogger().error("更新玩家数据失败: " + e.getMessage());
            return false;
        } finally {
            closeResources(connection, statement, null);
        }
    }
    
    public void setupTables() {
        Connection connection = null;
        Statement statement = null;
        
        try {
            connection = getConnection();
            statement = connection.createStatement();
            
            // 创建玩家数据表
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS player_data (" +
                    "uuid VARCHAR(36) PRIMARY KEY, " +
                    "expiry_time BIGINT, " +
                    "active BOOLEAN, " +
                    "particles_enabled BOOLEAN DEFAULT TRUE, " +
                    "shared_with VARCHAR(36), " +
                    "protection_level VARCHAR(50), " +
                    "keep_exp BOOLEAN DEFAULT FALSE, " +
                    "particle_effect VARCHAR(50), " +
                    "no_death_penalty BOOLEAN DEFAULT FALSE" +
                    ")");
            
            // 检查是否需要更新表结构添加新列
            ResultSet rs = connection.getMetaData().getColumns(null, null, "player_data", "protection_level");
            if (!rs.next()) {
                // 添加新列
                statement.executeUpdate("ALTER TABLE player_data ADD COLUMN protection_level VARCHAR(50)");
                statement.executeUpdate("ALTER TABLE player_data ADD COLUMN keep_exp BOOLEAN DEFAULT FALSE");
                statement.executeUpdate("ALTER TABLE player_data ADD COLUMN particle_effect VARCHAR(50)");
                statement.executeUpdate("ALTER TABLE player_data ADD COLUMN no_death_penalty BOOLEAN DEFAULT FALSE");
                plugin.getColorLogger().info("数据库表结构已更新，添加了保护等级相关字段");
            }
            rs.close();
            
        } catch (SQLException e) {
            plugin.getColorLogger().error("设置数据库表失败: " + e.getMessage());
        } finally {
            closeResources(connection, statement, null);
        }
    }
    
    private void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
        try {
            if (resultSet != null) {
                resultSet.close();
            }
            if (statement != null) {
                statement.close();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "关闭资源时出错: " + e.getMessage(), e);
        }
    }
} 