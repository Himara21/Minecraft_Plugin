package me.duck.rePlugin;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class InventoryManager implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        String commandName = cmd.getName().toLowerCase();

        if (commandName.equals("invsee")) {
            if (!player.hasPermission("rePlugin.invsee.use")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            if (args.length < 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                player.sendMessage("§cUse /" + label + " <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;
            if (target.hasPermission("rePlugin.invsee.block")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                sender.sendMessage("§cYou can't open inventory this user!");
                return true;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
            player.openInventory(target.getInventory());
        }
        if (commandName.equals("endersee")) {
            if (!player.hasPermission("rePlugin.endersee.use")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                sender.sendMessage("§cYou don't have permission to use this command!");
                return true;
            }
            if (args.length < 1) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                player.sendMessage("§cUse /" + label + " <player>");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) return true;
            if (target.hasPermission("rePlugin.endersee.block")) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                sender.sendMessage("§cYou can't open inventory this user!");
                return true;
            }
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
            player.openInventory(target.getEnderChest());
        }
        return true;
    }
}
