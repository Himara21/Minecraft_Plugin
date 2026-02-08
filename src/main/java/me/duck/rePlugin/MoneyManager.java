package me.duck.rePlugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MoneyManager implements CommandExecutor {
    private final RePlugin plugin;
    private final Connection connection;
    public MoneyManager(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    private void update (@NotNull Player player, int money) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("scoreboard");
        for (String entry : board.getEntries()) {
            if (entry.contains("§a✪§7Money")) {
                board.resetScores(entry);
                break;
            }
        }
        Score mon = obj.getScore("§a✪§7Money: §a" + money);
        mon.setScore(3);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }

        if (!player.hasPermission("rePlugin.ec.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length >= 3) {
            if (args[0].equals("set")) {
                if (!player.hasPermission("rePlugin.ec.set")) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    player.sendMessage("§c'" + args[0] + "' is not available!");
                    return true;
                }
                try {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) return true;
                    int amount = Integer.parseInt(args[2]);
                    String sql = "INSERT INTO bank (uuid, money) VALUES (?, ?) ON CONFLICT (uuid) DO UPDATE SET money = EXCLUDED.money";
                    try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                        pstmt.setString(1, target.getUniqueId().toString());
                        pstmt.setInt(2, amount);
                        pstmt.executeUpdate();
                        player.sendMessage("§a" + target.getName() + " money: §e" + amount);
                        update(target, amount);
                    }
                } catch (NumberFormatException | SQLException e) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
                    player.sendMessage("§cError! Please try again!");
                }
            }
        }
        if (args[0].equals("get")) {
            if (!player.hasPermission("rePlugin.ec.get")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§c'" + args[0] + "' is not available!");
                return true;
            }
            try {
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) return true;
                String sql = "SELECT money FROM bank WHERE uuid = ?";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, target.getUniqueId().toString());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String money = rs.getString("money");
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                        player.sendMessage("§a" + target.getName() + " money: §e" + money);
                        return true;
                    }
                }
            } catch (NumberFormatException | SQLException e) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cError! Please try again!");
            }
        }
        return true;
    }
}
