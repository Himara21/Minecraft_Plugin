package me.duck.rePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ToVanisch implements CommandExecutor {
    public static final Set<UUID> vanishPLayers = new HashSet<>();
    private final RePlugin plugin;
    public ToVanisch(RePlugin plugin) {
        this.plugin = plugin;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        if (!player.hasPermission("rePlugin.vanish.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        if (vanishPLayers.contains(player.getUniqueId())) {
            player.sendMessage("Vanish: §4off!");
            vanishPLayers.remove(player.getUniqueId());
            for (Player p : Bukkit.getOnlinePlayers()) {
                p.showPlayer(plugin, player);
            }
        } else {
            vanishPLayers.add(player.getUniqueId());
            player.sendMessage("Vanish: §aon!");
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (!p.hasPermission("rePlugin.vanish.see")) {
                    p.hidePlayer(plugin, player);
                }
            }
        }
        return true;
    }
}
