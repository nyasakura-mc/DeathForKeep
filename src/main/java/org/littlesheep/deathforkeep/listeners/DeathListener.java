/*
  死亡监听器
  处理玩家死亡事件
 */
package org.littlesheep.deathforkeep.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.GameMode;
import org.littlesheep.deathforkeep.DeathForKeep;
import org.littlesheep.deathforkeep.utils.Messages;
import org.littlesheep.deathforkeep.data.PlayerData;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Map;
import java.util.UUID;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class DeathListener implements Listener {

    private final DeathForKeep plugin;
    // 用于存储玩家的物品备份，使用ConcurrentHashMap提高并发性能
    private final Map<UUID, ItemStack[]> inventoryBackups = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> armorBackups = new ConcurrentHashMap<>();
    private final Map<UUID, ItemStack[]> enderChestBackups = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> expBackups = new ConcurrentHashMap<>();
    private final Map<UUID, Collection<PotionEffect>> potionEffectBackups = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> foodLevelBackups = new ConcurrentHashMap<>();
    private final Map<UUID, Float> saturationBackups = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> fireTicks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> backupTimestamps = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> pendingRestore = new ConcurrentHashMap<>();
    private final Map<UUID, DeathBackupData> completeBackups = new ConcurrentHashMap<>();
    private int periodicTaskId = -1;
    
    // 死亡备份数据类，存储所有需要备份的玩家数据
    private class DeathBackupData {
        @SuppressWarnings("unused")
        private final UUID playerUUID;
        @SuppressWarnings("unused")
        private final long timestamp;
        private final ItemStack[] inventory;
        private final ItemStack[] armor;
        private final ItemStack[] enderChest;
        private final ItemStack[] extraSlots; // 副手等
        private final int experience;
        private final int level;
        private final float exp;
        private final Collection<PotionEffect> potionEffects;
        private final int foodLevel;
        private final float saturation;
        private final int fireTicks;
        private final int airLevel;
        private final double health;
        @SuppressWarnings("unused")
        private final GameMode gameMode;
        
        public DeathBackupData(Player player) {
            this.playerUUID = player.getUniqueId();
            this.timestamp = System.currentTimeMillis();
            this.inventory = player.getInventory().getContents().clone();
            this.armor = player.getInventory().getArmorContents().clone();
            this.extraSlots = player.getInventory().getExtraContents().clone();
            this.enderChest = player.getEnderChest().getContents().clone();
            this.experience = player.getTotalExperience();
            this.level = player.getLevel();
            this.exp = player.getExp();
            this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.fireTicks = player.getFireTicks();
            this.airLevel = player.getRemainingAir();
            this.health = player.getHealth();
            this.gameMode = player.getGameMode();
        }
        
        // 恢复所有数据到玩家
        public void restore(Player player) {
            // 清空当前物品栏，防止物品重叠
            player.getInventory().clear();
            
            // 恢复物品
            player.getInventory().setContents(inventory);
            player.getInventory().setArmorContents(armor);
            player.getInventory().setExtraContents(extraSlots);
            
            // 恢复末影箱
            player.getEnderChest().setContents(enderChest);
            
            // 恢复经验
            player.setTotalExperience(0);
            player.setLevel(0);
            player.setExp(0);
            if (level > 0) {
                player.setLevel(level);
                player.setExp(exp);
            } else {
                player.giveExp(experience);
            }
            
            // 恢复药水效果
            for (PotionEffect effect : player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.addPotionEffects(potionEffects);
            
            // 恢复饥饿和饱和度
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
            
            // 恢复火焰时间
            player.setFireTicks(fireTicks);
            
            // 恢复空气值
            player.setRemainingAir(airLevel);
            
            // 恢复生命值（使用更新的API替代已弃用的getMaxHealth()）
            if (health > 0 && health <= player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()) {
                player.setHealth(Math.min(health, player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()));
            }
            
            // 更新物品栏
            player.updateInventory();
        }
    }

    public DeathListener(DeathForKeep plugin) {
        this.plugin = plugin;
        
        // 注册任务以定期清理过期备份（根据配置的时间自动清理）
        int cleanupTime = plugin.getConfig().getInt("backup.cleanup-time", 3600); // 默认1小时
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            backupTimestamps.entrySet().removeIf(entry -> 
                (now - entry.getValue() > cleanupTime * 1000)); // 根据配置的秒数转换为毫秒
            
            // 清理所有备份数据
            List<UUID> toRemove = new ArrayList<>();
            for (UUID uuid : completeBackups.keySet()) {
                if (!backupTimestamps.containsKey(uuid)) {
                    toRemove.add(uuid);
                }
            }
            
            for (UUID uuid : toRemove) {
                completeBackups.remove(uuid);
                pendingRestore.remove(uuid);
                inventoryBackups.remove(uuid);
                armorBackups.remove(uuid);
                enderChestBackups.remove(uuid);
                expBackups.remove(uuid);
                potionEffectBackups.remove(uuid);
                foodLevelBackups.remove(uuid);
                saturationBackups.remove(uuid);
                fireTicks.remove(uuid);
            }
            
            if (plugin.getConfig().getBoolean("backup.debug-mode", false) && !toRemove.isEmpty()) {
                plugin.getLogger().info("已清理 " + toRemove.size() + " 个过期的备份数据");
            }
        }, 20 * 60 * 10, 20 * 60 * 10); // 每10分钟执行一次
        
        // 启动定期备份任务
        startPeriodicBackupTask();
    }
    
    /**
     * 启动定期备份任务
     */
    private void startPeriodicBackupTask() {
        if (plugin.getConfig().getBoolean("backup.periodic.enabled", true)) {
            int interval = plugin.getConfig().getInt("backup.periodic.interval", 20); // 默认1秒（20刻）
            boolean onlyProtected = plugin.getConfig().getBoolean("backup.periodic.only-protected", true);
            
            // 取消现有任务
            stopPeriodicBackupTask();
            
            // 创建新任务
            periodicTaskId = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    // 如果设置只备份有保护的玩家，则检查玩家是否有保护
                    if (onlyProtected && !plugin.hasActiveProtection(player.getUniqueId())) {
                        continue;
                    }
                    
                    // 执行备份
                    createBackup(player);
                }
            }, interval, interval).getTaskId();
            
            if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                plugin.getLogger().info("已启动定期备份任务（间隔：" + interval + "刻）");
            }
        }
    }
    
    /**
     * 停止定期备份任务
     */
    private void stopPeriodicBackupTask() {
        if (periodicTaskId != -1) {
            Bukkit.getScheduler().cancelTask(periodicTaskId);
            periodicTaskId = -1;
            
            if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                plugin.getLogger().info("已停止定期备份任务");
            }
        }
    }
    
    /**
     * 为指定玩家创建备份
     * @param player 要备份的玩家
     */
    private void createBackup(Player player) {
        if (!plugin.getConfig().getBoolean("use-inventory-backup", true) || 
            !plugin.getConfig().getBoolean("backup.enabled", true)) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        try {
            // 创建完整备份
            DeathBackupData backup = new DeathBackupData(player);
            completeBackups.put(playerUUID, backup);
            backupTimestamps.put(playerUUID, System.currentTimeMillis());
            
            // 同时保留单独的备份，用于兼容现有代码
            inventoryBackups.put(playerUUID, player.getInventory().getContents().clone());
            armorBackups.put(playerUUID, player.getInventory().getArmorContents().clone());
            
            // 根据配置决定是否进行全面备份
            if (plugin.getConfig().getBoolean("backup.comprehensive", true)) {
                enderChestBackups.put(playerUUID, player.getEnderChest().getContents().clone());
                expBackups.put(playerUUID, player.getTotalExperience());
                potionEffectBackups.put(playerUUID, player.getActivePotionEffects());
                foodLevelBackups.put(playerUUID, player.getFoodLevel());
                saturationBackups.put(playerUUID, player.getSaturation());
                fireTicks.put(playerUUID, player.getFireTicks());
            }
            
            if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                plugin.getLogger().info("已为玩家 " + player.getName() + " 创建定期备份");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("为玩家 " + player.getName() + " 创建备份时出错: " + e.getMessage());
            if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                e.printStackTrace();
            }
        }
    }

    // 在死亡事件的最早阶段备份物品 (LOWEST优先级确保我们是最先处理的)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeathEarly(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        
        if (plugin.hasActiveProtection(playerUUID)) {
            // 备份玩家物品和经验(如果启用了备份功能)
            if (plugin.getConfig().getBoolean("use-inventory-backup", true) && 
                plugin.getConfig().getBoolean("backup.enabled", true) && 
                plugin.getConfig().getBoolean("backup.periodic.save-on-death", true)) {
                
                // 使用共用方法创建备份
                createBackup(player);
                
                // 标记为等待恢复
                pendingRestore.put(playerUUID, true);
                
                if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                    plugin.getLogger().info("已为玩家 " + player.getName() + " 创建死亡时刻备份");
                }
            }
            
            // 尝试设置保持物品
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }
    
    // 使用NORMAL优先级重新确认死亡事件的设置
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerDeathNormal(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        
        if (plugin.hasActiveProtection(playerUUID)) {
            // 再次确认设置，防止被其他插件覆盖
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }
    
    // 使用HIGH优先级再次确认死亡事件的设置
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeathHigh(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUniqueId();
        
        if (plugin.hasActiveProtection(playerUUID)) {
            // 再次确认设置，防止被其他插件覆盖
            event.setKeepInventory(true);
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            event.getDrops().clear();
        }
    }
    
    // 监听玩家重生事件，恢复物品 (LOWEST优先级确保我们是最先尝试恢复的)
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerRespawnEarly(PlayerRespawnEvent event) {
        tryRestorePlayerData(event.getPlayer(), 1);
    }
    
    // 使用NORMAL优先级再次尝试恢复物品
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawnNormal(PlayerRespawnEvent event) {
        tryRestorePlayerData(event.getPlayer(), 5);
    }
    
    // 使用HIGH优先级再次尝试恢复物品
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerRespawnHigh(PlayerRespawnEvent event) {
        tryRestorePlayerData(event.getPlayer(), 10);
    }
    
    // 使用HIGHEST优先级最后一次尝试恢复
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawnHighest(PlayerRespawnEvent event) {
        tryRestorePlayerData(event.getPlayer(), 20);
    }
    
    // 使用MONITOR优先级对恢复进行最终检查
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawnMonitor(PlayerRespawnEvent event) {
        final Player player = event.getPlayer();
        final UUID uuid = player.getUniqueId();
        
        // 再次确认最终恢复
        if (pendingRestore.containsKey(uuid) && pendingRestore.get(uuid)) {
            int finalDelay = plugin.getConfig().getInt("backup.final-delay", 40); // 默认2秒(40刻)
            
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // 最后的恢复检查
                if (pendingRestore.containsKey(uuid) && pendingRestore.get(uuid)) {
                    // 如果正常恢复没有成功，使用最终的完全恢复方法
                    if (completeBackups.containsKey(uuid)) {
                        completeBackups.get(uuid).restore(player);
                        
                        if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                            plugin.getLogger().info("已为玩家 " + player.getName() + " 执行最终备份恢复");
                        }
                        
                        pendingRestore.put(uuid, false);
                    }
                }
            }, finalDelay);
        }
    }
    
    // 尝试使用指定的延迟恢复玩家数据
    private void tryRestorePlayerData(Player player, long delay) {
        if (!plugin.getConfig().getBoolean("use-inventory-backup", true) || 
            !plugin.getConfig().getBoolean("backup.enabled", true)) {
            return;
        }
        
        UUID playerUUID = player.getUniqueId();
        
        if (pendingRestore.containsKey(playerUUID) && pendingRestore.get(playerUUID)) {
            Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    try {
                        if (!player.isOnline()) return;
                        
                        // 检查是否已经恢复或是否仍需要恢复
                        if (!pendingRestore.containsKey(playerUUID) || !pendingRestore.get(playerUUID)) {
                            return;
                        }
                        
                        // 优先使用完整备份恢复
                        if (completeBackups.containsKey(playerUUID)) {
                            completeBackups.get(playerUUID).restore(player);
                            
                            if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                                plugin.getLogger().info("已使用完整备份恢复玩家 " + player.getName() + " 的数据（延迟: " + delay + "）");
                            }
                            
                            pendingRestore.put(playerUUID, false);
                            return;
                        }
                        
                        // 使用旧的单独备份方式恢复
                        if (inventoryBackups.containsKey(playerUUID)) {
                            // 恢复物品
                            if (inventoryBackups.get(playerUUID) != null) {
                                player.getInventory().setContents(inventoryBackups.get(playerUUID));
                            }
                            
                            if (armorBackups.get(playerUUID) != null) {
                                player.getInventory().setArmorContents(armorBackups.get(playerUUID));
                            }
                            
                            // 恢复经验
                            if (expBackups.containsKey(playerUUID)) {
                                player.setTotalExperience(0);
                                player.setLevel(0);
                                player.setExp(0);
                                player.giveExp(expBackups.get(playerUUID));
                            }
                            
                            // 如果启用了全面备份，恢复额外数据
                            if (plugin.getConfig().getBoolean("backup.comprehensive", true)) {
                                // 恢复药水效果
                                if (potionEffectBackups.containsKey(playerUUID)) {
                                    for (PotionEffect effect : player.getActivePotionEffects()) {
                                        player.removePotionEffect(effect.getType());
                                    }
                                    player.addPotionEffects(potionEffectBackups.get(playerUUID));
                                }
                                
                                // 恢复饥饿度和饱和度
                                if (foodLevelBackups.containsKey(playerUUID)) {
                                    player.setFoodLevel(foodLevelBackups.get(playerUUID));
                                }
                                
                                if (saturationBackups.containsKey(playerUUID)) {
                                    player.setSaturation(saturationBackups.get(playerUUID));
                                }
                                
                                // 恢复火焰效果
                                if (fireTicks.containsKey(playerUUID)) {
                                    player.setFireTicks(fireTicks.get(playerUUID));
                                }
                                
                                // 恢复末影箱内容
                                if (enderChestBackups.containsKey(playerUUID) && enderChestBackups.get(playerUUID) != null) {
                                    player.getEnderChest().setContents(enderChestBackups.get(playerUUID));
                                }
                            }
                            
                            // 更新物品栏
                            player.updateInventory();
                            
                            if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                                plugin.getLogger().info("已使用单独备份恢复玩家 " + player.getName() + " 的数据（延迟: " + delay + "）");
                            }
                            
                            pendingRestore.put(playerUUID, false);
                        }
                    } catch (Exception e) {
                        plugin.getLogger().severe("恢复玩家 " + player.getName() + " 的数据时出错（延迟: " + delay + "）: " + e.getMessage());
                        if (plugin.getConfig().getBoolean("backup.debug-mode", false)) {
                            e.printStackTrace();
                        }
                    }
                }
            }, delay);
        }
    }
    
    // 在玩家登出时清理数据
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        
        // 延迟清理，确保其他插件处理完成
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            cleanupPlayerBackups(uuid);
        }, 100L); // 5秒后清理
    }
    
    // 清理玩家的备份数据
    private void cleanupPlayerBackups(UUID uuid) {
        completeBackups.remove(uuid);
        pendingRestore.remove(uuid);
        inventoryBackups.remove(uuid);
        armorBackups.remove(uuid);
        enderChestBackups.remove(uuid);
        expBackups.remove(uuid);
        potionEffectBackups.remove(uuid);
        foodLevelBackups.remove(uuid);
        saturationBackups.remove(uuid);
        fireTicks.remove(uuid);
        backupTimestamps.remove(uuid);
    }
    
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        UUID uuid = player.getUniqueId();
        
        // 检查是否有保护
        if (plugin.hasActiveProtection(uuid)) {
            PlayerData playerData = plugin.getPlayerData(uuid);
            
            if (playerData != null) {
                // 获取保护等级
                String level = playerData.getProtectionLevel();
                ConfigurationSection levelConfig = null;
                
                if (level != null) {
                    levelConfig = plugin.getConfig().getConfigurationSection("protection-levels." + level);
                }
                
                // 保留经验值
                boolean keepExp = playerData.isKeepExp() || 
                                 (levelConfig != null && levelConfig.getBoolean("keep-exp", false));
                if (keepExp) {
                    event.setKeepLevel(true);
                    event.setDroppedExp(0);
                }
                
                // 避免死亡惩罚
                boolean noDeathPenalty = playerData.isNoDeathPenalty() || 
                                        (levelConfig != null && levelConfig.getBoolean("no-death-penalty", false));
                if (noDeathPenalty) {
                    // 避免其他死亡惩罚（例如饥饿值减少、耐久度减少等）
                    // 这里可以添加相关实现
                }
                
                // 保留物品
                event.setKeepInventory(true);
                event.getDrops().clear();
                
                // 播放粒子效果
                String particleEffect = playerData.getParticleEffect();
                if (particleEffect == null && levelConfig != null) {
                    particleEffect = levelConfig.getString("particle-effect");
                }
                
                if (particleEffect != null && plugin.areParticlesEnabled(uuid)) {
                    try {
                        final String effectName = particleEffect;
                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            try {
                                Particle particle = Particle.valueOf(effectName.toUpperCase());
                                player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 
                                                               50, 0.5, 0.5, 0.5, 0.1);
                            } catch (Exception ex) {
                                plugin.getColorLogger().error("无效的粒子效果类型: " + effectName);
                            }
                        }, 5L);
                    } catch (Exception e) {
                        plugin.getColorLogger().error("显示粒子效果出错: " + e.getMessage());
                    }
                }
                
                // 播放声音
                if (plugin.getConfig().getBoolean("sounds.protection-used.enabled", true)) {
                    String soundName = plugin.getConfig().getString("sounds.protection-used.sound", "ENTITY_TOTEM_USE");
                    float volume = (float) plugin.getConfig().getDouble("sounds.protection-used.volume", 1.0);
                    float pitch = (float) plugin.getConfig().getDouble("sounds.protection-used.pitch", 1.0);
                    
                    try {
                        Sound sound = Sound.valueOf(soundName.toUpperCase());
                        player.getWorld().playSound(player.getLocation(), sound, volume, pitch);
                    } catch (Exception e) {
                        plugin.getColorLogger().error("无效的声音类型: " + soundName);
                    }
                }
                
                // 发送消息
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.sendMessage(plugin.getMessages().getMessage("protection.activated"));
                }, 20L);
                
                // 即时保存玩家数据，防止出现意外
                plugin.savePlayerData(uuid);
            }
        }
    }
    
    /**
     * 在指定位置生成粒子效果
     * 此方法保留以便日后使用
     * @param location 要生成粒子的位置
     */
    @SuppressWarnings("unused")
    private void spawnProtectionParticles(Location location) {
        if (location == null || location.getWorld() == null) {
            plugin.getLogger().warning("无法生成粒子效果: 位置或世界为null");
            return;
        }
        
        try {
            String particleType = plugin.getConfig().getString("particles.type", "TOTEM");
            int count = plugin.getConfig().getInt("particles.count", 50);
            
            try {
                Particle particle = Particle.valueOf(particleType);
                location.getWorld().spawnParticle(particle, location, count, 0.5, 1.0, 0.5, 0.1);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的粒子类型: " + particleType + "，使用默认TOTEM");
                location.getWorld().spawnParticle(Particle.TOTEM, location, count, 0.5, 1.0, 0.5, 0.1);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("生成粒子效果时出错: " + e.getMessage());
        }
    }
    
    /**
     * 广播玩家死亡保护消息
     * 此方法保留以便日后使用
     * @param player 死亡的玩家
     */
    @SuppressWarnings("unused")
    private void broadcastDeathProtection(Player player) {
        String broadcastRange = plugin.getConfig().getString("broadcast-range", "world");
        Messages messages = plugin.getMessages();
        String message = messages.getMessage("death.broadcast", "player", player.getName());
        
        if ("server".equalsIgnoreCase(broadcastRange)) {
            // 全服广播
            Bukkit.broadcastMessage(message);
        } else if ("world".equalsIgnoreCase(broadcastRange)) {
            // 同世界广播
            World world = player.getWorld();
            for (Player p : world.getPlayers()) {
                p.sendMessage(message);
            }
        } else if ("none".equalsIgnoreCase(broadcastRange)) {
            // 不广播
            return;
        } else {
            // 默认同世界广播
            World world = player.getWorld();
            for (Player p : world.getPlayers()) {
                p.sendMessage(message);
            }
        }
    }
    
    // 在插件禁用时停止所有任务
    public void onDisable() {
        stopPeriodicBackupTask();
    }
} 