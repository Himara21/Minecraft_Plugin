package me.duck.rePlugin;

import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;

import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TpTpa implements CommandExecutor{
    public static final Map<UUID, UUID> waitingPlayer = new HashMap<>();
    public static final Map<UUID, UUID> activeTargets = new HashMap<>();
    public static final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();

    private final Map<String, Long> requestCooldowns = new HashMap<>();

    private final RePlugin plugin;
    private final Connection connection;
    public TpTpa(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    private void bukT(Player sender, Player target) {
        UUID suuid = sender.getUniqueId();

        if (pendingTasks.containsKey(suuid)) {
            pendingTasks.get(suuid).cancel();
        }
        activeTargets.put(target.getUniqueId(), sender.getUniqueId());
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 10;

            @Override
            public void run() {
                if (!sender.isOnline() || !target.isOnline()) {
                    if (sender.isOnline()) {
                        sender.sendMessage("§cPlayer is Offline!");
                        sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                    }
                    this.cancel();
                    pendingTasks.remove(suuid);
                    activeTargets.remove(target.getUniqueId());
                    return;
                }
                if (timeLeft > 0) {
                    sender.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§eTime left: §d" + timeLeft));
                    sender.playSound(sender.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    timeLeft--;
                } else {
                    sender.teleport(target);
                    sender.playSound(sender.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT,1.0f, 1.0f);
                    sender.playSound(target.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT,1.0f, 1.0f);
                    sender.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                            new TextComponent("§aGoodluck! :)"));
                    this.cancel();
                    pendingTasks.remove(suuid);
                    activeTargets.remove(target.getUniqueId());
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pendingTasks.put(suuid, task);
    }

    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }

        String commandName = cmd.getName().toLowerCase();
        UUID playerUUID = player.getUniqueId();

        if (commandName.equals("tpa")) {
            if (pendingTasks.containsKey(playerUUID)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou cannot send requests while teleporting!");
                return true;
            }
            if (!player.hasPermission("rePlugin.tpa.use")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou do not have permission to use this command!");
                return true;
            }
            if (args.length < 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cUsage: /tpa [player]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cPlayer not found!");
                return true;
            }
            if (target.hasPermission("rePlugin.tpa.lock")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou cannot send a request to this player!");
                return true;
            }
            if (target.getUniqueId().equals(playerUUID)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou cannot send a request to yourself!");
                return true;
            }
            if (!player.getWorld().getName().equals("world") || !target.getWorld().getName().equals("world")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                player.sendMessage("§cTeleportation is only available in the normal world!");
                return true;
            }
            String cooldownKey = playerUUID.toString() + ":" + target.getUniqueId().toString();
            if (requestCooldowns.containsKey(cooldownKey)) {
                long unlockTime = requestCooldowns.get(cooldownKey);
                if (System.currentTimeMillis() < unlockTime) {
                    long secondTime = (unlockTime - System.currentTimeMillis()) / 1000;
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                    player.sendMessage("§cWait " + secondTime + "s for the request!");
                    return true;
                }
            }
            waitingPlayer.put(target.getUniqueId(), playerUUID);
            requestCooldowns.put(cooldownKey, System.currentTimeMillis() + 10000);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
            player.sendMessage("§aRequest send to §e" + target.getName());

            target.playSound(target.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
            target.sendMessage("§e" + player.getName() + " §awants to teleport to you!");
            target.sendMessage("§7Write §a/tpaaccept §7 or §c/tpadeny");
            target.sendMessage("§7The request will expire in 10 seconds.");

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                UUID currentSender = waitingPlayer.get(target.getUniqueId());
                if (currentSender != null && currentSender.equals(playerUUID)) {
                    waitingPlayer.remove(target.getUniqueId());
                    if (player.isOnline()) player.sendMessage("§cThe request to " + target.getName() + " has expired.");
                    if (target.isOnline()) target.sendMessage("Request from " + player.getName() + " has expired.");
                }
            }, 20L * 60);
            return true;
        }
        if (commandName.equals("tpaaccept")) {
            if (pendingTasks.containsKey(playerUUID)) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou cannot accept requests while teleporting!");
                return true;
            }
            UUID senderUUID = waitingPlayer.remove(playerUUID);
            if (senderUUID == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou have no active teleportation requests!");
                return true;
            }
            if (!player.hasPermission("rePlugin.tpa.request")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cYou do not have permission to use this command!");
                return true;
            }
            Player requester = Bukkit.getPlayer(senderUUID);
            if (requester != null && requester.isOnline()) {
                bukT(requester, player);
                player.sendMessage("§aRequest accepted!");
                player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.0f);
                requester.sendMessage("§aPlayer §e" + player.getName() + " §aaccepted your request! Do not move!");
                requester.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.0f);
            } else {
                player.sendMessage("§cThe sender of the request is not online.");
            }
            return true;
        }
        if (commandName.equals("tpadeny")) {
            UUID senderUuid = waitingPlayer.remove(playerUUID);
            if (senderUuid != null) {
                player.sendMessage("§cYou rejected the request.");
                Player requester = Bukkit.getPlayer(senderUuid);
                if (requester != null && requester.isOnline()) {
                    requester.playSound(requester.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1.0f);
                    requester.sendMessage("§cYour request to " + player.getName() + " has been denied.");
                }
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS,1.0f, 1.0f);
                player.sendMessage("§cNo requests to deny.");
            }
            return true;
        }
        if (commandName.equals("tpalock")) {
            UUID uuid = player.getUniqueId();

            LuckPerms luckPerms = LuckPermsProvider.get();
            User user = luckPerms.getUserManager().getUser(uuid);
            if (user == null) {
                player.sendMessage("§cError, write admins in Discord!");
                return true;
            }
            String permissionNode = "rePlugin.tpa.lock";
            Node node = Node.builder(permissionNode).build();

            if (user.getNodes().stream().anyMatch(n -> n.getKey().equals(permissionNode))) {
                user.data().remove(node);
                player.sendMessage("§aYou have §2ENABLED §arequests for teleportation.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
            } else {
                user.data().add(node);
                player.sendMessage("§aYou have §4DISABLED §arequests for teleportation.");
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 0.5f);
            }
            luckPerms.getUserManager().saveUser(user);
        }
        return true;
    }
}
