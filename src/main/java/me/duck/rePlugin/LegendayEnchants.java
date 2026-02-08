package me.duck.rePlugin;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class LegendayEnchants implements CommandExecutor, Listener {
    private final RePlugin plugin;
    private final Connection connection;
    private final String tag = "§r§f§r";

    public void applyLifesteal(Player player, ItemStack item, int level) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String itemName = item.getType().name();
        boolean isAllowed = item.getType() == Material.ENCHANTED_BOOK
                || itemName.endsWith("_SWORD")
                || itemName.endsWith("_AXE");
        if (!isAllowed) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cOnly Books or Tools!");
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "vampirism");
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);

        updateCustomLore(meta);
        item.setItemMeta(meta);
    }
    public void Lifesteal(Player attacker, int level, EntityDamageByEntityEvent event) {
        double damage = event.getFinalDamage();
        double healAmount = damage * (level * 0.15);
        double maxHealth = attacker.getAttribute(Attribute.MAX_HEALTH).getValue();
        double currentHealth = attacker.getHealth();
        if (currentHealth < maxHealth) {
            double newHealth = Math.min(maxHealth, currentHealth + healAmount);
            attacker.setHealth(newHealth);

            attacker.getWorld().spawnParticle(
                    Particle.HEART,
                    attacker.getLocation().add(0, 1, 0),
                    3, 0.5, 0.5, 0.5, 0.1
            );
            attacker.getWorld().spawnParticle(
                    Particle.DAMAGE_INDICATOR,
                    event.getEntity().getLocation().add(0, 1, 0),
                    5, 0.2, 0.2, 0.2, 0.2
            );
            attacker.playSound(attacker.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1f);
        }
    }

    public LegendayEnchants(RePlugin plugin, Connection connection) {
        this.plugin = plugin;
        this.connection = connection;
    }
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String @NotNull [] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Nur in Game");
            return true;
        }
        if (!player.hasPermission("rePlugin.legench.use")) {
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
        String itemName = item.getType().name();
        boolean isAllowed = item.getType() == Material.ENCHANTED_BOOK
                || itemName.endsWith("_SWORD")
                || itemName.endsWith("_AXE");
        if (!isAllowed) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1, 1);
            player.sendMessage("§cOnly Books or Tools!");
            return true;
        }
        try {
            int level =  Integer.parseInt(args[1]);
            if (args[0].toLowerCase().equals("vampirism")) {
                applyLifesteal(player, item, level);
            }
        } catch (NumberFormatException e) {
            return true;
        }
        return true;
    }

    @EventHandler
    public void OnHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) return;
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (weapon.getType().isAir()) return;
        ItemMeta meta = weapon.getItemMeta();
        if (meta == null) return;
        NamespacedKey vampir = new NamespacedKey(plugin, "vampirism");
        if (meta.getPersistentDataContainer().has(vampir, PersistentDataType.INTEGER)) {
            int level = meta.getPersistentDataContainer().get(vampir, PersistentDataType.INTEGER);
            Lifesteal(attacker, level, event);
        }
    }

    public void checkTags(Player player) {
        Inventory inventory = player.getInventory();
        for (ItemStack item: inventory.getContents()) {
            if (item == null || item.getType().isAir()) continue;
            ItemMeta meta = item.getItemMeta();
            if (meta == null) continue;

            boolean hasLegendEnch = false;
            for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
                if (key.getNamespace().equals(plugin.getName().toLowerCase())) {
                    hasLegendEnch = true;
                    break;
                }
            }

            if (hasLegendEnch) {
                updateCustomLore(meta);
                item.setItemMeta(meta);
            }
        }
    }

    @EventHandler
    public void onAnvilCombie(PrepareAnvilEvent event) {
        ItemStack leftItem = event.getInventory().getItem(0);
        ItemStack rightItem = event.getInventory().getItem(1);
        if (leftItem == null || rightItem == null) return;
        Material lType = leftItem.getType();
        Material rType = rightItem.getType();
        String lName = leftItem.getType().name();
        String rName = rightItem.getType().name();

        boolean lIsWeapon = lName.endsWith("_SWORD") || lName.endsWith("_AXE");
        boolean lIsBook = (lType == Material.ENCHANTED_BOOK);
        boolean rIsWeapon = rName.endsWith("_SWORD") || rName.endsWith("_AXE");
        boolean rIsBook = (rType == Material.ENCHANTED_BOOK);

        if (!lIsWeapon && !lIsBook) return;
        if (lIsBook && !rIsBook) return;
        if (lIsWeapon && (!rIsBook && lType != rType)) return;

        ItemMeta leftMeta = leftItem.getItemMeta();
        ItemMeta rightMeta = rightItem.getItemMeta();
        if (leftMeta == null || rightMeta == null) return;
        ItemStack result = leftItem.clone();
        ItemMeta resMeta = result.getItemMeta();
        boolean wasChanged = false;

        for (NamespacedKey key : rightMeta.getPersistentDataContainer().getKeys()) {
            if (!key.getNamespace().equals(plugin.getName().toLowerCase())) continue;

            Integer levelRight = rightMeta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            if (levelRight == null) continue;
            int finalLevel = levelRight;
            if (leftMeta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER)) {
                int levelLeft = leftMeta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
                finalLevel = (levelLeft == levelRight) ? levelLeft + 1 : Math.max(levelLeft, levelRight);
            }
            if (finalLevel > 4) finalLevel = 4;
            resMeta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, finalLevel);
            wasChanged = true;
        }
        if (!wasChanged) {
            for (NamespacedKey key : leftMeta.getPersistentDataContainer().getKeys()) {
                if (key.getNamespace().equals(plugin.getName().toLowerCase())) continue;
                wasChanged = true;
                break;
            }
        }
        if (wasChanged) {
            resMeta.addEnchant(Enchantment.UNBREAKING, 1, false);
            updateCustomLore(resMeta);
            result.setItemMeta(resMeta);
            event.setResult(result);
            event.getView().setRepairCost(5 + (result.getPersistentDataContainer().getKeys().size() * 2));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        checkTags(player);
    }

    public void updateCustomLore(ItemMeta meta) {
        List<String> newLore = new ArrayList<>();
        if (meta.hasLore()) {
            for (String line : meta.getLore()) {
                if (line.startsWith(tag)) continue;
                if (line.contains("Vampirism") || line.contains("Poison") || line.contains("Freeze")) continue;
                newLore.add(line);
            }
        }
        for (NamespacedKey key : meta.getPersistentDataContainer().getKeys()) {
            if (!key.getNamespace().equals(plugin.getName().toLowerCase())) continue;

            Integer level = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
            if (level == null) continue;

            String rawName = key.getKey();
            String color = switch (rawName) {
                case "vampirism" -> "§4";
                case "poison" -> "§2";
                case "freeze" -> "§b";
                default -> "§7";
            };

            String name = rawName.substring(0, 1).toUpperCase() + rawName.substring(1);
            String labLev = "I";
            switch (level) {
                case 1 -> labLev = "I";
                case 2 -> labLev = "II";
                case 3 -> labLev = "III";
                case 4 -> labLev = "IV";
                case 5 -> labLev = "V";
                case 6 -> labLev = "VI";
                case 7 -> labLev = "VII";
                case 8 -> labLev = "VIII";
                case 9 -> labLev = "IX";
                case 10 -> labLev = "X";
            }

            newLore.add(tag + color + name + " " + labLev);
        }
        meta.setLore(newLore);
    }
    private String setRoman(int level) {
        String[] roman = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};
        return (level >= 1 && level <= 10) ? roman[level] : String.valueOf(level);
    }
}
