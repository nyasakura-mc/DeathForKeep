package org.littlesheep.deathforkeep.utils;

import org.bukkit.ChatColor;
import java.util.logging.Logger;

public class ColorLogger {
    private final Logger logger;
    private final String version;

    public ColorLogger(Logger logger) {
        this.logger = logger;
        this.version = "1.0.0-SNAPSHOT"; // 默认版本号
    }
    
    public ColorLogger(Logger logger, String version) {
        this.logger = logger;
        this.version = version;
    }

    public static String format(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public void info(String message) {
        logger.info(format("&a[INFO] &f" + message));
    }

    public void error(String message) {
        logger.severe(format("&c[ERROR] &f" + message));
    }

    public void logStartup() {
        String[] logo = {
            "&6 ____             _   _     _____           _  __              ",
            "&6|  _ \\  ___  __ _| |_| |__ |  ___|__  _ __ | |/ /___  ___ _ __ ",
            "&6| | | |/ _ \\/ _` | __| '_ \\| |_ / _ \\| '_ \\| ' // _ \\/ _ \\ '_ \\",
            "&6| |_| |  __/ (_| | |_| | | |  _| (_) | | | | . \\  __/  __/ |_) |",
            "&6|____/ \\___|\\__,_|\\__|_| |_|_|  \\___/|_| |_|_|\\_\\___|\\___| .__/",
            "&6                                                          |_|   "
        };
        
        for (String line : logo) {
            logger.info(format(line));
        }
        
        logger.info(format("&6=========================================================="));
        logger.info(format("&6 DeathForKeep &fv" + version));
        logger.info(format("&6 作者: LittleSheep"));
        logger.info(format("&6=========================================================="));
    }
    
    public void logShutdown() {
        logger.info(format("&6=========================================================="));
        logger.info(format("&6 DeathForKeep &f已关闭"));
        logger.info(format("&6=========================================================="));
    }
    
    public void logReload() {
        logger.info(format("&6=========================================================="));
        logger.info(format("&6 DeathForKeep &f已重新加载"));
        logger.info(format("&6=========================================================="));
    }
} 