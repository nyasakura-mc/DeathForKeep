package org.littlesheep.deathforkeep.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.littlesheep.deathforkeep.DeathForKeep;

import java.util.UUID;

public class PlaceholderHook extends PlaceholderExpansion {

    private final DeathForKeep plugin;

    public PlaceholderHook(DeathForKeep plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "deathkeep";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        switch (identifier) {
            case "time":
                return plugin.getRemainingTimeFormatted(player.getUniqueId());
            case "status":
                return plugin.hasActiveProtection(player.getUniqueId()) ? 
                       plugin.getMessages().getMessage("placeholder.status-active") :
                       plugin.getMessages().getMessage("placeholder.status-expired");
            case "particles":
                return plugin.getPlayerData(player.getUniqueId()).isParticlesEnabled() ?
                       plugin.getMessages().getMessage("gui.common.enabled") :
                       plugin.getMessages().getMessage("gui.common.disabled");
            case "share_status":
                UUID sharedWith = plugin.getPlayerData(player.getUniqueId()).getSharedWith();
                if (sharedWith != null) {
                    String playerName = Bukkit.getOfflinePlayer(sharedWith).getName();
                    return plugin.getMessages().getMessage("placeholder.shared-with")
                            .replace("%player%", playerName != null ? playerName : "Unknown");
                }
                return plugin.getMessages().getMessage("placeholder.not-shared");
        }

        return null;
    }
} 