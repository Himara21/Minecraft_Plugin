package me.duck.rePlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToSpawnLocation implements CommandExecutor {
    private final RePlugin plugin;
    private final Connection connection;
    public static final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();
    public ToSpawnLocation(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    private void bukT(Player player, Location location) {
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
                    player.teleport(location);
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§aWelcome to spawn :)"));
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

        if (!player.hasPermission("rePlugin.spawn.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        String commandName = cmd.getName().toLowerCase();

        if (commandName.equals("spawn")) {
            if (!player.getWorld().getName().equals("world")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                player.sendMessage("§cTeleportation is only available in the normal world!");
                return true;
            }
            String sql = "SELECT vvalue FROM otherData WHERE nname = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, "spawn");
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String vvalue = rs.getString("vvalue");
                    String [] cords = vvalue.split(" ");
                    if (cords.length >= 3) {
                        double x = Double.parseDouble(cords[0]);
                        double y = Double.parseDouble(cords[1]);
                        double z = Double.parseDouble(cords[2]);
                        Location location = new Location(Bukkit.getWorld("world"), x, y, z);
                        bukT(player, location);
                    }
                }
            } catch (SQLException | NumberFormatException e) {
                player.sendMessage("§cSpawn is empty!");
            }
            return true;
        } else if (commandName.equals("set_spawn")) {
            if (!player.hasPermission("rePlugin.spawn.set")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                player.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage("§cUse /" + label + " <x> <y> <z>>");
                return true;
            }
            try {
                double x = Double.parseDouble(args[0]);
                double y = Double.parseDouble(args[1]);
                double z = Double.parseDouble(args[2]);
                String sql = "INSERT INTO otherData (nname, vvalue) VALUES (?, ?) ON CONFLICT (nname) DO UPDATE SET vvalue = EXCLUDED.vvalue";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, "spawn");
                    pstmt.setString(2, args[0] + " " + args[1] + " " + args[2]);
                    pstmt.executeUpdate();
                }
                JoinListener.spawnLocation = args[0] + " " + args[1] + " " + args[2];
                player.sendMessage("§aNew Spawn - x:" + x + " y:" + y + " z:" + z);
            } catch (NumberFormatException | SQLException e) {
                player.sendMessage("§cUse only numbers or data error!");
                e.printStackTrace();
            }
        }
        return false;
    }
}
