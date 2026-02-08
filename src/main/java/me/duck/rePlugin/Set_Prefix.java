package me.duck.rePlugin;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

class CachedPrefix {
    String prefix;
    String color;

    CachedPrefix(String prefix, String color) {
        this.prefix = prefix;
        this.color = color;
    }
}

public class Set_Prefix implements CommandExecutor, Listener {
    private final Map<UUID, CachedPrefix> prefixCache = new HashMap<>();
    private final RePlugin plugin;
    private final Connection connection;
    public Set_Prefix(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    private void updateSQL(UUID uuid, String prefix, String color) {
        Player player = Bukkit.getPlayer(uuid);
        try {
            if (player == null) return;
            String sql = "INSERT INTO playerPrefix (uuid, prefix, color) VALUES (?, ?, ?) ON CONFLICT (uuid) DO UPDATE SET prefix = EXCLUDED.prefix, color = EXCLUDED.color;";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setString(2, prefix);
                pstmt.setString(3, color);
                pstmt.executeUpdate();
            }
        } catch (NumberFormatException | SQLException e) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            player.sendMessage("§cError! Please try again!");
        }
    }

    private void setPrefix(Player player, String prefix, String color) {
        updateSQL(player.getUniqueId(), prefix, color);
        prefixCache.put(player.getUniqueId(), new CachedPrefix(prefix, color));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage("§aNew prefix: §" + color + "§l" + prefix);


        updateTabPrefix(player, prefix, color);
        updateAllRank(player);
    }

    private String[] getPrefix(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        try {
            if (player == null) return new String[] {"", ""};
            String sql = "SELECT prefix, color FROM playerPrefix WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    String pr = rs.getString("prefix");
                    String col = rs.getString("color");
                    return new String[] {pr, col};
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new String[] {"", ""};
    }

    private void updateTabPrefix(Player player, String prefix, String color) {
        if (player == null) return;
        if (prefix.isEmpty() || color.isEmpty()) {
            player.setPlayerListName(null);
            return;
        }
        player.setPlayerListName("§" + color + "§l" + prefix + " §r§" + color + player.getName());
    }

    private void updateAllRank(Player target) {
        CachedPrefix data = prefixCache.get(target.getUniqueId());
        if (data == null) return;
        String prefix = data.prefix;
        String color = data.color;

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            updateNamePlateFor(viewer.getScoreboard(), target, prefix, color);
        }
    }

    private void updateNamePlateFor (Scoreboard board, Player target, String prefix, String color) {
        if (target == null) return;

        String weight = switch (prefix.toUpperCase()) {
            case "OWNER" -> "001_";
            case "DEV", "DUCK" -> "002_";
            case "HELPER" -> "005_";
            default -> "100_";
        };

        String teamName = weight + target.getName();
        for (Team oldTeam : board.getTeams()) {
            if (oldTeam.hasEntry(target.getName())) {
                oldTeam.removeEntry(target.getName());
            }
        }
        Team team = board.getTeam(teamName);
        if (team == null) {
            team = board.registerNewTeam(teamName);
        }

        String legacyText = "§" + color + "§l" + prefix + " §r§f";
        team.prefix(LegacyComponentSerializer.legacySection().deserialize(legacyText));
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

        if (!team.hasEntry(target.getName())) {
            team.addEntry(target.getName());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String[] data = getPrefix(uuid);
        String prefix = data[0];
        String color = data[1];
        prefixCache.put(uuid, new CachedPrefix(prefix, color));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Scoreboard scoreBoard = player.getScoreboard();

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.equals(player)) continue;

                String pPrefix;
                String pColor;

                if (prefixCache.containsKey(online.getUniqueId())) {
                    CachedPrefix pData = prefixCache.get(online.getUniqueId());
                    pPrefix = pData.prefix;
                    pColor = pData.color;
                } else {
                    String[] pData = getPrefix(online.getUniqueId());
                    pPrefix = pData[0];
                    pColor = pData[1];
                    prefixCache.put(online.getUniqueId(), new CachedPrefix(pPrefix, pColor));
                }

                updateNamePlateFor(scoreBoard, online, pPrefix, pColor);
            }

            if (!prefix.isEmpty()) {
                updateTabPrefix(player, prefix, color);
                updateAllRank(player);
            }
        }, 20L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
//        prefixCache.remove(event.getPlayer().getUniqueId());

//        Scoreboard scoreBoard = Bukkit.getScoreboardManager().getMainScoreboard();
//        Team team = scoreBoard.getTeam(event.getPlayer().getName());
//
//        if (team == null) return;
//        team.unregister();
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        CachedPrefix data = prefixCache.get(player.getUniqueId());
        if (data == null) return;
        if (data.prefix == null || data.prefix.isEmpty() || data.color == null || data.color.isEmpty()) {
            event.renderer((source, sourceDisplayName, message, viewer) -> {
                return Component.text()
                        .append(sourceDisplayName.color(NamedTextColor.GRAY))
                        .append(Component.text(" >> ", NamedTextColor.GRAY))
                        .append(message.color(NamedTextColor.WHITE))
                        .build();
            });
            return;
        }
        String legacy = "§" + data.color + "§l" + data.prefix + " ";
        Component fullPrefix = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize(legacy);

        TextColor nameColor = NamedTextColor.GRAY;
        try {
            nameColor = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().deserialize("§" + data.color).color();
        } catch (Exception ignored) {}

        TextColor finalColor = nameColor;
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            return Component.text()
                    .append(fullPrefix)
                    .append(sourceDisplayName.color(finalColor))
                    .append(Component.text(" >> ", NamedTextColor.GRAY))
                    .append(message.color(NamedTextColor.WHITE))
                    .build();
        });
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        if (!player.hasPermission("rePlugin.setprefix.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
            player.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length >= 2) {
            setPrefix(player, args[0], args[1]);
            return true;
        }
        player.sendMessage("§cUsage: /setprefix <prefix_name> <color_code>");
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
        return true;
    }
}