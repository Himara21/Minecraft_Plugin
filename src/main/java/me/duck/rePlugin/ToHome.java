package me.duck.rePlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.maven.artifact.repository.metadata.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.sql.Connection;

public class ToHome implements CommandExecutor {
    private final RePlugin plugin;
    private final Connection connection;
    public ToHome(RePlugin plugin, Connection connection) {
        this.connection = connection;
        this.plugin = plugin;
    }
    public static final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();

    private void bukT(Player player) {
        if (pendingTasks.containsKey(player.getUniqueId())) return;

        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 10;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    pendingTasks.remove(player.getUniqueId());
                    this.cancel();
                    return;
                }

                if (timeLeft > 0) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§eTime left: §d" + timeLeft));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    timeLeft--;
                } else {
                    this.cancel();
                    pendingTasks.remove(player.getUniqueId());
                    if (player.getRespawnLocation() == null) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                        player.sendMessage("§cYou don't have bed!");
                        return;
                    }
                    player.teleport(player.getRespawnLocation());
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§aWelcome to home :)"));
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pendingTasks.put(player.getUniqueId(), task);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        if (!player.hasPermission("rePlugin.home.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        if (player.getRespawnLocation() == null) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cYou don't have bed!");
            return true;
        }
        if (!player.getWorld().getName().equals("world")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cTeleportation is only available in the normal world!");
            return true;
        }
        if (player.hasPermission("rePlugin.home.undelay")) {
            if (player.getRespawnLocation() == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                player.sendMessage("§cYou don't have bed!");
                return true;
            }
            if (args.length >= 1) {
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null || target.getRespawnLocation() == null) {
                    player.sendMessage("§4" + target.getName() + " §cdon't have bed!");
                    return true;
                }
                player.teleport(target.getRespawnLocation());
                return true;
            }
            player.teleport(player.getRespawnLocation());
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                    new TextComponent("§aWelcome to home :)"));
            return true;
        }
        bukT(player);
        return true;
    }
}
