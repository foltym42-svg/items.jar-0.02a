package pl.n1mcffa.selectitemy;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.FireworkEffect;
import org.bukkit.entity.Firework;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.TileState;
import org.bukkit.block.data.BlockData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.meta.SkullMeta;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import com.destroystokyo.paper.event.player.PlayerLaunchProjectileEvent;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Particle;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Bee;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SelectItemy extends JavaPlugin implements Listener {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private NamespacedKey heartKey;     // na itemie: "normal" lub "infernal"
    private NamespacedKey infernalKey;  // na graczu: ile piekielnych serc zuzyl
    private NamespacedKey eventItemKey;  // na itemie eventowym: id (np. zeszyt)
    private final Map<UUID, Long> zeszytCooldowns = new HashMap<>();
    private final Map<UUID, ItemStack[]> costumeCache = new HashMap<>();
    private final Map<UUID, boolean[]> costumeShown = new HashMap<>();
    private final Map<UUID, Long> windFall = new HashMap<>(); // Wichrowa Kosa: do kiedy brak obrazen od upadku
    private final Map<UUID, Long> burns = new HashMap<>(); // Piglin: kto plonie specjalnym plomieniem -> do kiedy
    private int burnTick = 0;
    private final Map<Block, BlockData> trail = new HashMap<>(); // Soul Sand z kostiumu Lobuza: blok -> poprzedni stan
    private final Map<UUID, Mark> marks = new HashMap<>(); // Zakreslacz: kogo oznaczyl dany gracz
    private final Map<UUID, ItemDisplay> petDisplays = new HashMap<>(); // lewitujaca glowa za graczem
    private int petTick = 0;
    private final Map<String, Long> itemCooldowns = new HashMap<>(); // klucz: uuid:idItemu
    private final Map<UUID, Long> antiPearl = new HashMap<>();       // gracz -> do kiedy nie moze uzyc perly
    private NamespacedKey pocketIdKey;   // na kieszeni: unikalne id
    private NamespacedKey pocketDataKey; // na kieszeni: zapisane przedmioty
    private NamespacedKey petKey;        // na zwierzaku: id poziomu
    private NamespacedKey petDateKey;    // na zwierzaku: data zlapania
    private NamespacedKey petMobKey;     // na zwierzaku: typ moba
    private NamespacedKey petDataKey;    // na zwierzaku: efekty (id=wartosc;...)
    private NamespacedKey starKey;       // na mobie z gwiazdka
    private NamespacedKey cageChanceKey; // na klatce: szansa zlapania (%)
    private NamespacedKey costumeStoreKey; // na graczu: kostiumy w menu /kostium
    private NamespacedKey costumeKey;     // na czesci kostiumu: id kostiumu
    private NamespacedKey costumeHpKey;   // modyfikator zycia z kostiumu
    private NamespacedKey soulEffKey;     // modyfikator Soul Speed (brak spowolnienia)
    private NamespacedKey soulSpdKey;     // modyfikator Soul Speed (predkosc)
    private NamespacedKey heartsKey;    // na graczu: zapisane maksymalne zycie (lifesteal)
    private NamespacedKey statKills;   // na graczu: zabojstwa
    private NamespacedKey statDeaths;  // na graczu: smierci
    private NamespacedKey statRanking; // na graczu: ranking
    private NamespacedKey projKey; // na pocisku: id przedmiotu eventowego
    private NamespacedKey beeKey;  // na pszczole: uuid wlasciciela
    private NamespacedKey bookKey;        // na ksiedze: id enchantu
    private NamespacedKey bookTargetsKey; // na ksiedze: dozwolone przedmioty (po przecinku)
    private NamespacedKey bookLoreKey;    // na ksiedze: linia lore dodawana do zbroi

    private static final int MAX_NORMAL = 20;   // zwykle serca (razem z 10 startowymi)
    private static final int MAX_INFERNAL = 10; // piekielne serca
    private static final int MAX_TOTAL = MAX_NORMAL + MAX_INFERNAL; // 30 serc = 60 HP

    /** Holder rozpoznaje nasze menu. category == null -> glowne menu SelectGUI. */
    private static class Menu implements InventoryHolder {
        final String category;
        final Map<Integer, String> slots = new HashMap<>();
        Inventory inv;
        Menu(String category) { this.category = category; }
        @Override public @NotNull Inventory getInventory() { return inv; }
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        heartKey = new NamespacedKey(this, "heart");
        infernalKey = new NamespacedKey(this, "infernal_hearts");
        bookKey = new NamespacedKey(this, "book");
        eventItemKey = new NamespacedKey(this, "event_item");
        projKey = new NamespacedKey(this, "proj");
        pocketIdKey = new NamespacedKey(this, "pocket_id");
        pocketDataKey = new NamespacedKey(this, "pocket_data");
        petKey = new NamespacedKey(this, "pet");
        petDataKey = new NamespacedKey(this, "pet_data");
        petMobKey = new NamespacedKey(this, "pet_mob");
        petDateKey = new NamespacedKey(this, "pet_date");
        starKey = new NamespacedKey(this, "star");
        cageChanceKey = new NamespacedKey(this, "cage_chance");
        beeKey = new NamespacedKey(this, "bee");
        statKills = new NamespacedKey(this, "stat_kills");
        statDeaths = new NamespacedKey(this, "stat_deaths");
        statRanking = new NamespacedKey(this, "stat_ranking");
        heartsKey = new NamespacedKey(this, "max_hp");
        costumeKey = new NamespacedKey(this, "costume");
        costumeStoreKey = new NamespacedKey(this, "costume_store");
        costumeHpKey = new NamespacedKey(this, "costume_hp");
        soulEffKey = new NamespacedKey(this, "soul_eff");
        soulSpdKey = new NamespacedKey(this, "soul_spd");
        bookTargetsKey = new NamespacedKey(this, "book_targets");
        bookLoreKey = new NamespacedKey(this, "book_lore");
        Bukkit.getPluginManager().registerEvents(this, this);
        for (World w : Bukkit.getWorlds()) applyRespawnRule(w);
        Bukkit.getScheduler().runTaskTimer(this, this::tickEnchants, 40L, 20L);
        for (Player online : Bukkit.getOnlinePlayers()) createBoard(online);
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (Player online : Bukkit.getOnlinePlayers()) updateBoard(online);
        }, 20L, Math.max(5L, getConfig().getLong("scoreboard.update-ticks", 20)));
        Bukkit.getScheduler().runTaskTimer(this, this::updatePetDisplays, 20L, 2L);
        Bukkit.getScheduler().runTaskTimer(this, this::updateMarks, 10L, 5L);
        Bukkit.getScheduler().runTaskTimer(this, this::updateCostumes, 20L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, this::updateBurns, 20L, 10L);
        Bukkit.getScheduler().runTaskTimer(this, this::updateCostumeVisuals, 40L, 10L);
    }

    @Override
    public void onDisable() {
        for (Map.Entry<Block, BlockData> en : trail.entrySet()) en.getKey().setBlockData(en.getValue(), false);
        trail.clear();
        for (ItemDisplay d : petDisplays.values()) d.remove();
        petDisplays.clear();
        ScoreboardManager mgr = Bukkit.getScoreboardManager();
        if (mgr == null) return;
        for (Player online : Bukkit.getOnlinePlayers()) online.setScoreboard(mgr.getMainScoreboard());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (cmd.getName().equalsIgnoreCase("repair")) return handleRepair(sender, args);
        if (cmd.getName().equalsIgnoreCase("wyplacserce")) return handleWithdrawHearts(sender, args);
        if (cmd.getName().equalsIgnoreCase("zwierzak")) {
            if (sender instanceof Player zp) showPet(zp);
            else sender.sendMessage("Tylko dla graczy.");
            return true;
        }
        if (cmd.getName().equalsIgnoreCase("kostium")) {
            if (sender instanceof Player kp) openCostumeMenu(kp);
            else sender.sendMessage("Tylko dla graczy.");
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("selectitemy.reload")) return true;
            reloadConfig();
            for (Player online : Bukkit.getOnlinePlayers()) createBoard(online);
            sender.sendMessage(msg("messages.reloaded"));
            return true;
        }
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Tylko dla graczy.");
            return true;
        }
        if (!p.hasPermission("selectitemy.use")) return true;
        openMain(p);
        return true;
    }

    private int rows(int value) {
        return Math.max(1, Math.min(6, value));
    }

    private void openMain(Player p) {
        Menu menu = new Menu(null);
        int rows = rows(getConfig().getInt("menu-rows", 6));
        menu.inv = Bukkit.createInventory(menu, rows * 9, MM.deserialize(getConfig().getString("menu-title", "SelectGUI")));
        ConfigurationSection cats = getConfig().getConfigurationSection("categories");
        if (cats != null) {
            for (String key : cats.getKeys(false)) {
                ConfigurationSection s = cats.getConfigurationSection(key);
                if (s == null) continue;
                int slot = s.getInt("slot", -1);
                if (slot < 0 || slot >= rows * 9) {
                    getLogger().warning("Zly slot kategorii: " + key);
                    continue;
                }
                ItemStack item = buildItem(s, key);
                if (item == null) continue;
                menu.inv.setItem(slot, item);
                menu.slots.put(slot, key);
            }
        }
        p.openInventory(menu.inv);
    }

    private void openCategory(Player p, String catKey) {
        ConfigurationSection cat = getConfig().getConfigurationSection("categories." + catKey);
        if (cat == null) return;
        Menu menu = new Menu(catKey);
        int rows = rows(cat.getInt("rows", 6));
        int size = rows * 9;
        menu.inv = Bukkit.createInventory(menu, size, MM.deserialize(cat.getString("title", catKey)));

        int backSlot = getConfig().getInt("back-slot", 49);
        if (backSlot >= 0 && backSlot < size) {
            ItemStack back = new ItemStack(Material.ARROW);
            ItemMeta meta = back.getItemMeta();
            meta.displayName(MM.deserialize(getConfig().getString("back-name", "Wroc")).decoration(TextDecoration.ITALIC, false));
            back.setItemMeta(meta);
            menu.inv.setItem(backSlot, back);
            menu.slots.put(backSlot, "__back");
        }

        if (catKey.equals("kostiumy")) {
            int ts = cat.getInt("takeoff-slot", 48);
            if (ts >= 0 && ts < size && !menu.slots.containsKey(ts)) {
                ItemStack off = new ItemStack(Material.BARRIER);
                ItemMeta om = off.getItemMeta();
                om.displayName(MM.deserialize(cat.getString("takeoff-name", "<red>Zdejmij kostium")).decoration(TextDecoration.ITALIC, false));
                om.lore(List.of(MM.deserialize(cat.getString("takeoff-lore", "")).decoration(TextDecoration.ITALIC, false)));
                off.setItemMeta(om);
                menu.inv.setItem(ts, off);
                menu.slots.put(ts, "__takeoff");
            }
        }

        ConfigurationSection items = cat.getConfigurationSection("items");
        if (items != null) {
            int auto = 0;
            for (String key : items.getKeys(false)) {
                ConfigurationSection s = items.getConfigurationSection(key);
                if (s == null) continue;
                int slot = s.getInt("slot", -1);
                if (slot < 0) {
                    while (auto < size && menu.slots.containsKey(auto)) auto++;
                    slot = auto;
                }
                if (slot >= size || menu.slots.containsKey(slot)) {
                    getLogger().warning("Zly lub zajety slot itemu: " + catKey + "." + key);
                    continue;
                }
                ItemStack item = buildItem(s, key);
                if (item == null) continue;
                menu.inv.setItem(slot, item);
                menu.slots.put(slot, key);
            }
        }
        p.openInventory(menu.inv);
    }

    private ItemStack buildItem(ConfigurationSection s, String key) {
        Material mat = Material.matchMaterial(s.getString("material", "STONE"));
        if (mat == null || mat.isAir()) {
            getLogger().warning("Zly material dla: " + key);
            return null;
        }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(MM.deserialize(s.getString("name", key)).decoration(TextDecoration.ITALIC, false));
        String headTex = s.getString("texture", "");
        if (!headTex.isEmpty() && meta instanceof SkullMeta skull) {
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(headTex.getBytes()), "head");
            profile.setProperty(new ProfileProperty("textures", headTex));
            skull.setPlayerProfile(profile);
        }
        List<Component> lore = new ArrayList<>();
        for (String line : s.getStringList("lore")) {
            lore.add(MM.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        if (meta instanceof LeatherArmorMeta lm && s.contains("color")) {
            try {
                lm.setColor(Color.fromRGB(Integer.parseInt(s.getString("color", "000000"), 16)));
            } catch (NumberFormatException ex) {
                getLogger().warning("Zly kolor dla: " + key);
            }
        }
        if (s.contains("heart")) {
            meta.getPersistentDataContainer().set(heartKey, PersistentDataType.STRING, s.getString("heart", "normal"));
        }
        if (s.contains("book")) {
            var pdc = meta.getPersistentDataContainer();
            pdc.set(bookKey, PersistentDataType.STRING, s.getString("book", ""));
            pdc.set(bookTargetsKey, PersistentDataType.STRING, String.join(",", s.getStringList("apply-to")));
            pdc.set(bookLoreKey, PersistentDataType.STRING, s.getString("enchant-lore", s.getString("name", "")));
        }
        if (s.contains("event-item")) {
            meta.getPersistentDataContainer().set(eventItemKey, PersistentDataType.STRING, s.getString("event-item", ""));
        }
        if (s.contains("knockback-resistance")) {
            // +X Knockback Resistance na czesc (slot zalezy od materialu: helm, klata, spodnie, buty)
            String n = mat.name();
            EquipmentSlotGroup grp = n.endsWith("_HELMET") ? EquipmentSlotGroup.HEAD
                    : n.endsWith("_CHESTPLATE") ? EquipmentSlotGroup.CHEST
                    : n.endsWith("_LEGGINGS") ? EquipmentSlotGroup.LEGS
                    : n.endsWith("_BOOTS") ? EquipmentSlotGroup.FEET : null;
            if (grp != null) {
                meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(
                        new NamespacedKey(this, "kb_" + key), s.getDouble("knockback-resistance", 0.25),
                        AttributeModifier.Operation.ADD_NUMBER, grp));
            }
        }
        if (s.contains("costume")) {
            meta.getPersistentDataContainer().set(costumeKey, PersistentDataType.STRING, s.getString("costume", ""));
        }
        if (s.contains("cage-chance")) {
            meta.getPersistentDataContainer().set(cageChanceKey, PersistentDataType.INTEGER, s.getInt("cage-chance", 50));
        }
        ConfigurationSection en = s.getConfigurationSection("enchants");
        if (en != null) {
            for (String ek : en.getKeys(false)) {
                Enchantment ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft(ek.toLowerCase()));
                if (ench == null) {
                    getLogger().warning("Zly enchant '" + ek + "' dla: " + key);
                    continue;
                }
                meta.addEnchant(ench, en.getInt(ek, 1), true);
            }
        }
        meta.addItemFlags(ItemFlag.values());
        if (s.getBoolean("show-attributes", false)) meta.removeItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        if (s.getBoolean("show-enchants", false)) meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        if (s.getBoolean("show-enchants", false)) meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu menu)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        String key = menu.slots.get(e.getSlot());
        if (key == null) return;

        if (menu.category == null) {
            openCategory(p, key);
            return;
        }
        if (key.equals("__back")) {
            openMain(p);
            return;
        }
        if (key.equals("__takeoff")) {
            takeOffCostume(p);
            return;
        }

        if (!p.hasPermission("selectitemy.bypass")) {
            long left = cooldowns.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
            if (left > 0) {
                p.sendMessage(MM.deserialize(getConfig().getString("messages.cooldown", ""),
                        Placeholder.unparsed("time", formatTime(left / 1000))));
                return;
            }
        }

        ConfigurationSection s = getConfig().getConfigurationSection("categories." + menu.category + ".items." + key);
        if (s == null) return;
        ItemStack item = buildItem(s, key);
        if (item == null) return;
        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage(msg("messages.no-space"));
            return;
        }
        p.getInventory().addItem(item);
        cooldowns.put(p.getUniqueId(), System.currentTimeMillis() + getConfig().getLong("cooldown-seconds", 10800) * 1000L);
        p.sendMessage(MM.deserialize(getConfig().getString("messages.received", ""),
                Placeholder.component("item", item.getItemMeta().displayName())));
    }


    /** Prawy klik na sercu: +1 serce (2 HP) z limitami 20 zwyklych / 10 piekielnych / 30 lacznie. */
    @EventHandler
    public void onUseHeart(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String type = item.getItemMeta().getPersistentDataContainer().get(heartKey, PersistentDataType.STRING);
        if (type == null) return;
        e.setCancelled(true);

        Player p = e.getPlayer();
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        int total = (int) Math.round(attr.getBaseValue() / 2.0);
        int infernal = p.getPersistentDataContainer().getOrDefault(infernalKey, PersistentDataType.INTEGER, 0);
        int normal = total - infernal;

        boolean infernalHeart = type.equalsIgnoreCase("infernal");
        if (infernalHeart) {
            if (infernal >= MAX_INFERNAL || total >= MAX_TOTAL) {
                p.sendMessage(msg("messages.heart-max-infernal"));
                return;
            }
            p.getPersistentDataContainer().set(infernalKey, PersistentDataType.INTEGER, infernal + 1);
        } else if (normal >= MAX_NORMAL) {
            p.sendMessage(msg("messages.heart-max-normal"));
            return;
        }

        setMaxHealth(p, attr.getBaseValue() + 2.0);
        p.setHealth(Math.min(p.getHealth() + 2.0, attr.getValue()));
        item.setAmount(item.getAmount() - 1);
        p.sendMessage(msg("messages.heart-used"));
    }

    /** Kowadlo: przedmiot + ksiega -> przedmiot z enchantem (np. Lakomczuch na helm). */
    @EventHandler
    public void onAnvil(PrepareAnvilEvent e) {
        AnvilInventory inv = e.getInventory();
        ItemStack base = inv.getFirstItem();
        ItemStack book = inv.getSecondItem();
        if (base == null || book == null || !book.hasItemMeta()) return;
        var bpdc = book.getItemMeta().getPersistentDataContainer();
        String id = bpdc.get(bookKey, PersistentDataType.STRING);
        if (id == null) return;

        String targets = bpdc.getOrDefault(bookTargetsKey, PersistentDataType.STRING, "");
        if (!Arrays.asList(targets.split(",")).contains(base.getType().name())) {
            e.setResult(null);
            return;
        }
        NamespacedKey enchKey = new NamespacedKey(this, "ench_" + id);
        if (base.hasItemMeta() && base.getItemMeta().getPersistentDataContainer().has(enchKey, PersistentDataType.BYTE)) {
            e.setResult(null); // juz nalozony
            return;
        }

        ItemStack result = base.clone();
        ItemMeta rm = result.getItemMeta();
        rm.getPersistentDataContainer().set(enchKey, PersistentDataType.BYTE, (byte) 1);
        List<Component> lore = rm.lore() == null ? new ArrayList<>() : new ArrayList<>(rm.lore());
        lore.add(MM.deserialize(bpdc.getOrDefault(bookLoreKey, PersistentDataType.STRING, id))
                .decoration(TextDecoration.ITALIC, false));
        rm.lore(lore);
        result.setItemMeta(rm);
        e.setResult(result);
        e.getView().setRepairCost(getConfig().getInt("anvil-cost", 5));
    }

    /** Lakomczuch: szansa na odnowienie glodu, gdy gracz z enchantem na helmie uderza w walce. */
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        ItemStack helmet = p.getInventory().getHelmet();
        if (helmet == null || !helmet.hasItemMeta()) return;
        NamespacedKey k = new NamespacedKey(this, "ench_lakomczuch");
        if (!helmet.getItemMeta().getPersistentDataContainer().has(k, PersistentDataType.BYTE)) return;
        if (Math.random() < getConfig().getDouble("lakomczuch-chance", 0.20)) {
            p.setFoodLevel(20);
        }
    }

    /** Co sekunde: stale efekty z enchantow na zbroi (ognioodpornosc z napiersnika, szybkosc z butow). */
    private void tickEnchants() {
        NamespacedKey fire = new NamespacedKey(this, "ench_obsydianowa_ochrona");
        NamespacedKey speed = new NamespacedKey(this, "ench_szybkosc");
        for (Player p : Bukkit.getOnlinePlayers()) {
            keepEffect(p, p.getInventory().getChestplate(), fire, PotionEffectType.FIRE_RESISTANCE, 0);
            keepEffect(p, p.getInventory().getBoots(), speed, PotionEffectType.SPEED, 0);
        }
    }

    private void keepEffect(Player p, ItemStack piece, NamespacedKey key, PotionEffectType type, int amplifier) {
        if (piece == null || !piece.hasItemMeta()) return;
        if (!piece.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE)) return;
        PotionEffect cur = p.getPotionEffect(type);
        // nie nadpisuj mocniejszego lub dluzszego efektu (np. z mikstury)
        if (cur != null && (cur.getAmplifier() > amplifier
                || (cur.getAmplifier() == amplifier && cur.getDuration() > 80))) return;
        p.addPotionEffect(new PotionEffect(type, 80, amplifier, true, false, true));
    }

    /** Zeszyt: PPM naprawia jedna losowa, uszkodzona czesc zalozonej zbroi (cooldown z configu, item jednorazowy). */
    @EventHandler
    public void onUseEventItem(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        ItemStack item = e.getItem();
        if (item == null || !item.hasItemMeta()) return;
        String id = item.getItemMeta().getPersistentDataContainer().get(eventItemKey, PersistentDataType.STRING);
        if (id == null) return;
        if (id.equals("kupidyn") || id.equals("antyperla") || id.equals("pajecza_kusza")) return; // obsluga przy strzale / rzucie
        e.setCancelled(true);

        Player p = e.getPlayer();
        switch (id) {
            case "pszczeli_roj" -> { useBeeNest(p); return; }
            case "pradawna_laska" -> { useStaff(p); return; }
            case "kieszen" -> { openPocket(p, e.getItem()); return; }
            case "leki_dziadka" -> { useMedicine(p, e.getItem()); return; }
            case "jajko_zwierzaka" -> { useStarEgg(e); return; }
            case "mityczna_zmiana" -> { openReroll(p); return; }
            case "wichrowa_kosa" -> { useWindMace(p); return; }
            case "petarda" -> { usePetarda(e); return; }
            case "zeszyt" -> { }
            default -> { return; }
        }
        long left = zeszytCooldowns.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
        if (left > 0) {
            p.sendMessage(MM.deserialize(getConfig().getString("messages.zeszyt-cooldown", ""),
                    Placeholder.unparsed("time", formatTime((left + 999) / 1000))));
            return;
        }

        // zbierz uszkodzone czesci zalozonej zbroi
        ItemStack[] armor = {p.getInventory().getHelmet(), p.getInventory().getChestplate(),
                p.getInventory().getLeggings(), p.getInventory().getBoots()};
        String[] names = {"Hełm", "Napierśnik", "Spodnie", "Buty"};
        List<Integer> damaged = new ArrayList<>();
        for (int i = 0; i < armor.length; i++) {
            ItemStack a = armor[i];
            if (a != null && a.hasItemMeta() && a.getItemMeta() instanceof Damageable d && d.getDamage() > 0) {
                damaged.add(i);
            }
        }
        if (damaged.isEmpty()) {
            p.sendMessage(msg("messages.zeszyt-nothing"));
            return;
        }

        int idx = damaged.get(new Random().nextInt(damaged.size()));
        Damageable dm = (Damageable) armor[idx].getItemMeta();
        dm.setDamage(0);
        armor[idx].setItemMeta(dm);
        p.getInventory().setArmorContents(new ItemStack[]{armor[3], armor[2], armor[1], armor[0]});

        item.setAmount(item.getAmount() - 1);
        zeszytCooldowns.put(p.getUniqueId(),
                System.currentTimeMillis() + getConfig().getLong("zeszyt-cooldown-seconds", 90) * 1000L);
        p.showTitle(Title.title(msg("messages.zeszyt-title"),
                MM.deserialize(getConfig().getString("messages.zeszyt-subtitle", ""),
                        Placeholder.unparsed("piece", names[idx]))));
    }

    // ---------- wspolne cooldowny przedmiotow eventowych ----------

    private long cdLeft(Player p, String id) {
        return itemCooldowns.getOrDefault(p.getUniqueId() + ":" + id, 0L) - System.currentTimeMillis();
    }

    private void cdStart(Player p, String id, long seconds) {
        itemCooldowns.put(p.getUniqueId() + ":" + id, System.currentTimeMillis() + seconds * 1000L);
    }

    private boolean checkCd(Player p, String id) {
        long left = cdLeft(p, id);
        if (left > 0) {
            p.sendMessage(MM.deserialize(getConfig().getString("messages.item-cooldown", ""),
                    Placeholder.unparsed("time", formatTime((left + 999) / 1000))));
            return false;
        }
        return true;
    }

    private String eventItemId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(eventItemKey, PersistentDataType.STRING);
    }

    // ---------- Pszczeli Roj: fireball -> 8 pszczol ----------

    private void useBeeNest(Player p) {
        if (!checkCd(p, "pszczeli_roj")) return;
        Fireball fb = p.launchProjectile(Fireball.class, p.getEyeLocation().getDirection().multiply(1.2));
        fb.setIsIncendiary(false);
        fb.setYield(0f);
        fb.getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "pszczeli_roj");
        cdStart(p, "pszczeli_roj", getConfig().getLong("pszczeli-roj.cooldown-seconds", 120));
    }

    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        // fireball z Pszczelego Roju nie wybucha
        if (e.getEntity().getPersistentDataContainer().has(projKey, PersistentDataType.STRING)) e.setCancelled(true);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent e) {
        Projectile pr = e.getEntity();
        String tag = pr.getPersistentDataContainer().get(projKey, PersistentDataType.STRING);
        if (tag == null) return;
        Player owner = pr.getShooter() instanceof Player sp ? sp : null;
        switch (tag) {
            case "pszczeli_roj" -> spawnBees(pr, e, owner);
            case "pajecza_kusza" -> webTrap(e, owner);
            case "kupidyn" -> {
                if (owner != null && e.getHitEntity() instanceof LivingEntity victim && !victim.equals(owner)
                        && cdLeft(owner, "kupidyn") <= 0) {
                    cdStart(owner, "kupidyn", getConfig().getLong("kupidyn.cooldown-seconds", 90));
                    Bukkit.getScheduler().runTask(this, () -> pull(owner, victim));
                }
            }
            case "antyperla" -> {
                if (e.getHitEntity() instanceof Player victim && !victim.equals(owner)) {
                    long sec = getConfig().getLong("antyperla.block-seconds", 20);
                    antiPearl.put(victim.getUniqueId(), System.currentTimeMillis() + sec * 1000L);
                    victim.sendMessage(msg("messages.antiperla-hit-victim"));
                    if (owner != null) owner.sendMessage(msg("messages.antiperla-hit-owner"));
                }
            }
            default -> { }
        }
    }

    private void spawnBees(Projectile pr, ProjectileHitEvent e, Player owner) {
        Location loc;
        if (e.getHitEntity() != null) {
            loc = e.getHitEntity().getLocation().add(0, 1, 0);
        } else if (e.getHitBlock() != null && e.getHitBlockFace() != null) {
            loc = e.getHitBlock().getRelative(e.getHitBlockFace()).getLocation().add(0.5, 0.5, 0.5);
        } else {
            loc = pr.getLocation();
        }

        // cel: trafiony gracz albo najblizszy gracz w poblizu (nie wlasciciel)
        Player target = null;
        if (e.getHitEntity() instanceof Player hp && !hp.equals(owner)) {
            target = hp;
        } else {
            double best = Double.MAX_VALUE;
            for (Player near : loc.getNearbyPlayers(10)) {
                if (near.equals(owner)) continue;
                double d = near.getLocation().distanceSquared(loc);
                if (d < best) { best = d; target = near; }
            }
        }

        final Player finalTarget = target;
        String ownerId = owner != null ? owner.getUniqueId().toString() : "none";
        int count = getConfig().getInt("pszczeli-roj.bees", 8);
        Random rnd = new Random();
        for (int i = 0; i < count; i++) {
            Location at = loc.clone().add(rnd.nextDouble() - 0.5, rnd.nextDouble() * 0.5, rnd.nextDouble() - 0.5);
            Bee bee = loc.getWorld().spawn(at, Bee.class, b -> {
                b.getPersistentDataContainer().set(beeKey, PersistentDataType.STRING, ownerId);
                b.customName(MM.deserialize(getConfig().getString("pszczeli-roj.bee-name", "<yellow>[Pszczoła roju]")));
                b.setCustomNameVisible(true);
                b.setAnger(600);
                if (finalTarget != null) b.setTarget(finalTarget);
            });
            Bukkit.getScheduler().runTaskLater(this, () -> { if (bee.isValid()) bee.remove(); }, 300L);
        }
    }

    @EventHandler
    public void onBeeSting(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Bee bee)) return;
        String ownerId = bee.getPersistentDataContainer().get(beeKey, PersistentDataType.STRING);
        if (ownerId == null || !(e.getEntity() instanceof LivingEntity victim)) return;
        if (victim.getUniqueId().toString().equals(ownerId)) { e.setCancelled(true); return; }

        double pct = getConfig().getDouble("pszczeli-roj.health-percent", 0.10);
        e.setDamage(Math.max(1.0, victim.getHealth() * pct)); // 10% aktualnego zdrowia

        int poisonTicks = getConfig().getInt("pszczeli-roj.poison-seconds", 15) * 20;
        int poisonAmp = getConfig().getInt("pszczeli-roj.poison-level", 2) - 1;
        int nauseaTicks = getConfig().getInt("pszczeli-roj.nausea-seconds", 10) * 20;
        int nauseaAmp = getConfig().getInt("pszczeli-roj.nausea-level", 2) - 1;
        // nastepny tick, zeby nadpisac efekt zatrucia z waniliowego uzadlenia
        Bukkit.getScheduler().runTask(this, () -> {
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, poisonTicks, poisonAmp));
            victim.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaTicks, nauseaAmp));
        });
    }

    // ---------- Luk Kupidyna: strzala przyciaga przeciwnika ----------

    @EventHandler
    public void onShootBow(EntityShootBowEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        String bowId = eventItemId(e.getBow());
        if ("pajecza_kusza".equals(bowId)) {
            if (cdLeft(p, "pajecza_kusza") > 0) return; // zwykly strzal, gdy trwa cooldown
            e.getProjectile().getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "pajecza_kusza");
            return;
        }
        if (!"kupidyn".equals(bowId)) return;
        if (cdLeft(p, "kupidyn") > 0) return; // zwykly strzal, gdy trwa cooldown
        e.getProjectile().getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "kupidyn");
    }

    /** Przyciaga cel w strone gracza (wspolne dla Kupidyna i Laski). */
    private void pull(Player owner, LivingEntity target) {
        Vector v = owner.getLocation().toVector().subtract(target.getLocation().toVector());
        double dist = v.length();
        if (dist < 0.5) return;
        v.normalize().multiply(Math.min(3.0, 0.6 + dist * 0.12));
        v.setY(Math.max(v.getY(), 0.35));
        target.setVelocity(v);
    }

    // ---------- Antyperla: rzut sniezka + blokada ender perly ----------

    @EventHandler
    public void onLaunch(PlayerLaunchProjectileEvent e) {
        Player p = e.getPlayer();
        if (e.getProjectile() instanceof EnderPearl) {
            long left = antiPearl.getOrDefault(p.getUniqueId(), 0L) - System.currentTimeMillis();
            if (left > 0) {
                e.setCancelled(true);
                p.sendMessage(MM.deserialize(getConfig().getString("messages.antiperla-blocked", ""),
                        Placeholder.unparsed("time", formatTime((left + 999) / 1000))));
            }
            return;
        }
        if (!"antyperla".equals(eventItemId(e.getItemStack()))) return;
        if (!checkCd(p, "antyperla")) {
            e.setCancelled(true);
            return;
        }
        e.getProjectile().getPersistentDataContainer().set(projKey, PersistentDataType.STRING, "antyperla");
        cdStart(p, "antyperla", getConfig().getLong("antyperla.cooldown-seconds", 30));
    }

    // ---------- Pradawna Laska: przyciaga cel z celownika + Spowolnienie ----------

    private void useStaff(Player p) {
        if (!checkCd(p, "pradawna_laska")) return;
        double range = getConfig().getDouble("pradawna-laska.range", 15);
        RayTraceResult r = p.getWorld().rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), range,
                FluidCollisionMode.NEVER, true, 0.5, en -> en instanceof LivingEntity && !en.equals(p));
        if (r == null || !(r.getHitEntity() instanceof LivingEntity target)) {
            p.sendMessage(msg("messages.laska-no-target"));
            return;
        }
        pull(p, target);
        int ticks = getConfig().getInt("pradawna-laska.slowness-seconds", 6) * 20;
        int amp = getConfig().getInt("pradawna-laska.slowness-level", 4) - 1;
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, amp));
        cdStart(p, "pradawna_laska", getConfig().getLong("pradawna-laska.cooldown-seconds", 30));
    }

    // ---------- zasady smierci: natychmiastowy respawn i minimum serc ----------

    private void applyRespawnRule(World w) {
        if (getConfig().getBoolean("immediate-respawn", true)) {
            w.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        applyRespawnRule(e.getWorld());
    }

    /** Gracz na 1 sercu, ktory zginie, nie jest zerowany: dalej ma minimum serc z configu. */
    private void ensureMinHearts(Player p) {
        applySavedHealth(p);
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        double min = Math.max(1, getConfig().getInt("min-hearts", 1)) * 2.0;
        if (attr != null && attr.getBaseValue() < min) {
            setMaxHealth(p, min);
            p.setHealth(Math.min(min, attr.getValue()));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Bukkit.getScheduler().runTask(this, () -> ensureMinHearts(p));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(this, () -> ensureMinHearts(p));
    }

    // ---------- Kieszen Zlodzieja ----------

    private static final int POCKET_SLOTS = 45;
    private static final int POCKET_BUTTON = 49;

    private static class Pocket implements InventoryHolder {
        final String id;
        Inventory inv;
        Pocket(String id) { this.id = id; }
        @Override public @NotNull Inventory getInventory() { return inv; }
    }

    private boolean isHeart(ItemStack item) {
        return item != null && item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(heartKey, PersistentDataType.STRING);
    }

    private boolean hasHeart(Player p) {
        for (ItemStack it : p.getInventory().getContents()) if (isHeart(it)) return true;
        return false;
    }

    /** Slot kieszeni w ekwipunku gracza (po id, a przy id == null pierwszej znalezionej) albo -1. */
    private int findPocketSlot(Player p, String id) {
        ItemStack[] c = p.getInventory().getContents();
        for (int i = 0; i < c.length; i++) {
            ItemStack it = c[i];
            if (!"kieszen".equals(eventItemId(it))) continue;
            if (id == null) return i;
            String pid = it.getItemMeta().getPersistentDataContainer().get(pocketIdKey, PersistentDataType.STRING);
            if (id.equals(pid)) return i;
        }
        return -1;
    }

    private List<ItemStack> readPocket(ItemStack pocket) {
        List<ItemStack> list = new ArrayList<>();
        if (pocket == null || !pocket.hasItemMeta()) return list;
        byte[] data = pocket.getItemMeta().getPersistentDataContainer().get(pocketDataKey, PersistentDataType.BYTE_ARRAY);
        if (data == null) return list;
        for (ItemStack it : ItemStack.deserializeItemsFromBytes(data)) {
            if (it != null && !it.getType().isAir()) list.add(it);
        }
        return list;
    }

    private void writePocket(Player p, int slot, List<ItemStack> items) {
        ItemStack pocket = p.getInventory().getItem(slot);
        if (pocket == null) return;
        ItemMeta m = pocket.getItemMeta();
        m.getPersistentDataContainer().set(pocketDataKey, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(items));
        pocket.setItemMeta(m);
        p.getInventory().setItem(slot, pocket);
    }

    /** Dodaje caly stack do kieszeni (laczy ze stackami); false, gdy sie nie miesci. */
    private boolean addToPocket(List<ItemStack> pocket, ItemStack item) {
        int max = item.getMaxStackSize();
        int room = (POCKET_SLOTS - pocket.size()) * max;
        for (ItemStack s : pocket) if (s.isSimilar(item)) room += max - s.getAmount();
        if (room < item.getAmount()) return false;
        int left = item.getAmount();
        for (ItemStack s : pocket) {
            if (left <= 0) break;
            if (!s.isSimilar(item)) continue;
            int add = Math.min(max - s.getAmount(), left);
            s.setAmount(s.getAmount() + add);
            left -= add;
        }
        while (left > 0) {
            ItemStack n = item.clone();
            int put = Math.min(max, left);
            n.setAmount(put);
            pocket.add(n);
            left -= put;
        }
        return true;
    }

    private void openPocket(Player p, ItemStack hand) {
        if (hand == null) return;
        ItemMeta m = hand.getItemMeta();
        String id = m.getPersistentDataContainer().get(pocketIdKey, PersistentDataType.STRING);
        if (id == null) {
            id = UUID.randomUUID().toString();
            m.getPersistentDataContainer().set(pocketIdKey, PersistentDataType.STRING, id);
            hand.setItemMeta(m);
            p.getInventory().setItem(EquipmentSlot.HAND, hand);
        }
        Pocket pk = new Pocket(id);
        pk.inv = Bukkit.createInventory(pk, 54, MM.deserialize(getConfig().getString("kieszen.title", "Kieszeń Złodzieja")));
        refreshPocket(p, pk);
        p.openInventory(pk.inv);
    }

    private void refreshPocket(Player p, Pocket pk) {
        pk.inv.clear();
        int slot = findPocketSlot(p, pk.id);
        if (slot >= 0) {
            List<ItemStack> items = readPocket(p.getInventory().getItem(slot));
            for (int i = 0; i < items.size() && i < POCKET_SLOTS; i++) pk.inv.setItem(i, items.get(i));
        }
        ItemStack btn = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta m = btn.getItemMeta();
        m.displayName(MM.deserialize(getConfig().getString("kieszen.button-name", "<green>Wypłać przedmioty"))
                .decoration(TextDecoration.ITALIC, false));
        m.lore(List.of(MM.deserialize(getConfig().getString("kieszen.button-lore", ""))
                .decoration(TextDecoration.ITALIC, false)));
        btn.setItemMeta(m);
        pk.inv.setItem(POCKET_BUTTON, btn);
    }

    @EventHandler
    public void onPocketClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Pocket pk)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = findPocketSlot(p, pk.id);
        if (slot < 0) { p.closeInventory(); return; }

        List<ItemStack> items = readPocket(p.getInventory().getItem(slot));
        int clicked = e.getSlot();
        boolean overflow = false;
        if (clicked == POCKET_BUTTON) {
            for (Iterator<ItemStack> it = items.iterator(); it.hasNext(); ) {
                int res = giveOrDrop(p, it.next());
                if (res != 2) it.remove();
                if (res != 0) overflow = true;
            }
        } else if (clicked >= 0 && clicked < items.size() && clicked < POCKET_SLOTS) {
            int res = giveOrDrop(p, items.get(clicked));
            if (res != 2) items.remove(clicked);
            if (res != 0) overflow = true;
        } else {
            return;
        }
        writePocket(p, slot, items);
        refreshPocket(p, pk);
        if (overflow) {
            p.sendMessage(msg(getConfig().getBoolean("kieszen.drop-when-full", true)
                    ? "messages.pocket-dropped" : "messages.pocket-no-space"));
        }
    }

    /** Po zabiciu gracza jego przedmioty wpadaja do Kieszeni Zlodzieja zabojcy (serce do ekwipunku, jesli juz ma serce). */
    @EventHandler(priority = EventPriority.HIGH)
    public void onKillSteal(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;
        int slot = findPocketSlot(killer, null);
        if (slot < 0) return;

        List<ItemStack> pocket = readPocket(killer.getInventory().getItem(slot));
        boolean heartToInventory = hasHeart(killer);
        boolean any = false;
        for (Iterator<ItemStack> it = e.getDrops().iterator(); it.hasNext(); ) {
            ItemStack drop = it.next();
            if (drop == null || drop.getType().isAir()) continue;
            if (heartToInventory && isHeart(drop)) {
                Map<Integer, ItemStack> left = killer.getInventory().addItem(drop.clone());
                if (left.isEmpty()) { it.remove(); any = true; continue; }
                if (addToPocket(pocket, left.values().iterator().next())) { it.remove(); any = true; }
                continue;
            }
            if (addToPocket(pocket, drop.clone())) { it.remove(); any = true; }
        }
        if (any) {
            writePocket(killer, slot, pocket);
            killer.sendMessage(MM.deserialize(getConfig().getString("messages.pocket-loot", ""),
                    Placeholder.unparsed("player", victim.getName())));
        }
    }

    /** Zwykle klikniecie nie moze wkladac przedmiotow do bundla (zawartosc kieszeni trzymamy osobno). */
    @EventHandler
    public void onBundleClick(InventoryClickEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof Pocket || h instanceof Menu) return;
        ItemStack cur = e.getCurrentItem();
        ItemStack cursor = e.getCursor();
        boolean curPocket = "kieszen".equals(eventItemId(cur));
        boolean cursorPocket = "kieszen".equals(eventItemId(cursor));
        boolean curEmpty = cur == null || cur.getType().isAir();
        boolean cursorEmpty = cursor == null || cursor.getType().isAir();
        if ((curPocket && !cursorEmpty) || (cursorPocket && !curEmpty)) e.setCancelled(true);
    }

    // ---------- Kusza: pulapka 3x3 z pajeczyn ----------

    /** Trafienie tworzy 3x3 pajeczyn wokol trafionego miejsca (plaszczyzna prostopadla do trafionej sciany). */
    private void webTrap(ProjectileHitEvent e, Player owner) {
        if (owner != null && cdLeft(owner, "pajecza_kusza") > 0) return; // trwa cooldown
        Block center;
        BlockFace face = BlockFace.UP;
        if (e.getHitEntity() != null) {
            Entity target = e.getHitEntity();
            center = target.getLocation().getBlock(); // spodnie/buty: normalnie, na wysokosci stop
            double rel = e.getEntity().getLocation().getY() - target.getLocation().getY();
            if (rel >= getConfig().getDouble("pajecza-kusza.legs-height", 0.75)) {
                center = center.getRelative(BlockFace.UP); // tors/glowa: jeden blok wyzej
            }
        } else if (e.getHitBlock() != null && e.getHitBlockFace() != null) {
            face = e.getHitBlockFace();
            center = e.getHitBlock().getRelative(face);
        } else {
            return;
        }

        List<Block> placed = new ArrayList<>();
        for (int a = -1; a <= 1; a++) {
            for (int b = -1; b <= 1; b++) {
                Block bl;
                switch (face) {
                    case UP, DOWN -> bl = center.getRelative(a, 0, b);
                    case NORTH, SOUTH -> bl = center.getRelative(a, b, 0);
                    default -> bl = center.getRelative(0, b, a);
                }
                if (bl.getType().isAir()) {
                    bl.setType(Material.COBWEB);
                    placed.add(bl);
                }
            }
        }
        if (owner != null && !placed.isEmpty()) {
            cdStart(owner, "pajecza_kusza", getConfig().getLong("pajecza-kusza.cooldown-seconds", 120));
        }
        long ticks = getConfig().getLong("pajecza-kusza.duration-seconds", 10) * 20L;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            for (Block bl : placed) if (bl.getType() == Material.COBWEB) bl.setType(Material.AIR);
        }, ticks);
    }

    // ---------- Leki Dziadka: usuwa negatywne efekty i leczy do pelna ----------

    private void useMedicine(Player p, ItemStack item) {
        if (item == null || !checkCd(p, "leki_dziadka")) return;
        for (String name : getConfig().getStringList("leki-dziadka.negative-effects")) {
            PotionEffectType t = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(name.toLowerCase()));
            if (t != null) p.removePotionEffect(t);
        }
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr != null) p.setHealth(attr.getValue());
        item.setAmount(item.getAmount() - 1); // jednorazowy
        cdStart(p, "leki_dziadka", getConfig().getLong("leki-dziadka.cooldown-seconds", 60));
        p.sendMessage(msg("messages.leki-used"));
    }

    // ---------- Zwierzaki: moby z gwiazdka, klatki, itemy-zwierzaki z bonusami ----------

    @EventHandler
    public void onStarSpawn(CreatureSpawnEvent e) {
        CreatureSpawnEvent.SpawnReason reason = e.getSpawnReason();
        if (reason != CreatureSpawnEvent.SpawnReason.NATURAL && reason != CreatureSpawnEvent.SpawnReason.CHUNK_GEN) return;
        if (!getConfig().getStringList("pets.star-mobs").contains(e.getEntityType().name())) return;
        if (Math.random() >= getConfig().getDouble("pets.star-chance", 0.03)) return;
        markStar(e.getEntity());
    }

    private void markStar(LivingEntity le) {
        le.customName(MM.deserialize(getConfig().getString("pets.star-name", "<yellow>⭐")));
        le.setCustomNameVisible(true);
        le.getPersistentDataContainer().set(starKey, PersistentDataType.STRING, "1");
    }

    /** Jajko zwierzaka: PPM na blok przywoluje losowego moba z listy star-mobs, od razu z gwiazdka. */
    private void useStarEgg(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (e.getClickedBlock() == null) return;
        List<String> mobs = getConfig().getStringList("pets.star-mobs");
        if (mobs.isEmpty()) return;
        EntityType type;
        try {
            type = EntityType.valueOf(mobs.get(new Random().nextInt(mobs.size())).toUpperCase());
        } catch (IllegalArgumentException ex) {
            return;
        }
        Location loc = e.getClickedBlock().getRelative(e.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        Entity ent = loc.getWorld().spawnEntity(loc, type);
        if (ent instanceof LivingEntity le) markStar(le);
        ItemStack hand = e.getItem();
        if (hand != null && p.getGameMode() != GameMode.CREATIVE) hand.setAmount(hand.getAmount() - 1);
    }

    /** PPM klatka na moba z gwiazdka: losowanie, czy sie udalo (50% / 100%), potem item-zwierzak. */
    @EventHandler
    public void onCatch(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        ItemStack cage = p.getInventory().getItemInMainHand();
        if (!"klatka".equals(eventItemId(cage))) return;
        e.setCancelled(true);

        Entity target = e.getRightClicked();
        if (!target.getPersistentDataContainer().has(starKey, PersistentDataType.STRING)) {
            p.sendMessage(msg("messages.cage-not-star"));
            return;
        }
        int chance = cage.getItemMeta().getPersistentDataContainer().getOrDefault(cageChanceKey, PersistentDataType.INTEGER, 50);
        if (chance < 100) {
            // klatka zalezna od rangi: najwyzsza szansa ssposrod uprawnien gracza z pets.rank-chances
            for (Map<?, ?> rc : getConfig().getMapList("pets.rank-chances")) {
                Object perm = rc.get("permission");
                Object val = rc.get("chance");
                if (perm != null && val instanceof Number n && p.hasPermission(perm.toString())) {
                    chance = Math.max(chance, Math.min(100, n.intValue()));
                }
            }
        }
        boolean success = new Random().nextInt(100) < chance;
        if (success || getConfig().getBoolean("pets.consume-cage-on-fail", true)) {
            cage.setAmount(cage.getAmount() - 1);
        }
        if (!success) {
            p.sendMessage(msg("messages.cage-fail"));
            return;
        }
        EntityType type = target.getType();
        target.remove();
        ItemStack pet = generatePet(type);
        for (ItemStack left : p.getInventory().addItem(pet).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        p.sendMessage(MM.deserialize(getConfig().getString("messages.pet-caught", ""),
                Placeholder.component("pet", pet.getItemMeta().displayName())));
    }

    /** Losuje wartosc wg krzywej z configu (pets.value-curve): wysokie wartosci sa bardzo rzadkie. */
    private double sampleCurve(double min, double max, Random rnd) {
        double u = 1.0 - rnd.nextDouble(); // (0, 1]
        double prevF = 0.0, prevP = 1.0;
        for (Map<?, ?> pt : getConfig().getMapList("pets.value-curve")) {
            if (!(pt.get("f") instanceof Number fn) || !(pt.get("chance") instanceof Number cn)) continue;
            double f = fn.doubleValue();
            double pr = cn.doubleValue() / 100.0;
            if (pr <= 0 || pr >= prevP) continue;
            if (u >= pr) {
                double t = (Math.log(prevP) - Math.log(u)) / (Math.log(prevP) - Math.log(pr));
                return min + (prevF + t * (f - prevF)) * (max - min);
            }
            prevF = f;
            prevP = pr;
        }
        return max;
    }

    private double rollValue(ConfigurationSection t, List<Double> range, Random rnd) {
        if (range.size() < 2) return 1.0;
        double min = range.get(0), max = range.get(1);
        double v = t.getBoolean("uniform", false) ? min + (max - min) * rnd.nextDouble() : sampleCurve(min, max, rnd);
        return Math.floor(v); // procenty zawsze calkowite: 45, nie 45.1
    }

    private Map<String, Double> rollEffects(ConfigurationSection t, Random rnd) {
        int count = t.getInt("effects", 2);
        double skew = t.getDouble("skew", getConfig().getDouble("pets.skew", 3.0));
        Map<String, Double> eff = new LinkedHashMap<>();
        // najlepsze efekty: Obrazenia krytyczne + Redukcja obrazen (mityczny zawsze, nizsze poziomy z mala szansa)
        if (rnd.nextDouble() * 100 < t.getDouble("best-chance", 0)) {
            eff.put("avg-damage", rollValue(t, t.getDoubleList("best-range.avg-damage"), rnd));
            eff.put("damage-reduction", rollValue(t, t.getDoubleList("best-range.damage-reduction"), rnd));
        }
        List<String> pool = new ArrayList<>(List.of("avg-damage", "crit-chance", "fall-reduction", "dripstone-reduction", "poison-chance"));
        Collections.shuffle(pool, rnd);
        for (String id : pool) {
            if (eff.size() >= count) break;
            if (eff.containsKey(id)) continue; // juz wylosowany jako "najlepszy" efekt
            eff.put(id, rollValue(t, t.getDoubleList("range"), rnd));
        }
        return eff;
    }

    private ItemStack generatePet(EntityType mob) {
        ConfigurationSection tiers = getConfig().getConfigurationSection("pets.tiers");
        Random rnd = new Random();
        int total = 0;
        for (String k : tiers.getKeys(false)) total += tiers.getInt(k + ".weight");
        int r = rnd.nextInt(Math.max(1, total));
        String tier = tiers.getKeys(false).iterator().next();
        for (String k : tiers.getKeys(false)) {
            r -= tiers.getInt(k + ".weight");
            if (r < 0) { tier = k; break; }
        }
        ConfigurationSection t = tiers.getConfigurationSection(tier);
        return buildPetItem(mob, tier, t, rollEffects(t, rnd), null);
    }

    private boolean isPet(ItemStack it) {
        return it != null && it.hasItemMeta()
                && it.getItemMeta().getPersistentDataContainer().has(petDataKey, PersistentDataType.STRING);
    }

    /** Mityczna zmiana: losuje efekty zwierzaka od nowa (ten sam poziom, ta sama data zlapania). */
    private ItemStack rerollPet(ItemStack pet) {
        var pdc = pet.getItemMeta().getPersistentDataContainer();
        String tier = pdc.get(petKey, PersistentDataType.STRING);
        String mobName = pdc.get(petMobKey, PersistentDataType.STRING);
        String caught = pdc.get(petDateKey, PersistentDataType.STRING);
        ConfigurationSection t = getConfig().getConfigurationSection("pets.tiers." + tier);
        if (t == null || mobName == null) return pet;
        EntityType mob;
        try {
            mob = EntityType.valueOf(mobName);
        } catch (IllegalArgumentException ex) {
            return pet;
        }
        return buildPetItem(mob, tier, t, rollEffects(t, new Random()), caught);
    }

    private String fmt(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
    }

    private ItemStack buildPetItem(EntityType mob, String tier, ConfigurationSection t, Map<String, Double> eff, String caught) {
        String tex = getConfig().getString("pets.heads." + mob.name(), "");
        Material mat = Material.matchMaterial(getConfig().getString("pets.pet-material", ""));
        if (mat == null && !tex.isEmpty()) mat = Material.PLAYER_HEAD;
        if (mat == null && mob == EntityType.ZOMBIE) mat = Material.ZOMBIE_HEAD;
        if (mat == null && mob == EntityType.SKELETON) mat = Material.SKELETON_HEAD;
        if (mat == null && mob == EntityType.CREEPER) mat = Material.CREEPER_HEAD;
        if (mat == null) mat = Material.matchMaterial(mob.name() + "_SPAWN_EGG");
        if (mat == null) mat = Material.EGG;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta sm && !tex.isEmpty()) {
            PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(mob.name().getBytes()), "pet");
            profile.setProperty(new ProfileProperty("textures", tex));
            sm.setPlayerProfile(profile);
        }
        meta.displayName(MM.deserialize(getConfig().getString("pets.pet-name", "<green><bold>Klatka z zwierzakiem"))
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(MM.deserialize("<white>Poziom: " + t.getString("name", tier)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        StringBuilder data = new StringBuilder();
        for (Map.Entry<String, Double> en : eff.entrySet()) {
            String label = getConfig().getString("pets.effects." + en.getKey(), en.getKey());
            double v = en.getValue();
            // kolor zalezy od wartosci: niska/ujemna czerwona, srednia zlota, wysoka jasnoniebieska
            String color = v < getConfig().getDouble("pets.color-red-below", 10) ? "<red>"
                    : v < getConfig().getDouble("pets.color-gold-below", 25) ? "<gold>" : "<aqua>";
            lore.add(MM.deserialize(color + label + " " + (v >= 0 ? "+" : "") + fmt(v) + "%")
                    .decoration(TextDecoration.ITALIC, false));
            if (data.length() > 0) data.append(';');
            data.append(en.getKey()).append('=').append(en.getValue());
        }
        lore.add(Component.empty());
        String date = caught != null ? caught : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yy HH:mm"));
        lore.add(MM.deserialize("<white>Zlapany: <green>" + date).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.setMaxStackSize(1);
        meta.getPersistentDataContainer().set(petKey, PersistentDataType.STRING, tier);
        meta.getPersistentDataContainer().set(petMobKey, PersistentDataType.STRING, mob.name());
        meta.getPersistentDataContainer().set(petDataKey, PersistentDataType.STRING, data.toString());
        meta.getPersistentDataContainer().set(petDateKey, PersistentDataType.STRING, date);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** Bonusy z pierwszego zwierzaka znalezionego w ekwipunku (nie sumuja sie). */
    private Map<String, Double> petBonuses(Player p) {
        for (ItemStack it : p.getInventory().getContents()) {
            if (it == null || !it.hasItemMeta()) continue;
            String data = it.getItemMeta().getPersistentDataContainer().get(petDataKey, PersistentDataType.STRING);
            if (data == null) continue;
            Map<String, Double> m = new HashMap<>();
            for (String part : data.split(";")) {
                String[] kv = part.split("=");
                if (kv.length != 2) continue;
                try { m.merge(kv[0].equals("crit-damage") ? "avg-damage" : kv[0], Double.parseDouble(kv[1]), Double::sum); } catch (NumberFormatException ignored) { }
            }
            return m;
        }
        return Collections.emptyMap();
    }

    private double factor(double percent) {
        return 1.0 - Math.max(-100, Math.min(100, percent)) / 100.0; // ujemna redukcja = wiecej obrazen
    }

    /** Redukcje obrazen: ogolna, od upadku, od dripstone. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetDefense(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        Map<String, Double> b = petBonuses(p);
        if (b.isEmpty()) return;
        double m = factor(b.getOrDefault("damage-reduction", 0.0));
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL) {
            m *= factor(b.getOrDefault("fall-reduction", 0.0));
        }
        DamageType dt = e.getDamageSource().getDamageType();
        if (dt.equals(DamageType.STALAGMITE) || dt.equals(DamageType.FALLING_STALACTITE)) {
            m *= factor(b.getOrDefault("dripstone-reduction", 0.0));
        }
        e.setDamage(e.getDamage() * m);
    }

    /** Srednie obrazenia, szansa na krytyka i obrazenia krytyczne. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPetAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        Map<String, Double> b = petBonuses(p);
        if (b.isEmpty()) return;
        double m = 1.0 + b.getOrDefault("avg-damage", 0.0) / 100.0;
        boolean crit = p.getFallDistance() > 0 && !p.isOnGround() && !p.isSprinting()
                && !p.isClimbing() && !p.isInWater() && !p.isInsideVehicle();
        if (!crit && Math.random() * 100 < b.getOrDefault("crit-chance", 0.0)) {
            crit = true;
            m *= 1.5; // wymuszony krytyk dziala jak waniliowy
            e.getEntity().getWorld().spawnParticle(Particle.CRIT, e.getEntity().getLocation().add(0, 1, 0), 15);
        }
        e.setDamage(e.getDamage() * Math.max(0.0, m));

        // Szansa na zatrucie
        if (e.getEntity() instanceof LivingEntity victim
                && Math.random() * 100 < b.getOrDefault("poison-chance", 0.0)) {
            int ticks = getConfig().getInt("pets.poison-seconds", 5) * 20;
            victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, ticks, 0));
        }
    }

    /** Jajo zwierzaka nie moze przywolywac moba. */
    @EventHandler
    public void onPetItemUse(PlayerInteractEvent e) {
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        if (it.getItemMeta().getPersistentDataContainer().has(petDataKey, PersistentDataType.STRING)) e.setCancelled(true);
    }

    // ---------- Scoreboard (sidebar po prawej) ----------

    private static class Board {
        final Scoreboard sb;
        final List<Team> teams = new ArrayList<>();
        Board(Scoreboard sb) { this.sb = sb; }
    }

    private final Map<UUID, Board> boards = new HashMap<>();

    private int stat(Player p, NamespacedKey key) {
        return p.getPersistentDataContainer().getOrDefault(key, PersistentDataType.INTEGER, 0);
    }

    private void addStat(Player p, NamespacedKey key, int delta) {
        int v = stat(p, key) + delta;
        if (key.equals(statRanking)) v = Math.max(0, v);
        p.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, v);
    }

    /** Podstawia placeholdery PlaceholderAPI (jesli plugin jest zainstalowany), inaczej zwraca tekst bez zmian. */
    private String papi(Player p, String text) {
        if (text == null || !text.contains("%") || Bukkit.getPluginManager().getPlugin("PlaceholderAPI") == null) return text;
        try {
            Class<?> c = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            return (String) c.getMethod("setPlaceholders", org.bukkit.OfflinePlayer.class, String.class).invoke(null, p, text);
        } catch (Exception ex) {
            return text;
        }
    }

    private void createBoard(Player p) {
        if (!getConfig().getBoolean("scoreboard.enabled", true)) return;
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("selectitemy", Criteria.DUMMY,
                MM.deserialize(getConfig().getString("scoreboard.title", "SelectItemy")));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        List<String> lines = getConfig().getStringList("scoreboard.lines");
        int n = Math.min(15, lines.size());
        Board board = new Board(sb);
        for (int i = 0; i < n; i++) {
            Team t = sb.registerNewTeam("l" + i);
            String entry = "\u00a7" + Integer.toHexString(i); // unikalny, niewidoczny wpis
            t.addEntry(entry);
            obj.getScore(entry).setScore(n - i);
            board.teams.add(t);
        }
        boards.put(p.getUniqueId(), board);
        p.setScoreboard(sb);
        updateBoard(p);
    }

    private void updateBoard(Player p) {
        Board b = boards.get(p.getUniqueId());
        if (b == null) return;
        List<String> lines = getConfig().getStringList("scoreboard.lines");
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        int hearts = attr == null ? 10 : (int) Math.round(attr.getValue() / 2.0);
        TagResolver res = TagResolver.resolver(
                Placeholder.unparsed("nick", p.getName()),
                Placeholder.unparsed("kills", String.valueOf(stat(p, statKills))),
                Placeholder.unparsed("deaths", String.valueOf(stat(p, statDeaths))),
                Placeholder.unparsed("ranking", String.valueOf(ranking(p))),
                Placeholder.unparsed("hearts", String.valueOf(hearts)),
                Placeholder.unparsed("sector", papi(p, getConfig().getString("scoreboard.sources.sector", "-"))),
                Placeholder.unparsed("clan", papi(p, getConfig().getString("scoreboard.sources.clan", "-"))),
                Placeholder.unparsed("money", papi(p, getConfig().getString("scoreboard.sources.money", "0"))));
        for (int i = 0; i < b.teams.size() && i < lines.size(); i++) {
            b.teams.get(i).prefix(MM.deserialize(lines.get(i), res));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        applySavedHealth(e.getPlayer());
        createBoard(e.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        boards.remove(e.getPlayer().getUniqueId());
        marks.remove(e.getPlayer().getUniqueId());
        costumeCache.remove(e.getPlayer().getUniqueId());
        costumeShown.remove(e.getPlayer().getUniqueId());
        ItemDisplay pd = petDisplays.remove(e.getPlayer().getUniqueId());
        if (pd != null) pd.remove();
    }

    private int ranking(Player p) {
        return p.getPersistentDataContainer().getOrDefault(statRanking, PersistentDataType.INTEGER,
                getConfig().getInt("scoreboard.ranking-start", 500));
    }

    private void setRanking(Player p, int v) {
        p.getPersistentDataContainer().set(statRanking, PersistentDataType.INTEGER, Math.max(0, v));
    }

    /** Statystyki: zabojstwa, smierci i ranking. Im wyzszy ranking, tym mniej dostajesz za zabojstwo i tym wiecej tracisz za smierc. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onStats(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        addStat(victim, statDeaths, 1);
        int vr = ranking(victim);
        double pct = getConfig().getDouble("scoreboard.ranking-death-percent", 5.0);
        int minLoss = getConfig().getInt("scoreboard.ranking-death-min", 5);
        int loss = Math.max(minLoss, (int) Math.round(vr * pct / 100.0));
        setRanking(victim, vr - loss);

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            addStat(killer, statKills, 1);
            int kr = ranking(killer);
            double base = getConfig().getDouble("scoreboard.ranking-kill-base", 50);
            double scale = getConfig().getDouble("scoreboard.ranking-kill-scale", 1000);
            int gain = Math.max(1, (int) Math.round(base * scale / (scale + kr)));
            setRanking(killer, kr + gain);
        }
    }

    // ---------- Kieszen: brak miejsca = przedmiot na ziemie ----------

    /** 0 = trafil do ekwipunku, 1 = wyrzucony na ziemie, 2 = zostaje w kieszeni (gdy drop-when-full: false). */
    private int giveOrDrop(Player p, ItemStack st) {
        Map<Integer, ItemStack> left = p.getInventory().addItem(st.clone());
        if (left.isEmpty()) return 0;
        ItemStack rest = left.values().iterator().next();
        if (getConfig().getBoolean("kieszen.drop-when-full", true)) {
            p.getWorld().dropItemNaturally(p.getLocation(), rest);
            return 1;
        }
        st.setAmount(rest.getAmount());
        return 2;
    }

    // ---------- /repair i /repair all ----------

    private boolean repairItem(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return false;
        if (!(it.getItemMeta() instanceof Damageable d) || d.getDamage() <= 0) return false;
        d.setDamage(0);
        it.setItemMeta(d);
        return true;
    }

    private boolean handleRepair(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Tylko dla graczy.");
            return true;
        }
        boolean all = args.length > 0 && args[0].equalsIgnoreCase("all");
        if (!p.hasPermission(all ? "selectitemy.repair.all" : "selectitemy.repair")) {
            p.sendMessage(msg("messages.no-permission"));
            return true;
        }
        int count = 0;
        if (all) {
            ItemStack[] c = p.getInventory().getContents(); // ekwipunek, zbroja i druga reka
            for (int i = 0; i < c.length; i++) {
                if (repairItem(c[i])) {
                    p.getInventory().setItem(i, c[i]);
                    count++;
                }
            }
        } else {
            ItemStack hand = p.getInventory().getItemInMainHand();
            if (repairItem(hand)) {
                p.getInventory().setItemInMainHand(hand);
                count++;
            }
        }
        if (count == 0) {
            p.sendMessage(msg("messages.repair-nothing"));
        } else if (all) {
            p.sendMessage(MM.deserialize(getConfig().getString("messages.repair-all", ""),
                    Placeholder.unparsed("count", String.valueOf(count))));
        } else {
            p.sendMessage(msg("messages.repair-hand"));
        }
        return true;
    }

    // ---------- Mityczna zmiana: GUI zmiany efektow zwierzaka ----------

    private static final int REROLL_PET_SLOT = 11;
    private static final int REROLL_BUTTON = 15;

    private static class Reroll implements InventoryHolder {
        Inventory inv;
        @Override public @NotNull Inventory getInventory() { return inv; }
    }

    private void openReroll(Player p) {
        Reroll rr = new Reroll();
        rr.inv = Bukkit.createInventory(rr, 27, MM.deserialize(getConfig().getString("mityczna-zmiana.title", "Mityczna zmiana")));
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) if (i != REROLL_PET_SLOT) rr.inv.setItem(i, filler);

        ItemStack btn = new ItemStack(Material.LIME_CONCRETE);
        ItemMeta bm = btn.getItemMeta();
        bm.displayName(MM.deserialize(getConfig().getString("mityczna-zmiana.button-name", "<green>Zmień efekty"))
                .decoration(TextDecoration.ITALIC, false));
        bm.lore(List.of(MM.deserialize(getConfig().getString("mityczna-zmiana.button-lore", ""))
                .decoration(TextDecoration.ITALIC, false)));
        btn.setItemMeta(bm);
        rr.inv.setItem(REROLL_BUTTON, btn);
        p.openInventory(rr.inv);
    }

    @EventHandler
    public void onRerollClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Reroll rr)) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ClickType ct = e.getClick();
        if (e.isShiftClick() || ct == ClickType.NUMBER_KEY || ct == ClickType.DOUBLE_CLICK || ct == ClickType.SWAP_OFFHAND) {
            e.setCancelled(true);
            return;
        }
        if (e.getClickedInventory() != e.getInventory()) return; // klikniecia w swoim ekwipunku sa dozwolone
        int slot = e.getSlot();
        if (slot == REROLL_PET_SLOT) {
            ItemStack cursor = e.getCursor();
            if (cursor != null && !cursor.getType().isAir() && !isPet(cursor)) e.setCancelled(true); // tylko zwierzaki
            return;
        }
        e.setCancelled(true);
        if (slot == REROLL_BUTTON) doReroll(p, rr);
    }

    private void doReroll(Player p, Reroll rr) {
        ItemStack pet = rr.inv.getItem(REROLL_PET_SLOT);
        if (!isPet(pet)) {
            p.sendMessage(msg("messages.reroll-no-pet"));
            return;
        }
        ItemStack[] c = p.getInventory().getContents();
        int paperSlot = -1;
        for (int i = 0; i < c.length; i++) {
            if ("mityczna_zmiana".equals(eventItemId(c[i]))) { paperSlot = i; break; }
        }
        if (paperSlot < 0) {
            p.sendMessage(msg("messages.reroll-no-paper"));
            return;
        }
        ItemStack paper = c[paperSlot];
        paper.setAmount(paper.getAmount() - 1);
        p.getInventory().setItem(paperSlot, paper.getAmount() <= 0 ? null : paper);
        rr.inv.setItem(REROLL_PET_SLOT, rerollPet(pet));
        p.sendMessage(msg("messages.reroll-done"));
    }

    @EventHandler
    public void onRerollClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof Reroll)) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        ItemStack pet = e.getInventory().getItem(REROLL_PET_SLOT);
        if (pet == null || pet.getType().isAir()) return;
        e.getInventory().setItem(REROLL_PET_SLOT, null);
        for (ItemStack left : p.getInventory().addItem(pet).values()) {
            p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
    }

    // ---------- Zwierzak jako lewitujaca glowa, ktora podaza za graczem ----------

    private ItemStack firstPet(Player p) {
        for (ItemStack it : p.getInventory().getContents()) if (isPet(it)) return it;
        return null;
    }

    private ItemDisplay spawnPetDisplay(Player p, ItemStack pet) {
        ItemStack shown = pet.clone();
        String mob = pet.getItemMeta().getPersistentDataContainer().get(petMobKey, PersistentDataType.STRING);
        String mobId = mob == null ? "" : mob;
        return p.getWorld().spawn(p.getLocation().add(0, 2, 0), ItemDisplay.class, d -> {
            d.setItemStack(shown);
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            d.setBillboard(Display.Billboard.VERTICAL);
            d.setTeleportDuration(3);
            d.setPersistent(false);
            d.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(0.8f, 0.8f, 0.8f), new AxisAngle4f()));
            d.getPersistentDataContainer().set(petMobKey, PersistentDataType.STRING, mobId);
        });
    }

    /** Co 2 ticki: glowa pierwszego zwierzaka z ekwipunku lata po prawej, lekko z tylu i buja sie w gore i w dol. */
    private void updatePetDisplays() {
        petTick += 2;
        for (Player p : Bukkit.getOnlinePlayers()) {
            ItemStack pet = p.isDead() ? null : firstPet(p);
            ItemDisplay d = petDisplays.get(p.getUniqueId());
            if (pet == null) {
                if (d != null) {
                    d.remove();
                    petDisplays.remove(p.getUniqueId());
                }
                continue;
            }
            String mob = pet.getItemMeta().getPersistentDataContainer().get(petMobKey, PersistentDataType.STRING);
            String shown = d == null ? null : d.getPersistentDataContainer().get(petMobKey, PersistentDataType.STRING);
            if (d == null || !d.isValid() || !Objects.equals(mob == null ? "" : mob, shown)) {
                if (d != null) d.remove();
                d = spawnPetDisplay(p, pet);
                petDisplays.put(p.getUniqueId(), d);
            }
            Location base = p.getLocation();
            Vector dir = base.getDirection().setY(0);
            if (dir.lengthSquared() < 1.0E-4) dir = new Vector(0, 0, 1);
            dir.normalize();
            Vector right = new Vector(-dir.getZ(), 0, dir.getX());
            double bob = Math.sin(petTick / 10.0) * 0.12;
            Location target = base.clone().add(right.multiply(0.9)).add(dir.multiply(-0.5)).add(0, 1.9 + bob, 0);
            d.teleport(target);
        }
    }

    private void showPet(Player p) {
        Map<String, Double> b = petBonuses(p);
        if (b.isEmpty()) {
            p.sendMessage(msg("messages.pet-none"));
            return;
        }
        for (Map.Entry<String, Double> en : b.entrySet()) {
            String label = getConfig().getString("pets.effects." + en.getKey(), en.getKey());
            p.sendMessage(MM.deserialize("<gray>» <white>" + label + ": <green>" + (en.getValue() >= 0 ? "+" : "") + fmt(en.getValue()) + "%"));
        }
    }

    // ---------- Lifesteal: kazda smierc zabiera serce ----------

    /** Ustawia maksymalne zycie gracza i zapisuje je (zeby przetrwalo respawn i restart). */
    private void setMaxHealth(Player p, double hp) {
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        attr.setBaseValue(hp);
        p.getPersistentDataContainer().set(heartsKey, PersistentDataType.DOUBLE, hp);
    }

    private void applySavedHealth(Player p) {
        Double hp = p.getPersistentDataContainer().get(heartsKey, PersistentDataType.DOUBLE);
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (hp == null || attr == null) return;
        if (attr.getBaseValue() != hp) attr.setBaseValue(hp);
    }

    /** Po kazdej smierci (takze z upadku, lawy itd.) gracz traci serce, ale nigdy nie schodzi ponizej minimum. Po zabiciu przez gracza wypada Serce. */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onLifesteal(PlayerDeathEvent e) {
        if (!getConfig().getBoolean("lifesteal.enabled", true)) return;
        Player victim = e.getEntity();
        AttributeInstance attr = victim.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return;
        double minHp = Math.max(1, getConfig().getInt("min-hearts", 1)) * 2.0;
        double loss = getConfig().getDouble("lifesteal.heart-hp", 2.0);
        double base = attr.getBaseValue();
        if (base - loss < minHp) return; // juz na minimum: serce nie spada nizej

        int total = (int) Math.round(base / 2.0);
        int infernal = victim.getPersistentDataContainer().getOrDefault(infernalKey, PersistentDataType.INTEGER, 0);
        int normal = total - infernal;
        // najpierw zwykle serca; piekielne trac sie dopiero, gdy zwyklych nie zostalo
        String lostItem = (normal > 1 || infernal == 0) ? "serce" : "piekielne_serce";
        if (lostItem.equals("piekielne_serce")) {
            victim.getPersistentDataContainer().set(infernalKey, PersistentDataType.INTEGER, infernal - 1);
        }
        setMaxHealth(victim, base - loss);

        int hearts = (int) Math.round((base - loss) / 2.0);
        victim.sendMessage(MM.deserialize(getConfig().getString("messages.lifesteal-lost", ""),
                Placeholder.unparsed("hearts", String.valueOf(hearts))));

        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim) && getConfig().getBoolean("lifesteal.drop-heart-on-pvp", true)) {
            ConfigurationSection hs = getConfig().getConfigurationSection("categories.serca.items." + lostItem);
            if (hs != null) {
                ItemStack heart = buildItem(hs, lostItem);
                if (heart != null) e.getDrops().add(heart);
            }
        }
    }

    // ---------- Zakreslacz: odleglosc na Action Barze, Glowing, PPM usuwa pozytywne efekty ----------

    private static class Mark {
        final UUID target;
        final long until;
        Mark(UUID target, long until) { this.target = target; this.until = until; }
    }

    /** Uderzenie przeciwnika Zakreslaczem: Glowing (domyslnie bialy) i odleglosc na Action Barze przez mark-seconds. */
    @EventHandler(ignoreCancelled = true)
    public void onMark(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity target)) return;
        if (!"zakreslacz".equals(eventItemId(p.getInventory().getItemInMainHand()))) return;
        long markMs = getConfig().getLong("zakreslacz.mark-seconds", 15) * 1000L;
        marks.put(p.getUniqueId(), new Mark(target.getUniqueId(), System.currentTimeMillis() + markMs));
        int glowTicks = getConfig().getInt("zakreslacz.glowing-seconds", 5) * 20;
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, glowTicks, 0));
    }

    private void updateMarks() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Mark>> it = marks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Mark> en = it.next();
            Player p = Bukkit.getPlayer(en.getKey());
            Entity target = Bukkit.getEntity(en.getValue().target);
            if (p == null || target == null || !target.isValid() || now > en.getValue().until
                    || !p.getWorld().equals(target.getWorld())) {
                it.remove();
                continue;
            }
            double dist = p.getLocation().distance(target.getLocation());
            String name = target instanceof Player tp ? tp.getName() : target.getType().name().toLowerCase().replace('_', ' ');
            p.sendActionBar(MM.deserialize(getConfig().getString("zakreslacz.actionbar", "<yellow><distance> kratek"),
                    Placeholder.unparsed("name", name),
                    Placeholder.unparsed("distance", String.valueOf(Math.round(dist)))));
        }
    }

    /** PPM Zakreslaczem na przeciwniku usuwa mu wszystkie pozytywne efekty (cooldown z configu). */
    @EventHandler
    public void onZakreslaczClick(PlayerInteractEntityEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!"zakreslacz".equals(eventItemId(p.getInventory().getItemInMainHand()))) return;
        e.setCancelled(true);
        if (!(e.getRightClicked() instanceof LivingEntity target)) return;
        if (!checkCd(p, "zakreslacz")) return;
        int removed = 0;
        for (String name : getConfig().getStringList("zakreslacz.positive-effects")) {
            PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(name.toLowerCase()));
            if (type != null && target.hasPotionEffect(type)) {
                target.removePotionEffect(type);
                removed++;
            }
        }
        if (removed == 0) {
            p.sendMessage(msg("messages.zakreslacz-none"));
            return;
        }
        cdStart(p, "zakreslacz", getConfig().getLong("zakreslacz.cooldown-seconds", 180));
        p.sendMessage(MM.deserialize(getConfig().getString("messages.zakreslacz-removed", ""),
                Placeholder.unparsed("count", String.valueOf(removed))));
    }

    // ---------- Kostium Lobuza ----------

    private boolean isCostume(ItemStack it, String id) {
        return it != null && it.hasItemMeta()
                && id.equals(it.getItemMeta().getPersistentDataContainer().get(costumeKey, PersistentDataType.STRING));
    }

    /** Liczba czesci kostiumu: zalozonych jako zbroja plus tych w menu /kostium. */
    private int costumePieces(Player p, String id) {
        int n = 0;
        for (ItemStack it : p.getInventory().getArmorContents()) if (isCostume(it, id)) n++;
        for (ItemStack it : costumeItems(p)) if (isCostume(it, id)) n++;
        return n;
    }

    /** Dodaje, zmienia lub usuwa nasz modyfikator atrybutu (amount == 0 usuwa). */
    private void setModifier(Player p, Attribute attrType, NamespacedKey key, double amount, AttributeModifier.Operation op) {
        AttributeInstance inst = p.getAttribute(attrType);
        if (inst == null) return;
        AttributeModifier existing = null;
        for (AttributeModifier m : inst.getModifiers()) {
            if (key.equals(m.getKey())) { existing = m; break; }
        }
        if (amount == 0) {
            if (existing != null) inst.removeModifier(existing);
            return;
        }
        if (existing != null) {
            if (existing.getAmount() == amount) return;
            inst.removeModifier(existing);
        }
        inst.addModifier(new AttributeModifier(key, amount, op, EquipmentSlotGroup.ANY));
    }

    /** Co pol sekundy: +zycie za kazda czesc oraz Soul Speed I (bez spowolnienia i z bonusem) przy pelnym komplecie na Soul Sand. */
    private void updateCostumes() {
        double hpPer = getConfig().getDouble("lobuz.hp-per-piece", 1.0); // 1 HP = 0,5 serca
        double eff = getConfig().getDouble("lobuz.soul-efficiency", 1.0);
        double bonus = getConfig().getDouble("lobuz.soul-speed-bonus", 0.3);
        for (Player p : Bukkit.getOnlinePlayers()) {
            int n = costumePieces(p, "lobuz");
            double hp = n * hpPer
                    + costumePieces(p, "wampir") * getConfig().getDouble("wampir.hp-per-piece", 1.0)
                    + costumePieces(p, "piglin") * getConfig().getDouble("piglin.hp-per-piece", 1.0);
            setModifier(p, Attribute.MAX_HEALTH, costumeHpKey, hp, AttributeModifier.Operation.ADD_NUMBER);
            boolean onSoul = false;
            if (n >= 4) {
                Material under = p.getLocation().subtract(0, 0.2, 0).getBlock().getType();
                onSoul = under == Material.SOUL_SAND || under == Material.SOUL_SOIL;
            }
            setModifier(p, Attribute.MOVEMENT_EFFICIENCY, soulEffKey, onSoul ? eff : 0, AttributeModifier.Operation.ADD_NUMBER);
            setModifier(p, Attribute.MOVEMENT_SPEED, soulSpdKey, onSoul ? bonus : 0, AttributeModifier.Operation.ADD_SCALAR);
        }
    }

    /** +2,5% redukcji obrazen za kazda czesc kostiumu Lobuza. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCostumeDefense(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        int n = costumePieces(p, "lobuz");
        if (n == 0) return;
        e.setDamage(e.getDamage() * factor(n * getConfig().getDouble("lobuz.damage-reduction-per-piece", 2.5)));
    }

    /** Pelny komplet: pod poruszajacym sie graczem blok zamienia sie w Soul Sand i po kilku sekundach wraca. */
    @EventHandler(ignoreCancelled = true)
    public void onCostumeMove(PlayerMoveEvent e) {
        if (e.getTo() == null || e.getFrom().getBlock().equals(e.getTo().getBlock())) return;
        Player p = e.getPlayer();
        if (costumePieces(p, "lobuz") < 4) return;
        Block below = e.getTo().getBlock().getRelative(BlockFace.DOWN);
        Material type = below.getType();
        if (trail.containsKey(below) || !type.isOccluding() || type.getHardness() < 0
                || type == Material.SOUL_SAND || type == Material.SOUL_SOIL
                || below.getState() instanceof TileState) return;
        trail.put(below, below.getBlockData());
        below.setType(Material.SOUL_SAND, false);
        long ticks = getConfig().getLong("lobuz.trail-seconds", 3) * 20L;
        Bukkit.getScheduler().runTaskLater(this, () -> {
            BlockData old = trail.remove(below);
            if (old != null && below.getType() == Material.SOUL_SAND) below.setBlockData(old, false);
        }, ticks);
    }

    /** Tymczasowego Soul Sandu nie mozna wykopac (inaczej dawalby darmowy drop). */
    @EventHandler(ignoreCancelled = true)
    public void onTrailBreak(BlockBreakEvent e) {
        if (trail.containsKey(e.getBlock())) e.setCancelled(true);
    }

    /** Zdejmuje zalozone czesci kostiumu do ekwipunku (gdy brak miejsca, wypadaja na ziemie). */
    private void takeOffCostume(Player p) {
        var inv = p.getInventory();
        ItemStack[] armor = inv.getArmorContents();
        List<ItemStack> removed = new ArrayList<>();
        for (int i = 0; i < armor.length; i++) {
            ItemStack it = armor[i];
            if (it != null && it.hasItemMeta()
                    && it.getItemMeta().getPersistentDataContainer().has(costumeKey, PersistentDataType.STRING)) {
                removed.add(it);
                armor[i] = null;
            }
        }
        if (removed.isEmpty()) {
            p.sendMessage(msg("messages.costume-none"));
            return;
        }
        inv.setArmorContents(armor);
        for (ItemStack it : removed) {
            for (ItemStack left : inv.addItem(it).values()) p.getWorld().dropItemNaturally(p.getLocation(), left);
        }
        p.sendMessage(MM.deserialize(getConfig().getString("messages.costume-taken-off", ""),
                Placeholder.unparsed("count", String.valueOf(removed.size()))));
    }

    // ---------- Kostiumy Wampira i Piglina ----------

    /** Wampir: +1 obrazen za kazda czesc. Piglin: +2% srednich obrazen za kazda czesc i (pelny komplet) specjalny plomien. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCostumeAttack(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        int w = costumePieces(p, "wampir");
        int g = costumePieces(p, "piglin");
        if (w == 0 && g == 0) return;
        double dmg = e.getDamage() + w * getConfig().getDouble("wampir.damage-per-piece", 1.0);
        dmg *= 1.0 + g * getConfig().getDouble("piglin.avg-damage-per-piece", 2.0) / 100.0;
        e.setDamage(dmg);
        if (g >= 4 && e.getEntity() instanceof LivingEntity target) {
            long ms = getConfig().getLong("piglin.flame-seconds", 5) * 1000L;
            burns.put(target.getUniqueId(), System.currentTimeMillis() + ms);
            target.setFireTicks(Math.max(target.getFireTicks(), (int) (ms / 50)));
        }
    }

    /** Plomien Piglina: nie gasnie w wodzie i rani nawet przy ognioodpornosci (Obsydianowa Ochrona). */
    private void updateBurns() {
        burnTick++;
        long now = System.currentTimeMillis();
        double dmg = getConfig().getDouble("piglin.flame-damage", 1.0);
        for (Iterator<Map.Entry<UUID, Long>> it = burns.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Long> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            if (!(ent instanceof LivingEntity t) || !t.isValid() || t.isDead() || now > en.getValue()) {
                it.remove();
                continue;
            }
            t.setFireTicks(Math.max(t.getFireTicks(), 30)); // woda nie gasi plomienia
            if (burnTick % 2 == 0 && (t.hasPotionEffect(PotionEffectType.FIRE_RESISTANCE) || t.isInWater())) {
                t.damage(dmg); // zwykly ogien i tak rani, gdy nic go nie blokuje
            }
        }
    }

    /** Wampir: po pekniecie totemu (pelny komplet) dostajesz Sile 2. */
    @EventHandler(ignoreCancelled = true)
    public void onTotemPop(EntityResurrectEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        if (costumePieces(p, "wampir") < 4) return;
        int ticks = getConfig().getInt("wampir.strength-seconds", 30) * 20;
        Bukkit.getScheduler().runTask(this, () -> p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, 1)));
    }

    // ---------- Menu kostiumu (/kostium): kostium jako wyglad, wlasciwosci prawdziwego seta zostaja ----------

    private static final int COSTUME_FIRST = 10; // sloty 10-13: helm, zbroja, spodnie, buty
    private static final int COSTUME_SIGN = 15;
    private static final EquipmentSlot[] COSTUME_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private static class CostumeMenu implements InventoryHolder {
        Inventory inv;
        @Override public @NotNull Inventory getInventory() { return inv; }
    }

    private int costumeSlotIndex(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return -1;
        if (!it.getItemMeta().getPersistentDataContainer().has(costumeKey, PersistentDataType.STRING)) return -1;
        String n = it.getType().name();
        if (n.endsWith("_HELMET") || n.endsWith("_HEAD") || n.endsWith("_SKULL")) return 0;
        if (n.endsWith("_CHESTPLATE")) return 1;
        if (n.endsWith("_LEGGINGS")) return 2;
        if (n.endsWith("_BOOTS")) return 3;
        return -1;
    }

    private ItemStack[] costumeItems(Player p) {
        return costumeCache.computeIfAbsent(p.getUniqueId(), k -> {
            ItemStack[] out = new ItemStack[4];
            byte[] data = p.getPersistentDataContainer().get(costumeStoreKey, PersistentDataType.BYTE_ARRAY);
            if (data != null) {
                ItemStack[] stored = ItemStack.deserializeItemsFromBytes(data);
                for (int i = 0; i < 4 && i < stored.length; i++) {
                    if (stored[i] != null && !stored[i].getType().isAir()) out[i] = stored[i];
                }
            }
            return out;
        });
    }

    private void saveCostumes(Player p, ItemStack[] items) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < 4; i++) list.add(items[i] == null ? new ItemStack(Material.AIR) : items[i]);
        p.getPersistentDataContainer().set(costumeStoreKey, PersistentDataType.BYTE_ARRAY, ItemStack.serializeItemsAsBytes(list));
        costumeCache.put(p.getUniqueId(), items);
    }

    private void openCostumeMenu(Player p) {
        CostumeMenu cm = new CostumeMenu();
        cm.inv = Bukkit.createInventory(cm, 27, MM.deserialize(getConfig().getString("costume-menu.title", "Menu kostiumu")));
        ItemStack pane = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta pm = pane.getItemMeta();
        pm.displayName(MM.deserialize(getConfig().getString("costume-menu.pane-name", " ")).decoration(TextDecoration.ITALIC, false));
        pane.setItemMeta(pm);
        for (int i = 0; i < 27; i++) cm.inv.setItem(i, pane);

        ItemStack sign = new ItemStack(Material.OAK_SIGN);
        ItemMeta sm = sign.getItemMeta();
        sm.displayName(MM.deserialize(getConfig().getString("costume-menu.sign-name", "<gold>Menu kostiumu")).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        for (String l : getConfig().getStringList("costume-menu.sign-lore")) {
            lore.add(MM.deserialize(l).decoration(TextDecoration.ITALIC, false));
        }
        sm.lore(lore);
        sign.setItemMeta(sm);
        cm.inv.setItem(COSTUME_SIGN, sign);

        refreshCostumeMenu(p, cm);
        p.openInventory(cm.inv);
    }

    private void refreshCostumeMenu(Player p, CostumeMenu cm) {
        ItemStack[] items = costumeItems(p);
        for (int i = 0; i < 4; i++) cm.inv.setItem(COSTUME_FIRST + i, items[i]); // puste sloty zostaja puste
    }

    @EventHandler
    public void onCostumeMenuClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof CostumeMenu cm)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        int raw = e.getRawSlot();
        ItemStack[] items = costumeItems(p);

        if (raw >= 0 && raw < e.getInventory().getSize()) {
            int idx = raw - COSTUME_FIRST;
            if (idx < 0 || idx > 3) return;
            ItemStack cur = items[idx];
            ItemStack cursor = e.getCursor();
            if (cur != null && !cur.getType().isAir()) {
                // klikniecie w kostium: sciaga go do ekwipunku (brak miejsca = ostrzezenie)
                if (p.getInventory().firstEmpty() == -1) {
                    p.sendMessage(msg("messages.costume-full"));
                    return;
                }
                p.getInventory().addItem(cur);
                items[idx] = null;
            } else if (cursor != null && !cursor.getType().isAir()) {
                if (costumeSlotIndex(cursor) != idx) {
                    p.sendMessage(msg("messages.costume-wrong"));
                    return;
                }
                ItemStack one = cursor.clone();
                one.setAmount(1);
                items[idx] = one;
                cursor.setAmount(cursor.getAmount() - 1);
                p.setItemOnCursor(cursor.getAmount() <= 0 ? null : cursor);
            } else {
                return;
            }
        } else if (e.isShiftClick()) {
            // shift + klik na czesc kostiumu w ekwipunku: zaklada ja od razu
            ItemStack it = e.getCurrentItem();
            int idx = costumeSlotIndex(it);
            if (idx < 0) return;
            if (items[idx] != null && !items[idx].getType().isAir()) {
                p.sendMessage(msg("messages.costume-occupied"));
                return;
            }
            ItemStack one = it.clone();
            one.setAmount(1);
            items[idx] = one;
            it.setAmount(it.getAmount() - 1);
            e.setCurrentItem(it.getAmount() <= 0 ? null : it);
        } else {
            return;
        }
        saveCostumes(p, items);
        refreshCostumeMenu(p, cm);
        updateCostumeVisuals();
    }

    /** Co pol sekundy pokazuje wszystkim graczom kostium z menu na ciele gracza (prawdziwy set nadal dziala). */
    private void updateCostumeVisuals() {
        for (Player owner : Bukkit.getOnlinePlayers()) {
            ItemStack[] items = costumeItems(owner);
            boolean[] prev = costumeShown.computeIfAbsent(owner.getUniqueId(), k -> new boolean[4]);
            boolean any = false, anyPrev = false;
            for (int i = 0; i < 4; i++) {
                if (items[i] != null && !items[i].getType().isAir()) any = true;
                if (prev[i]) anyPrev = true;
            }
            if (!any && !anyPrev) continue;
            for (Player viewer : owner.getWorld().getPlayers()) {
                if (viewer != owner && viewer.getLocation().distanceSquared(owner.getLocation()) > 96 * 96) continue;
                for (int i = 0; i < 4; i++) {
                    boolean has = items[i] != null && !items[i].getType().isAir();
                    if (has) viewer.sendEquipmentChange(owner, COSTUME_SLOTS[i], items[i]);
                    else if (prev[i]) viewer.sendEquipmentChange(owner, COSTUME_SLOTS[i], owner.getInventory().getItem(COSTUME_SLOTS[i]));
                }
            }
            for (int i = 0; i < 4; i++) prev[i] = items[i] != null && !items[i].getType().isAir();
        }
    }

    // ---------- /wyplacserce <ilosc> [piekielne] ----------

    private boolean handleWithdrawHearts(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Tylko dla graczy.");
            return true;
        }
        int n;
        try {
            n = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        } catch (NumberFormatException ex) {
            n = 0;
        }
        if (n <= 0) {
            p.sendMessage(msg("messages.withdraw-usage"));
            return true;
        }
        boolean infernal = args.length > 1 && args[1].equalsIgnoreCase("piekielne");
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        if (attr == null) return true;
        double base = attr.getBaseValue();
        int total = (int) Math.round(base / 2.0);
        int infernalCount = p.getPersistentDataContainer().getOrDefault(infernalKey, PersistentDataType.INTEGER, 0);
        int normalCount = total - infernalCount;
        int minHearts = Math.max(1, getConfig().getInt("min-hearts", 1));
        int available = Math.max(0, Math.min(infernal ? infernalCount : normalCount, total - minHearts));
        if (n > available) {
            p.sendMessage(MM.deserialize(getConfig().getString("messages.withdraw-max", ""),
                    Placeholder.unparsed("max", String.valueOf(available))));
            return true;
        }
        String key = infernal ? "piekielne_serce" : "serce";
        ConfigurationSection hs = getConfig().getConfigurationSection("categories.serca.items." + key);
        if (hs == null) return true;

        setMaxHealth(p, base - 2.0 * n);
        if (infernal) p.getPersistentDataContainer().set(infernalKey, PersistentDataType.INTEGER, infernalCount - n);
        p.setHealth(Math.min(p.getHealth(), attr.getValue()));

        int left = n;
        while (left > 0) {
            ItemStack heart = buildItem(hs, key);
            if (heart == null) break;
            int amount = Math.min(64, left);
            heart.setAmount(amount);
            for (ItemStack rest : p.getInventory().addItem(heart).values()) {
                p.getWorld().dropItemNaturally(p.getLocation(), rest); // brak miejsca = na ziemie
            }
            left -= amount;
        }
        p.sendMessage(MM.deserialize(getConfig().getString("messages.withdraw-done", ""),
                Placeholder.unparsed("count", String.valueOf(n))));
        return true;
    }

    // ---------- Siekiera Brutala: LPM kradnie 30% obecnego zdrowia przeciwnika ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBrutalAxe(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity target)) return;
        if (!"siekiera_brutala".equals(eventItemId(p.getInventory().getItemInMainHand()))) return;
        if (cdLeft(p, "siekiera_brutala") > 0) return; // trwa czas odnowienia: zwykle uderzenie
        double steal = target.getHealth() * getConfig().getDouble("siekiera-brutala.steal-percent", 30) / 100.0;
        if (steal <= 0) return;
        e.setDamage(e.getDamage() + steal);
        AttributeInstance attr = p.getAttribute(Attribute.MAX_HEALTH);
        double max = attr == null ? 20.0 : attr.getValue();
        p.setHealth(Math.min(max, p.getHealth() + steal));
        cdStart(p, "siekiera_brutala", getConfig().getLong("siekiera-brutala.cooldown-seconds", 120));
    }

    // ---------- Wichrowa Kosa (maczuga): PPM wyrzuca w gore, SHIFT + PPM spowalnia graczy dookola ----------

    private void useWindMace(Player p) {
        if (p.isSneaking()) {
            if (!checkCd(p, "wichrowa_aura")) return;
            double radius = getConfig().getDouble("wichrowa-kosa.aura-radius", 8);
            int ticks = getConfig().getInt("wichrowa-kosa.aura-effect-seconds", 3) * 20;
            int fatigue = getConfig().getInt("wichrowa-kosa.mining-fatigue-level", 3) - 1;
            int slow = getConfig().getInt("wichrowa-kosa.slowness-level", 1) - 1;
            for (Player t : p.getLocation().getNearbyPlayers(radius)) {
                if (t.equals(p)) continue;
                t.addPotionEffect(new PotionEffect(PotionEffectType.MINING_FATIGUE, ticks, fatigue));
                t.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, slow));
            }
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation().add(0, 1, 0), 40, radius / 4, 0.5, radius / 4, 0.02);
            cdStart(p, "wichrowa_aura", getConfig().getLong("wichrowa-kosa.aura-cooldown-seconds", 30));
        } else {
            if (!checkCd(p, "wichrowa_launch")) return;
            Vector v = p.getVelocity();
            v.setY(getConfig().getDouble("wichrowa-kosa.launch-velocity", 2.5));
            p.setVelocity(v);
            p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 25, 0.3, 0.1, 0.3, 0.05);
            long immune = getConfig().getLong("wichrowa-kosa.fall-immunity-seconds", 10);
            if (immune > 0) windFall.put(p.getUniqueId(), System.currentTimeMillis() + immune * 1000L);
            cdStart(p, "wichrowa_launch", getConfig().getLong("wichrowa-kosa.launch-cooldown-seconds", 30));
        }
    }

    /** Po wyrzuceniu Wichrowa Kosa przez chwile chroni przed obrazeniami od upadku. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWindFall(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL || !(e.getEntity() instanceof Player p)) return;
        Long until = windFall.get(p.getUniqueId());
        if (until == null) return;
        if (System.currentTimeMillis() < until) e.setCancelled(true);
        else windFall.remove(p.getUniqueId());
    }

    // ---------- Petarda: rakieta leci w gore, wybucha po drodze i niszczy bloki dookola ----------

    private void usePetarda(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block clicked = e.getClickedBlock();
        if (clicked == null || e.getAction() != Action.RIGHT_CLICK_BLOCK) {
            p.sendMessage(msg("messages.petarda-ground"));
            return;
        }
        Location start = clicked.getRelative(e.getBlockFace()).getLocation().add(0.5, 0.0, 0.5);
        ItemStack hand = e.getItem();
        if (hand != null && p.getGameMode() != GameMode.CREATIVE) hand.setAmount(hand.getAmount() - 1); // jednorazowa

        int power = getConfig().getInt("petarda.power", 3);
        float explosion = (float) getConfig().getDouble("petarda.explosion-power", 3.0);
        boolean breakBlocks = getConfig().getBoolean("petarda.break-blocks", true);
        int interval = Math.max(2, getConfig().getInt("petarda.explosion-interval-ticks", 4));
        int safeTicks = getConfig().getInt("petarda.safe-ticks", 6); // pierwsze ticki bez wybuchow, zeby nie zranic gracza przy ziemi

        Firework fw = start.getWorld().spawn(start, Firework.class, f -> {
            FireworkMeta m = f.getFireworkMeta();
            m.setPower(power);
            m.addEffect(FireworkEffect.builder().with(FireworkEffect.Type.BURST)
                    .withColor(Color.ORANGE, Color.RED).withFlicker().build());
            f.setFireworkMeta(m);
        });
        int[] ticks = {0};
        Bukkit.getScheduler().runTaskTimer(this, task -> {
            ticks[0] += 2;
            if (!fw.isValid()) {
                task.cancel();
                return;
            }
            Location at = fw.getLocation();
            boolean blocked = at.clone().add(0, 1.2, 0).getBlock().getType().isSolid(); // przeszkoda nad rakieta
            if (ticks[0] >= safeTicks && (ticks[0] % interval == 0 || blocked)) {
                at.getWorld().createExplosion(at, blocked ? explosion * 1.5f : explosion, false, breakBlocks, p);
            }
            if (blocked) {
                fw.detonate(); // rozbija sie o przeszkode
                task.cancel();
            }
        }, 2L, 2L);
    }

    // ---------- Miecz N1: szansa na Sile 2 przy uderzeniu ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onN1Sword(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p)) return;
        if (!"miecz_n1".equals(eventItemId(p.getInventory().getItemInMainHand()))) return;
        if (Math.random() * 100 >= getConfig().getDouble("miecz-n1.chance", 35)) return;
        int ticks = getConfig().getInt("miecz-n1.seconds", 5) * 20;
        int amp = getConfig().getInt("miecz-n1.strength-level", 2) - 1;
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, amp));
    }

    // ---------- Miecz N1: szansa na Sile 2 przy uderzeniu ----------

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwordN1(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player p) || !(e.getEntity() instanceof LivingEntity)) return;
        if (!"miecz_n1".equals(eventItemId(p.getInventory().getItemInMainHand()))) return;
        if (Math.random() * 100 >= getConfig().getDouble("miecz-n1.chance", 35)) return;
        int ticks = getConfig().getInt("miecz-n1.strength-seconds", 5) * 20;
        int amp = getConfig().getInt("miecz-n1.strength-level", 2) - 1;
        p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, ticks, amp));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getInventory().getHolder();
        if (h instanceof Menu || h instanceof Pocket || h instanceof CostumeMenu) e.setCancelled(true);
        if (h instanceof Reroll) {
            for (int raw : e.getRawSlots()) {
                if (raw < e.getInventory().getSize()) { e.setCancelled(true); break; }
            }
        }
    }

    private Component msg(String path) {
        return MM.deserialize(getConfig().getString(path, ""));
    }

    private String formatTime(long sec) {
        long h = sec / 3600, m = (sec % 3600) / 60, s = sec % 60;
        return h + "h " + m + "m " + s + "s";
    }
}
