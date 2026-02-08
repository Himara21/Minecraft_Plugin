package me.duck.rePlugin;

import org.bukkit.NamespacedKey;
import org.bukkit.Bukkit;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class OnEnchantCompleter implements CommandExecutor, TabCompleter {
    private final RePlugin plugin;
    private final Connection connection;
    public OnEnchantCompleter(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        if (!player.hasPermission("rePlugin.customench.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        if (args.length < 2) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cUse /" + label + " <enchant> <level>");
            return true;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType().isAir()) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            sender.sendMessage("§cHand is empty!");
            return true;
        }
        try {
            Enchantment enchant = Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(args[0].toLowerCase()));
            int level = Integer.parseInt(args[1]);
            if (enchant == null) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                sender.sendMessage("§cUncorrect enchantment!");
                return true;
            }
            if (level > 1000) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                sender.sendMessage("§cMaximum enchantment level is 1000!");
                return true;
            }
            item.addUnsafeEnchantment(enchant, level);
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
            player.sendMessage("§aSuccess enchantment added! §e" + enchant + " " + level);
        } catch (NumberFormatException e) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            sender.sendMessage("§cError! Please try again!");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (!(sender instanceof Player player)) return completions;
        if (args.length == 1) {
            ItemStack item = player.getInventory().getItemInMainHand();
            List<String> enchantNames;
            if (item.getType().isAir()) {
                enchantNames = Registry.ENCHANTMENT.stream()
                        .map(enchant -> enchant.getKey().getKey())
                        .collect(Collectors.toList());
            } else {
                enchantNames = Registry.ENCHANTMENT.stream()
                        .filter(enchant -> enchant.canEnchantItem(item))
                        .map(enchant -> enchant.getKey().getKey())
                        .collect(Collectors.toList());
                if (enchantNames.isEmpty()) {
                    enchantNames = Registry.ENCHANTMENT.stream()
                            .map(enchant -> enchant.getKey().getKey())
                            .collect(Collectors.toList());
                }
            }
            StringUtil.copyPartialMatches(args[0], enchantNames, completions);
        }
        return completions;
    }
}
