package org.littlesheep.deathforkeep.utils;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {
    
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdw])");
    
    /**
     * 将时间字符串转换为秒数
     * 支持的格式: 1s, 5m, 2h, 3d, 1w
     * s - 秒, m - 分钟, h - 小时, d - 天, w - 周
     * 
     * @param timeString 时间字符串
     * @return 秒数，如果格式无效则返回-1
     */
    public static long parseTimeToSeconds(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return -1;
        }
        
        // 尝试直接解析为数字（天数）
        try {
            return TimeUnit.DAYS.toSeconds(Long.parseLong(timeString));
        } catch (NumberFormatException ignored) {
            // 不是纯数字，继续使用模式匹配
        }
        
        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());
        long totalSeconds = 0;
        boolean found = false;
        
        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            
            switch (unit) {
                case "s":
                    totalSeconds += value;
                    break;
                case "m":
                    totalSeconds += TimeUnit.MINUTES.toSeconds(value);
                    break;
                case "h":
                    totalSeconds += TimeUnit.HOURS.toSeconds(value);
                    break;
                case "d":
                    totalSeconds += TimeUnit.DAYS.toSeconds(value);
                    break;
                case "w":
                    totalSeconds += TimeUnit.DAYS.toSeconds(value * 7);
                    break;
            }
        }
        
        return found ? totalSeconds : -1;
    }
    
    /**
     * 将秒数格式化为可读的时间字符串
     * 
     * @param seconds 秒数
     * @return 格式化的时间字符串 (例如: "1天 5小时 30分钟")
     */
    public static String formatTime(long seconds) {
        if (seconds <= 0) {
            return "0";
        }
        
        long days = TimeUnit.SECONDS.toDays(seconds);
        seconds -= TimeUnit.DAYS.toSeconds(days);
        
        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
        
        StringBuilder sb = new StringBuilder();
        
        if (days > 0) {
            sb.append(days).append("天 ");
        }
        
        if (hours > 0 || days > 0) {
            sb.append(hours).append("小时 ");
        }
        
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("分钟");
        } else {
            sb.append(seconds).append("秒");
        }
        
        return sb.toString().trim();
    }

    public static String formatTimeForCommand(int seconds) {
        if (seconds >= 86400) {
            return (seconds / 86400) + "days";
        } else if (seconds >= 3600) {
            return (seconds / 3600) + "hours";
        } else if (seconds >= 60) {
            return (seconds / 60) + "minutes";
        } else {
            return seconds + "seconds";
        }
    }
} 