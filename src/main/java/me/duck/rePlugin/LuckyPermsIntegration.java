package me.duck.rePlugin;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class LuckyPermsIntegration {
    private final LuckPerms luckyPerms = LuckPermsProvider.get();
    private final RePlugin plugin;

    public LuckyPermsIntegration(RePlugin plugin) {
        this.plugin = plugin;
        register();
    }

    private void register() {
        luckyPerms.getEventBus().subscribe(plugin, UserDataRecalculateEvent.class, event -> {
            User user = event.getUser();
            Player player = Bukkit.getPlayer(user.getUniqueId());

//            if (player != null && player.isOnline()) {
//                Bukkit.getScheduler().runTask(plugin, () -> {
//                    //update(player);
//                });
//            }
        });
    }
}

//    private void update(Player player) {
//        String prefix = getPlayerPrefix(player);
//        player.setDisplayName(prefix + " " + player.getName());
//        player.setPlayerListName(prefix + " " + player.getName());
//    }

//    public String getPlayerGroup(Player player) {
//        User user = luckyPerms.getUserManager().getUser(player.getUniqueId());
//        if (user == null) return "default";
//        return user.getPrimaryGroup();
//    }
//    public String getTeamPriority(Player player) {
//        String group = getPlayerGroup(player);
//        switch (group.toLowerCase()) {
//            case "duck": return "001duck";
//            case "admin": return "002admin";
//            case "moder": return "003moder";
//            default: return "100player";
//        }
//    }

//    public String getPlayerPrefix(Player player) {
//        User user = luckyPerms.getUserManager().getUser(player.getUniqueId());
//        if (user == null) return "";
//        String prefix = user.getCachedData().getMetaData().getPrefix();
//        return prefix != null ? prefix : "";
//    }
//}
