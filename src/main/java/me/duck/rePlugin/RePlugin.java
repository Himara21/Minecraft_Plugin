package me.duck.rePlugin;

import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public final class RePlugin extends JavaPlugin {
    private Database database;
    //private final Map<UUID, BukkitTask> pendingTasks = new HashMap<>();

    private void updateTabList(Player player) {
        int online = Bukkit.getOnlinePlayers().size();
        int ping = player.getPing();
        String tps = String.format("%.1f", Bukkit.getTPS()[0]);

        var serializer = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection();

        String headerText =
                "\n" +
                "§7IP: §acreepywild.gomc.fun";

        String footerText =
                "\n" +
                "§7Online: §a" + online + "\n" +
                "§7Ping: §a" + ping + "§7ms" + "\n" +
                "§7TPS: §a" + tps + "\n" +
                "\n" +
                "§e§l/menu" + "\n";

        player.sendPlayerListHeaderAndFooter(
                serializer.deserialize(headerText),
                serializer.deserialize(footerText)
        );
    }

    @Override
    public void onEnable() {

        this.database = new Database();
        this.database.init(getDataFolder());

        var connection = database.getConnection();

        GameModeCommand gm = new GameModeCommand();
        RtpLocation rtp = new RtpLocation(this);
        ToSpawnLocation spawn = new ToSpawnLocation(this, connection);
        ToVanisch vanish = new ToVanisch(this);
        ToHome home = new ToHome(this, connection);
        TpTpa tpa =  new TpTpa(this, connection);
        InventoryManager inventory = new InventoryManager();
        OnEnchantCompleter customEnch = new OnEnchantCompleter(this, connection);
        LegendayEnchants legEntchant = new LegendayEnchants(this, connection);
        HelpMenu helpmenu = new HelpMenu(this, connection);
        MoneyManager moneyManager = new MoneyManager(this, connection);
        Set_Prefix setprefix = new Set_Prefix(this, connection);


        Objects.requireNonNull(getCommand("gm")).setExecutor(gm);
        Objects.requireNonNull(getCommand("gm")).setTabCompleter(gm);

        Objects.requireNonNull(getCommand("rtp")).setExecutor(rtp);

        Objects.requireNonNull(getCommand("vanish")).setExecutor(vanish);

        Objects.requireNonNull(getCommand("spawn")).setExecutor(spawn);
        Objects.requireNonNull(getCommand("set_spawn")).setExecutor(spawn);

        Objects.requireNonNull(getCommand("customench")).setExecutor(customEnch);
        Objects.requireNonNull(getCommand("customench")).setTabCompleter(customEnch);

        Objects.requireNonNull(getCommand("invsee")).setExecutor(inventory);
        Objects.requireNonNull(getCommand("endersee")).setExecutor(inventory);

        Objects.requireNonNull(getCommand("tpa")).setExecutor(tpa);
        Objects.requireNonNull(getCommand("tpaaccept")).setExecutor(tpa);
        Objects.requireNonNull(getCommand("tpadeny")).setExecutor(tpa);
        Objects.requireNonNull(getCommand("tpalock")).setExecutor(tpa);

        Objects.requireNonNull(getCommand("legench")).setExecutor(legEntchant);

        Objects.requireNonNull(getCommand("menu")).setExecutor(helpmenu);

        Objects.requireNonNull(getCommand("home")).setExecutor(home);

        Objects.requireNonNull(getCommand("ec")).setExecutor(moneyManager);

        Objects.requireNonNull(getCommand("setprefix")).setExecutor(setprefix);


        getServer().getPluginManager().registerEvents(setprefix, this);
        getServer().getPluginManager().registerEvents(helpmenu, this);
        getServer().getPluginManager().registerEvents(legEntchant, this);
        getServer().getPluginManager().registerEvents(new JoinListener(this, connection), this);
        getLogger().info("Плагин запустился!");

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (JoinListener.saveTimeForPlayer.containsKey(p.getUniqueId())) {
                    int currentTime = JoinListener.saveTimeForPlayer.get(p.getUniqueId());
                    JoinListener.saveTimeForPlayer.put(p.getUniqueId(), currentTime + 1);
                }
                updateBoard(p);
                updateTabList(p);
            }
        }, 0L, 20L);
    }

    @Override
    public void onDisable() {
        getLogger().info("Плагин выключился! Сохранение данних!");

        for (Player p : Bukkit.getOnlinePlayers()) {
            saveTime(p);
        }

        Bukkit.getScheduler().cancelTasks(this);

        try {
            if (database != null && database.getConnection() != null && !database.getConnection().isClosed()) {
                database.getConnection().close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String timeConvertor(int time) {
        if (time < 60) return time + "s";
        else if (time < 3600) return (time / 60) + "m" + (time % 60) + "s";
        else if (time < 3600 * 24) return (time / 3600) + "h " + ((time % 3600)/60) + "m";
        else return (time/(3600*24)) + "d " + ((time % (3600*24))/3600) + "h";
    }

    private void updateBoard(Player player) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("scoreboard");
        if (obj == null) return;

        for (String entry : board.getEntries()) {
            if (entry.contains("§7◔Time")) {
                board.resetScores(entry);
                break;
            }
        }
        String timeTime = timeConvertor(getTime(player));
        obj.getScore("§7◔Time: " + timeTime).setScore(2);
    }

    private Integer getTime(Player player) {
        return JoinListener.saveTimeForPlayer.getOrDefault(player.getUniqueId(), 0);
    }

    private void saveTime(Player player) {
        try {
            String sql = "INSERT INTO playersData (uuid, gametimer) VALUES (?, ?) ON CONFLICT (uuid) DO UPDATE SET gametimer = EXCLUDED.gametimer";
            try (PreparedStatement pstmt = database.getConnection().prepareStatement(sql)) {
                pstmt.setString(1, player.getUniqueId().toString());
                pstmt.setInt(2, getTime(player));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            player.sendMessage("§cUse only numbers or data error!");
            e.printStackTrace();
        }
    }
}

//    public ItemStack getCursedCore() {
//        ItemStack cursedCore = new ItemStack(Material.HEAVY_CORE);
//        ItemMeta cursedCoreMeta = cursedCore.getItemMeta();
//        if (cursedCoreMeta == null) return null;
//        cursedCoreMeta.setDisplayName("§4Desecrated Core");
//        cursedCoreMeta.setLore(List.of("§7Radiates ominous energy...", "§8Used for higher enchantments"));
//        cursedCore.setItemMeta(cursedCoreMeta);
//
//        NamespacedKey coreKey = new NamespacedKey(this, "cursed_core_recipe");
//        ShapedRecipe coreRecipe = new ShapedRecipe(coreKey, cursedCore);
//        coreRecipe.shape("NAN", "AHA", "NAN");
//        coreRecipe.setIngredient('H', Material.HEAVY_CORE);
//        coreRecipe.setIngredient('A', Material.ENCHANTED_GOLDEN_APPLE);
//        coreRecipe.setIngredient('N', Material.NETHERITE_INGOT);
//        Bukkit.addRecipe(coreRecipe);
//        return cursedCore;
//    }
//    public ItemStack getEnchantedBook(String bName) {
//        ItemStack vBook = new ItemStack(Material.ENCHANTED_BOOK);
//        ItemMeta vBookMeta = vBook.getItemMeta();
//        if (vBookMeta == null) return null;
//        NamespacedKey vBookKey = new NamespacedKey(this, bName);
//        vBookMeta.getPersistentDataContainer().set(vBookKey, PersistentDataType.INTEGER, 3);
//        vBookMeta.setDisplayName("§5Ancient Grimoire");
//        vBookMeta.setLore(List.of("§4Vampirism I", "", "§7The price of power is too high..."));
//        vBookMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
//        vBook.setItemMeta(vBookMeta);
//        return vBook;
//    }
//
//    public void VampirismBookRecipe() {
//        ItemStack cursedCore = getCursedCore();
//        if (cursedCore == null) return;
//        ItemStack enchantedBook = getEnchantedBook("vampirism");
//        if (enchantedBook == null) return;
//        NamespacedKey coreKey = new NamespacedKey(this, "vapmire_book_recipe");
//        ShapedRecipe bookRecipe = new ShapedRecipe(coreKey, enchantedBook);
//        bookRecipe.shape("TGT", "GCG", "TGT");
//        bookRecipe.setIngredient('T', Material.TOTEM_OF_UNDYING);
//        bookRecipe.setIngredient('G', Material.GHAST_TEAR);
//        bookRecipe.setIngredient('C', new RecipeChoice.ExactChoice(cursedCore));
//        Bukkit.addRecipe(bookRecipe);
//    }