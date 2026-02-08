package me.duck.rePlugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import org.bukkit.GameMode;
import org.bukkit.util.StringUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class GameModeCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Тест, только в игре!");
            return true;
        }
        if (!player.hasPermission("rePlugin.gm") || !(player.isOp())) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        if (args.length < 1) {
            player.sendMessage("§cUse: /gamemode <gamemode> [player]");
            return true;
        }
        Player target;
        if (args.length >= 2) {
            target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage("§cPlayer not found!"); return true;
            }
        } else  {
            target = player;
        }

        switch(args[0].toLowerCase()) {
            case "creative": case "c": case "1":
                target.setGameMode(GameMode.CREATIVE);
                break;
            case "survival": case "s": case "0":
                target.setGameMode(GameMode.SURVIVAL);
                break;
            case "adventure": case "a": case "2":
                target.setGameMode(GameMode.ADVENTURE);
                break;
            default:
                player.sendMessage("§cUnknown gamemode!");
                return true;
        }

        return true;
    }
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String @NotNull [] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList("0", "1", "2"), suggestions);
        }
        else if (args.length == 2) {
            return null;
        }
        return suggestions;
    }
}