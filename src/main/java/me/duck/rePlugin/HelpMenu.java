package me.duck.rePlugin;

import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.format.NamedTextColor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import java.util.logging.Level;

public class HelpMenu implements CommandExecutor, Listener {
    private final RePlugin plugin;
    private final Connection connection;

    public HelpMenu(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }

    public void filler(Inventory gui) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fillerMeta = filler.getItemMeta();
        if (fillerMeta != null) {
            fillerMeta.setDisplayName(" ");
            filler.setItemMeta(fillerMeta);
        }
        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) gui.setItem(i, filler);
        }
    }

    public Inventory mainMenu(Player player) {
        Inventory gui = Bukkit.createInventory(new MenuHolder(), 27, "Menu");
        ItemStack item = new ItemStack(Material.BOOKSHELF);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName("§6§lLegendary Enchantments");
            meta.setLore(List.of("§7Open Enchantment menu"));
            item.setItemMeta(meta);
        }
        gui.setItem(13, item);
        filler(gui);
        return gui;
    }

    public void closeMenu(Player player) {
        Inventory gui = mainMenu(player);
        player.openInventory(gui);
    }

    public void charInfoMenu(Player player) {
        Inventory gui = Bukkit.createInventory(new CharInfoMenu(), 54, "CharInfoMenu");
        ItemStack vampirism = new ItemStack(Material.ENCHANTED_BOOK);
        ItemStack curseCore = coreForCraft("MAGMA_CREAM");
        ItemMeta vampirismMeta = vampirism.getItemMeta();

        ItemStack closeMenu = new ItemStack(Material.BARRIER);
        ItemMeta closeMenuMeta = closeMenu.getItemMeta();

        if (vampirismMeta != null) {
            vampirismMeta.setDisplayName("§4Vampirism");
            vampirismMeta.setLore(List.of("§7Heals the owner on hit.", "", "§fRecipe:", "§5Curse Core x1", "§7Totem x4", "§7Ghast Tear x4", "", "§a1000✪"));
            vampirism.setItemMeta(vampirismMeta);
        }

        if (closeMenuMeta != null) {
            closeMenuMeta.setDisplayName("§cBack");
            closeMenuMeta.setLore(List.of(""));
            closeMenu.setItemMeta(closeMenuMeta);
        }

        gui.setItem(16, vampirism);
        gui.setItem(23, curseCore);
        gui.setItem(49, closeMenu);
        filler(gui);
        player.openInventory(gui);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        if (!player.hasPermission("rePlugin.menu.use")) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
        Inventory gui = mainMenu(player);
        player.openInventory(gui);
        return true;
    }

    private Boolean checkCrafting (@NotNull Player player, ItemStack[] materials) {
        Inventory inventory = player.getInventory();
        Map<ItemStack, Integer> required = new HashMap<>();

        for (ItemStack material : materials) {
            if (material == null || material.getType() == Material.AIR) continue;
            ItemStack key = material.clone();
            key.setAmount(1);
            required.put(key, required.getOrDefault(key,0) + 1);
        }

        for (Map.Entry<ItemStack, Integer> entry : required.entrySet()) {
            ItemStack neededItem = entry.getKey();
            int neededAmount = entry.getValue();
            int foundAmount = 0;

            for (ItemStack invItem : inventory.getContents()) {
                if (invItem == null || invItem.getType() == Material.AIR) continue;
                if (isSimilar(neededItem, invItem)) {
                    foundAmount += invItem.getAmount();
                }
            }

            if (foundAmount < neededAmount) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                ItemMeta meta = entry.getKey().getItemMeta();
                if (meta != null && meta.hasDisplayName()) {
                    player.sendMessage(Component.text("§cYou are missing: §f")
                            .append(entry.getKey().getItemMeta().displayName()));
                } else {
                    String name = neededItem.getType().name().replace("_", " ").toLowerCase();
                    player.sendMessage("§cYou are missing: §f" + (neededAmount - foundAmount) + "x " + name);
                }
                return false;
            }
        }
        return true;
    }

    private boolean isSimilar(ItemStack needed, ItemStack inInv) {
        if (needed.getType() != inInv.getType()) return false;

        ItemMeta neededMeta = needed.getItemMeta();
        ItemMeta inInvMeta = inInv.getItemMeta();
        if (neededMeta == null || inInvMeta == null) return true;

        for (NamespacedKey key : neededMeta.getPersistentDataContainer().getKeys()) {
            if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                if(!inInvMeta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) return false;

                int neededVal = neededMeta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                int inInvVal = inInvMeta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);

                if (neededVal != inInvVal) return false;
            }
        }
        return true;
    }

    private void removeSimularItems(@NotNull Player player, ItemStack[] materials) {
        Inventory inventory = player.getInventory();

        for (ItemStack toRemove : materials) {
            if (toRemove == null || toRemove.getType() == Material.AIR) continue;

            int remaining = toRemove.getAmount();
            ItemStack[] contents = inventory.getContents();

            for (int i = 0; i < contents.length; i++) {
                ItemStack invItem = contents[i];
                if (invItem == null || invItem.getType() == Material.AIR) continue;

                if (isSimilar(invItem, toRemove)) {
                    int stackAmount = invItem.getAmount();

                    if (stackAmount <= remaining) {
                        remaining -= stackAmount;
                        inventory.setItem(i, null);
                    } else {
                        invItem.setAmount(stackAmount - remaining);
                        remaining = 0;
                    }
                }
                if (remaining <= 0) break;
            }
        }
    }

    private void update (@NotNull Player player, int money) {
        Scoreboard board = player.getScoreboard();
        Objective obj = board.getObjective("scoreboard");
        for (String entry : board.getEntries()) {
            if (entry.contains("§a✪§7Money")) {
                board.resetScores(entry);
                break;
            }
        }
        Score mon = obj.getScore("§a✪§7Money: §a" + money);
        mon.setScore(3);
    }

    private boolean checkMoney (@NotNull Player player, int amount, boolean minus) {
        String sql = "SELECT money FROM bank WHERE uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, player.getUniqueId().toString());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int money = rs.getInt("money");

                if (money >= amount) {
                    if (minus) {
                        String sql2 = "UPDATE bank SET money = money - ? WHERE uuid = ?";
                        try (PreparedStatement pstmt2 = connection.prepareStatement(sql2)) {
                            pstmt2.setString(2, player.getUniqueId().toString());
                            pstmt2.setInt(1, amount);
                            pstmt2.executeUpdate();
                        }
                        update(player, money - amount);
                        if (JoinListener.playerMoneyCache.containsKey(player.getUniqueId())) {
                            JoinListener.playerMoneyCache.put(player.getUniqueId(), money - amount);
                        }
                    }
                    return true;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private void playCraftEffects(Player player) {
        Location location = player.getLocation().add(0, 1, 0);
        World world = player.getWorld();

        world.playSound(location, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1, 1);
        world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1, 1);
        world.playSound(location, Sound.ITEM_TOTEM_USE, 1, 1);

        world.spawnParticle(Particle.ENCHANT, location, 60, 0.5, 0.5, 0.5, 0.2);
        world.spawnParticle(Particle.HAPPY_VILLAGER, location, 30, 0.4, 0.4, 0.4, 0.1);
        world.spawnParticle(Particle.WITCH, location, 40, 0.3, 0.3, 0.3, 0.1);
    }

    private ItemStack coreForCraft (String nameCore) {
        if (nameCore == null) {nameCore = "BARRIER";}
        ItemStack curseCore = new ItemStack(Material.matchMaterial(nameCore));
        ItemMeta curseCoreMeta = curseCore.getItemMeta();
        if (curseCoreMeta != null) {
            NamespacedKey coreKey = new NamespacedKey(plugin, nameCore.toLowerCase());
            curseCoreMeta.getPersistentDataContainer().set(coreKey, PersistentDataType.INTEGER, 1);

            switch (nameCore) {
                case "MAGMA_CREAM" -> {
                        curseCoreMeta.setDisplayName("§d§l✮§r §5Curse Core §d§l✮");
                        curseCoreMeta.setLore(List.of("§7Required for crafting Ancient Grimoires.", "", "§fRecipe:", "§7Magma Cream x1", "§7Ghast Tear x4", "§7Enchanted Golden Apple x4"));
                }
                case "ECHO_SHARD" -> curseCoreMeta.setDisplayName("§1Forgotten Fragment");
                case "IRON_INGOT" -> curseCoreMeta.setDisplayName("§eLegendary Ingot");
                default -> curseCoreMeta.setDisplayName("§cERROR!");
            }
            curseCoreMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
            curseCoreMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            curseCore.setItemMeta(curseCoreMeta);
        }
        return curseCore;
    }

    private void giveVampirismBook (Player player) {
        ItemStack book = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = book.getItemMeta();
        if (meta == null) return;
        String tag = "§r§f§r";
        NamespacedKey key = new NamespacedKey(plugin, "vampirism");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, 1);
        List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<>();
        lore.add(tag + "§4Vampirism " + "I");
        meta.setLore(lore);
        book.setItemMeta(meta);
        player.getInventory().addItem(book);

        HashMap<Integer, ItemStack> leftOver = player.getInventory().addItem(book);
        if (!leftOver.isEmpty()) {
            player.sendMessage("§cInventory full! Item dropped.");
            for (ItemStack drop : leftOver.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), drop);
            }
        }
    }

    @EventHandler
    public void onGuiClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);

            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;


            if (event.getRawSlot() == 13) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                player.closeInventory();
                charInfoMenu(player);
            } else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            }
        }
        if (event.getInventory().getHolder() instanceof CharInfoMenu) {
            event.setCancelled(true);
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

            if (event.getRawSlot() == 16) { //VAMPIRISM
                ItemStack curseCore = coreForCraft("MAGMA_CREAM");

                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                ItemMeta coreMeta = curseCore.getItemMeta();
                coreMeta.setLore(List.of("§7Required for crafting Ancient Grimoires."));
                curseCore.setItemMeta(coreMeta);
                ItemStack totem = new ItemStack(Material.TOTEM_OF_UNDYING);
                ItemStack ghastTear = new ItemStack(Material.GHAST_TEAR);
                ItemStack[] recipe = {curseCore, totem, totem, totem, totem, ghastTear, ghastTear, ghastTear, ghastTear};

                if (!checkMoney(player, 1000, false)) {
                    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
                    player.sendMessage("§cYou don't have enough money!");
                    return;
                }
                if (checkCrafting(player, recipe)) {
                    removeSimularItems(player, recipe);
                    checkMoney(player, 1000, true);
                    giveVampirismBook(player);
                    playCraftEffects(player);
                    player.sendMessage("§aCrafted successfully!");
                }
            } else if (event.getRawSlot() == 23) { //CURSE CORE
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
                ItemStack curseCore = coreForCraft("MAGMA_CREAM");
                ItemMeta coreMeta = curseCore.getItemMeta();
                coreMeta.setLore(List.of("§7Required for crafting Ancient Grimoires."));
                curseCore.setItemMeta(coreMeta);
                ItemStack magmaCream = new ItemStack(Material.MAGMA_CREAM);
                ItemStack ghastTear = new ItemStack(Material.GHAST_TEAR);
                ItemStack enchGApple = new ItemStack(Material.ENCHANTED_GOLDEN_APPLE);

                ItemStack[] recipe = {magmaCream, ghastTear, ghastTear, ghastTear, ghastTear, enchGApple, enchGApple, enchGApple, enchGApple};
                if (checkCrafting(player, recipe)) {
                    removeSimularItems(player, recipe);
                    player.getInventory().addItem(curseCore);
                    playCraftEffects(player);
                    player.sendMessage("§aCrafted successfully!");
                }
            } else if (event.getRawSlot() == 49) { //CLOSE MENU
                closeMenu(player);
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1, 1);
            }
            else {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            }
        }
    }
}
