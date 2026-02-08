package me.duck.rePlugin;

import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Listener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class JoinListener implements Listener {
    private final RePlugin plugin;
    private final Connection connection;

    public static String spawnLocation = null;

    public JoinListener(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
        updateSpawnCache();
    }

    private void updateSpawnCache() {
        Bukkit.getScheduler().runTask(plugin, () -> {
            spawnLocation = getData("spawn");
        });
    }

    public static final Map<UUID, Integer> saveTimeForPlayer = new HashMap<>();
    public static final Map<UUID, Integer> playerMoneyCache = new HashMap<>();

    private int getTimeFromDB(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return 0;
        String sql = "SELECT gametimer FROM playersData WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("gametimer");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private int getMoneyFromDB(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player == null) return 0;
        String sql = "SELECT money FROM bank WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("money");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private String getData(String was) {
        String sql = "SELECT vvalue FROM otherData WHERE nname = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, was);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("vvalue");
        } catch (SQLException | NumberFormatException e) {
            return null;
        }
        return null;
    }

    private void saveTime(Player player, int time) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = "INSERT INTO playersData (uuid, gametimer) VALUES (?, ?) ON CONFLICT (uuid) DO UPDATE SET gametimer = EXCLUDED.gametimer";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, player.getUniqueId().toString());
                    pstmt.setInt(2, time);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private String timeConvertor(int time) {
        if (time < 60) return time + "s";
        else if (time < 3600) return (time / 60) + "m" + (time % 60) + "s";
        else if (time < 3600 * 24) return (time / 3600) + "h " + ((time % 3600)/60) + "m";
        else return (time/(3600*24)) + "d " + ((time % (3600*24))/3600) + "h";
    }

//Integer.parseInt(Float.toString(time / 3600))
    private void addMoney(Player player, int money) {
        int currentMoney = playerMoneyCache.getOrDefault(player.getUniqueId(), 0);
        int newMoney = money + currentMoney;
        playerMoneyCache.put(player.getUniqueId(), newMoney);

        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e+ " + money));
        updateBalance(player, newMoney);

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String sql = "INSERT INTO bank (uuid, money) VALUES (?, ?) ON CONFLICT (uuid) DO UPDATE SET money = money + EXCLUDED.money";
                try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                    pstmt.setString(1, player.getUniqueId().toString());
                    pstmt.setInt(2, money);
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                player.sendMessage("§cUse only numbers or data error!");
            }
        });

    }

    public void createScoreboard(Player player, int money, String tt) {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        Scoreboard scoreboard = manager.getNewScoreboard();

        Objective obj = scoreboard.registerNewObjective("scoreboard", "dummy", "§aINFO");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        obj.getScore("  ").setScore(5);
        obj.getScore("§e☺§7Player: §e" + player.getName()).setScore(4);
        obj.getScore("§a✪§7Money: §a" + money).setScore(3);
        obj.getScore("§7◔Time: " + tt).setScore(2);
        obj.getScore(" ").setScore(1);
        obj.getScore("§ecreepywild.gomc.fun").setScore(0);

        player.setScoreboard(scoreboard);
    }

    public void updateBalance(Player player, int money) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("scoreboard");

        Integer rawTime = saveTimeForPlayer.getOrDefault(player.getUniqueId(), 0);
        String timeTime = timeConvertor(rawTime);

        if (obj == null) {
            createScoreboard(player, money, timeTime);
            return;
        }

        for (String entry : board.getEntries()) {
            if (entry.contains("§a✪§7Money")) board.resetScores(entry);
            if (entry.contains("§7◔Time")) board.resetScores(entry);
        }

        obj.getScore("§a✪§7Money: §a" + money).setScore(3);
        obj.getScore("§7◔Time: " + timeTime).setScore(2);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);
        Player player = event.getPlayer();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int gametime = getTimeFromDB(player.getUniqueId());
            int money = getMoneyFromDB(player.getUniqueId());

            Bukkit.getScheduler().runTask(plugin, () -> {
                playerMoneyCache.put(player.getUniqueId(), money);
                saveTimeForPlayer.put(player.getUniqueId(), gametime);
                updateBalance(player, money);
            });
        });

        for (UUID vanished : ToVanisch.vanishPLayers) {
            Player vanichedPlayer = Bukkit.getPlayer(vanished);
            if (vanichedPlayer != null && !player.hasPermission("rePlugin.vanish.see")) {
                player.hidePlayer(plugin, vanichedPlayer);
            }
        }

        player.sendMessage("§eHello, §b" + player.getName() + "!");
        if (player.isOp()) {
            int players = Bukkit.getOnlinePlayers().size();
            player.sendMessage("§aAll players: " + players);
//            Component message = Component.text("§eAdmin Join: §4" + player.getName());
//            Bukkit.broadcast(message);
        }
    }
    @EventHandler
    public void onMobDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        EntityType entityType = event.getEntityType();

        if (killer == null || !(event.getEntity() instanceof Monster)) return;
        double reward = 1;

        switch (entityType) {
            case WITHER: reward = 5.0; break;
            case ENDERMAN: reward = 1.2; break;
        }

        if (Math.random() < 0.35) {
            int rew = (int) (Math.random() * 10 * reward);
            if (rew < 1) rew = 1;
            addMoney(killer, rew);
        }
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (event.getEntity() instanceof Player p && ToVanisch.vanishPLayers.contains(p.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerSpawn(AsyncPlayerSpawnLocationEvent event) {
        if (event.isNewPlayer()) {
            String rawCords = getData("spawn");
            if (rawCords != null) {
                Location loc = parseLocation(rawCords);
                if (loc != null) event.setSpawnLocation(loc);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        String rawCords = getData("spawn");
        if (getData("spawn") != null && player.getRespawnLocation() == null) {
           Location loc = parseLocation(spawnLocation);
           if (loc != null) event.setRespawnLocation(loc);
        }
    }

    private Location parseLocation(String rawLocation) {
        try {
            String [] cords = rawLocation.split(" ");
            if (cords.length >= 3) {
                double x = Double.parseDouble(cords[0]);
                double y = Double.parseDouble(cords[1]);
                double z = Double.parseDouble(cords[2]);
                return new Location(Bukkit.getWorld("world"), x + 0.5, y, z + 0.5);
            }
        } catch (Exception ignored) {}
        return null;
    }

    @EventHandler
    public void inQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        Player player = event.getPlayer();

        sendStop(player);

        if (saveTimeForPlayer.containsKey(player.getUniqueId())) {
            saveTime(player, saveTimeForPlayer.get(player.getUniqueId()));
            saveTimeForPlayer.remove(player.getUniqueId());
        }
        playerMoneyCache.remove(player.getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        if (event.getEntity() instanceof Player player) {
            sendStop(player);
        }
    }

    private void sendStop(Player player) {
        UUID uuid = player.getUniqueId();
        boolean canceled = false;

        if (RtpLocation.pendingTasks.containsKey(uuid)) {
            RtpLocation.pendingTasks.get(uuid).cancel();
            RtpLocation.pendingTasks.remove(uuid);
            canceled = true;
        }
        if (ToHome.pendingTasks.containsKey(uuid)) {
            ToHome.pendingTasks.get(uuid).cancel();
            ToHome.pendingTasks.remove(uuid);
            canceled = true;
        }
        if (ToSpawnLocation.pendingTasks.containsKey(uuid)) {
            ToSpawnLocation.pendingTasks.get(uuid).cancel();
            ToSpawnLocation.pendingTasks.remove(uuid);
            canceled = true;
        }
        if (TpTpa.pendingTasks.containsKey(uuid)) {
            TpTpa.pendingTasks.get(uuid).cancel();
            TpTpa.pendingTasks.remove(uuid);
            canceled = true;

            UUID targerUUID = null;
            for (Map.Entry<UUID, UUID> entry : TpTpa.activeTargets.entrySet()) {
                if (entry.getValue().equals(player.getUniqueId())) {
                    targerUUID = entry.getKey();
                    break;
                }
            }
            if (targerUUID != null) {
                TpTpa.activeTargets.remove(targerUUID);
                Player target = Bukkit.getPlayer(targerUUID);
                if (target != null && target.isOnline()) {
                    target.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                    target.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cTELEPORT CANCELED!"));
                }
            }

        } else if (TpTpa.activeTargets.containsKey(uuid)) {
            UUID senderUUID = TpTpa.activeTargets.get(uuid);
            TpTpa.activeTargets.remove(uuid);
            canceled = true;

            if (senderUUID != null && TpTpa.pendingTasks.containsKey(senderUUID)) {
                TpTpa.pendingTasks.get(senderUUID).cancel();
                TpTpa.pendingTasks.remove(senderUUID);

                Player sender = Bukkit.getPlayer(senderUUID);
                if (sender != null && sender.isOnline()) {
                    sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cTELEPORT CANCELED!"));
                    sender.sendMessage("§cTeleport was canceled!");
                    sender.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                }
            }

        }

        if (canceled) {
            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§cTELEPORT CANCELED!"));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ() || event.getFrom().getY() != event.getTo().getY()) {
            double dist = event.getFrom().distance(event.getTo());
            if (dist < 0.05) return;
            sendStop(player);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            sendStop(player);
        }
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player attacker) {
            sendStop(attacker);
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player attacker) {
            sendStop(attacker);
        }
    }

    @EventHandler
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        GameMode gameMode = event.getNewGameMode();
        if (gameMode == GameMode.SPECTATOR) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0, false, false));
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false));
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 2f);
        } else if (player.getGameMode() == GameMode.SPECTATOR) {
            if (player.hasPotionEffect(PotionEffectType.NIGHT_VISION)) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
            if (player.hasPotionEffect(PotionEffectType.GLOWING)) {
                player.removePotionEffect(PotionEffectType.GLOWING);
            }
            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.5f, 2f);
        }
    };
}
