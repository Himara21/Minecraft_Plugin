package me.duck.rePlugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Sound;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Date;

public class RtpLocation implements CommandExecutor {
    private final RePlugin plugin;
    public RtpLocation(RePlugin plugin) {
        this.plugin = plugin;
    }
    private final Map<UUID, Long> coldowns = new HashMap<>();
    public static final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();
    public static final Map<UUID, Location> foundLocations = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }

        if (!player.hasPermission("rePlugin.rtp.use")) {
            player.sendMessage("§cYou do not have permission to use this command!");
            return true;
        }

        if (!player.getWorld().getName().equals("world")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cTeleportation is only available in the normal world!");
            return true;
        }

        long currentTime = System.currentTimeMillis();
        long lastTime = coldowns.getOrDefault(player.getUniqueId(), 0L);
        if (lastTime > currentTime) {
            long secondTime = (lastTime - currentTime) / 1000;
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 1.0f);
            player.sendMessage("§cTime left: " + secondTime + "s");
            return true;
        }

        if (pendingTasks.containsKey(player.getUniqueId())) {
            player.sendMessage("§cYou are already preparing to teleport!");
            return true;
        }
        bukT(player);
        startRtp(player, 0);
        return true;
    }

    private void bukT(Player player) {
        if (pendingTasks.containsKey(player.getUniqueId())) return;
        player.sendMessage("§e" + player.getName() + "§a accept a teleport!");
        BukkitTask task = new BukkitRunnable() {
            int timeLeft = 10;
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelAll(player.getUniqueId());
                    return;
                }

                if (timeLeft > 0) {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§eTime left: §d" + timeLeft));
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1.0f, 1.0f);
                    timeLeft--;
                } else {
                    Location loc = foundLocations.get(player.getUniqueId());

                    if (loc != null) {
                        rrra(player, loc);
                        cancelAll(player.getUniqueId());
                    } else {
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR,
                                new TextComponent("§cPlease wait :)"));

                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
        pendingTasks.put(player.getUniqueId(), task);
    }

    private void startRtp(Player player, int attempts) {
        if (attempts >= 10 || !player.isOnline() || !pendingTasks.containsKey(player.getUniqueId())) return;

        int x = ThreadLocalRandom.current().nextInt(-10000, 10000);
        int z = ThreadLocalRandom.current().nextInt(-10000, 10000);
        World world = Bukkit.getWorld("world");

        world.getChunkAtAsync(x >> 4, z >> 4).thenAccept(chunk -> {
            Block block = Objects.requireNonNull(world).getHighestBlockAt(x, z);

            if (block.getType() == Material.WATER || block.getType() == Material.LAVA) {
                startRtp(player, attempts + 1);
                return;
                //block.getLocation().add(0.5, 1.0, 0.5);
            }

            foundLocations.put(player.getUniqueId(), block.getLocation().add(0.5, 1, 0.5));
        });
    }

    private void rrra(Player player, Location location) {
        double x = location.getX();
        double z = location.getZ();
        double y = location.getY();

        Location loc = new Location(Bukkit.getWorld("world"), x, y, z, 180, 0);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("TELEPORT..."));
        player.teleport(loc);
        if (!player.hasPermission("rePlugin.rtp.nodelay")) {
            coldowns.put(player.getUniqueId(), System.currentTimeMillis() + (10 * 1000));
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 20, 4, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, false, false));
        player.spawnParticle(org.bukkit.Particle.CLOUD, loc, 50, 0.5, 0.5, 0.5, 0.1);
    }
    public static void cancelAll(UUID uuid) {
        if (pendingTasks.containsKey(uuid)) {
            pendingTasks.get(uuid).cancel();
            pendingTasks.remove(uuid);
        }
        foundLocations.remove(uuid);
    }

}