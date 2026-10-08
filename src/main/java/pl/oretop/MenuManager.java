package pl.oretop;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** Całe GUI: menu główne, ranking z paginacją i statystyki gracza. */
public final class MenuManager implements Listener {

    public enum Type { MAIN, TOP, STATS }

    public static final class Holder implements InventoryHolder {
        private final Type type;
        private final Stat stat;
        private final int page;
        private final int pages;
        private final UUID target;
        private Inventory inventory;

        private Holder(Type type, Stat stat, int page, int pages, UUID target) {
            this.type = type;
            this.stat = stat;
            this.page = page;
            this.pages = pages;
            this.target = target;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final int SIZE = 54;
    private static final int SLOT_HEADER = 4;
    private static final int SLOT_PREV = 45;
    private static final int SLOT_BACK = 49;
    private static final int SLOT_NEXT = 53;
    private static final int[] CONTENT = buildContentSlots();

    private final OreTopPlugin plugin;
    private final StatsManager stats;

    public MenuManager(OreTopPlugin plugin, StatsManager stats) {
        this.plugin = plugin;
        this.stats = stats;
    }

    private static int[] buildContentSlots() {
        int[] slots = new int[28];
        int i = 0;
        for (int row = 1; row <= 4; row++) {
            for (int col = 1; col <= 7; col++) {
                slots[i++] = row * 9 + col;
            }
        }
        return slots;
    }

    // ------------------------------------------------------------ otwieranie

    public void openMain(Player player) {
        Holder holder = new Holder(Type.MAIN, null, 0, 1, null);
        Inventory inv = Bukkit.createInventory(holder, SIZE,
                plugin.mm(plugin.getConfig().getString("gui.main-title", "<dark_gray>Topki serwera")));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(SLOT_HEADER, head(player.getUniqueId(), "<yellow><bold>Twoje statystyki",
                List.of("<gray>Kliknij, aby zobaczyć swoje", "<gray>wyniki i miejsca w rankingach")));

        for (Stat stat : plugin.enabledStats()) {
            int slot = plugin.statSlot(stat);
            if (slot >= 0 && slot < SIZE) {
                inv.setItem(slot, categoryItem(player, stat));
            }
        }
        inv.setItem(SLOT_BACK, item(Material.BARRIER, "<red><bold>Zamknij", List.of()));
        player.openInventory(inv);
    }

    public void openTop(Player player, Stat stat, int requestedPage) {
        List<StatsManager.Entry> list = stats.top(stat);
        int pages = Math.max(1, (int) Math.ceil(list.size() / (double) CONTENT.length));
        int page = Math.max(0, Math.min(requestedPage, pages - 1));

        Holder holder = new Holder(Type.TOP, stat, page, pages, null);
        String title = plugin.getConfig().getString("gui.top-title", "<dark_gray>Top » %name%")
                .replace("%name%", plugin.plain(plugin.statName(stat)))
                .replace("%page%", String.valueOf(page + 1));
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.mm(title));
        holder.inventory = inv;
        fill(inv);

        UUID me = player.getUniqueId();
        int myRank = stats.rank(stat, me);
        List<String> headerLore = new ArrayList<>();
        headerLore.add("<gray>" + plugin.statDescription(stat));
        headerLore.add("");
        headerLore.add("<gray>Twój wynik: <yellow>" + fmt(stats.value(stat, me))
                + (myRank > 0 ? " <dark_gray>(#" + myRank + ")" : ""));
        headerLore.add("<gray>Strona: <white>" + (page + 1) + "<dark_gray>/<white>" + pages);
        inv.setItem(SLOT_HEADER, item(plugin.statMaterial(stat), plugin.statName(stat), headerLore));

        if (list.isEmpty()) {
            inv.setItem(22, item(Material.PAPER, "<gray>Brak danych",
                    List.of("<dark_gray>Nikt jeszcze nie ma wyniku", "<dark_gray>w tej kategorii.")));
        }

        for (int i = 0; i < CONTENT.length; i++) {
            int index = page * CONTENT.length + i;
            if (index >= list.size()) {
                break;
            }
            StatsManager.Entry entry = list.get(index);
            int rank = index + 1;
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Wynik: <yellow>" + fmt(entry.value()));
            lore.add("<gray>Miejsce: " + rankColor(rank) + "#" + rank);
            lore.add("");
            lore.add("<green>▶ Kliknij, aby zobaczyć statystyki");
            inv.setItem(CONTENT[i], head(entry.uuid(), rankColor(rank) + "<bold>#" + rank + " <white>" + entry.name(), lore));
        }

        if (page > 0) {
            inv.setItem(SLOT_PREV, item(Material.ARROW, "<yellow>« Poprzednia strona",
                    List.of("<gray>Strona " + page + "/" + pages)));
        }
        if (page < pages - 1) {
            inv.setItem(SLOT_NEXT, item(Material.ARROW, "<yellow>Następna strona »",
                    List.of("<gray>Strona " + (page + 2) + "/" + pages)));
        }
        inv.setItem(SLOT_BACK, item(Material.OAK_DOOR, "<red><bold>Powrót do menu", List.of()));
        player.openInventory(inv);
    }

    public void openStats(Player viewer, UUID target) {
        Holder holder = new Holder(Type.STATS, null, 0, 1, target);
        String name = stats.name(target);
        String title = plugin.getConfig().getString("gui.stats-title", "<dark_gray>Statystyki » %player%")
                .replace("%player%", name);
        Inventory inv = Bukkit.createInventory(holder, SIZE, plugin.mm(title));
        holder.inventory = inv;
        fill(inv);

        inv.setItem(SLOT_HEADER, head(target, "<yellow><bold>" + name,
                List.of("<gray>Statystyki i miejsca w rankingach")));

        for (Stat stat : plugin.enabledStats()) {
            int slot = plugin.statSlot(stat);
            if (slot < 0 || slot >= SIZE) {
                continue;
            }
            int rank = stats.rank(stat, target);
            List<String> lore = new ArrayList<>();
            lore.add("<gray>Wynik: <yellow>" + fmt(stats.value(stat, target)));
            lore.add(rank > 0 ? "<gray>Miejsce: " + rankColor(rank) + "#" + rank : "<dark_gray>Brak miejsca w rankingu");
            lore.add("");
            lore.add("<green>▶ Kliknij, aby zobaczyć ranking");
            inv.setItem(slot, item(plugin.statMaterial(stat), plugin.statName(stat), lore));
        }
        inv.setItem(SLOT_BACK, item(Material.OAK_DOOR, "<red><bold>Powrót do menu", List.of()));
        viewer.openInventory(inv);
    }

    // --------------------------------------------------------------- kliknięcia

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null || event.getClickedInventory() != top) {
            return;
        }
        int slot = event.getSlot();
        if (slot < 0 || slot >= SIZE || top.getItem(slot) == null) {
            return;
        }

        switch (holder.type) {
            case MAIN -> {
                if (slot == SLOT_HEADER) {
                    click(player);
                    later(() -> openStats(player, player.getUniqueId()));
                } else if (slot == SLOT_BACK) {
                    click(player);
                    player.closeInventory();
                } else {
                    Stat stat = statAtSlot(slot);
                    if (stat != null) {
                        click(player);
                        later(() -> openTop(player, stat, 0));
                    }
                }
            }
            case TOP -> {
                if (slot == SLOT_PREV && holder.page > 0) {
                    click(player);
                    later(() -> openTop(player, holder.stat, holder.page - 1));
                } else if (slot == SLOT_NEXT && holder.page < holder.pages - 1) {
                    click(player);
                    later(() -> openTop(player, holder.stat, holder.page + 1));
                } else if (slot == SLOT_BACK) {
                    click(player);
                    later(() -> openMain(player));
                } else {
                    for (int i = 0; i < CONTENT.length; i++) {
                        if (CONTENT[i] != slot) {
                            continue;
                        }
                        List<StatsManager.Entry> list = stats.top(holder.stat);
                        int index = holder.page * CONTENT.length + i;
                        if (index < list.size()) {
                            UUID target = list.get(index).uuid();
                            click(player);
                            later(() -> openStats(player, target));
                        }
                        break;
                    }
                }
            }
            case STATS -> {
                if (slot == SLOT_BACK) {
                    click(player);
                    later(() -> openMain(player));
                } else {
                    Stat stat = statAtSlot(slot);
                    if (stat != null) {
                        click(player);
                        later(() -> openTop(player, stat, 0));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    // -------------------------------------------------------------- pomocnicze

    private Stat statAtSlot(int slot) {
        for (Stat stat : plugin.enabledStats()) {
            if (plugin.statSlot(stat) == slot) {
                return stat;
            }
        }
        return null;
    }

    private void later(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    private void click(Player player) {
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
    }

    private ItemStack categoryItem(Player viewer, Stat stat) {
        List<String> lore = new ArrayList<>();
        lore.add("<gray>" + plugin.statDescription(stat));
        lore.add("");

        List<StatsManager.Entry> top = stats.top(stat);
        if (top.isEmpty()) {
            lore.add("<dark_gray>Brak danych");
        } else {
            for (int i = 0; i < Math.min(3, top.size()); i++) {
                StatsManager.Entry e = top.get(i);
                lore.add(rankColor(i + 1) + "#" + (i + 1) + " <white>" + e.name()
                        + " <dark_gray>» <yellow>" + fmt(e.value()));
            }
        }

        UUID id = viewer.getUniqueId();
        int rank = stats.rank(stat, id);
        lore.add("");
        lore.add("<gray>Twój wynik: <yellow>" + fmt(stats.value(stat, id))
                + (rank > 0 ? " <dark_gray>(#" + rank + ")" : ""));
        lore.add("");
        lore.add("<green>▶ Kliknij, aby zobaczyć ranking");
        return item(plugin.statMaterial(stat), plugin.statName(stat), lore);
    }

    private void fill(Inventory inv) {
        Material material = Material.matchMaterial(plugin.getConfig().getString("gui.filler", "BLACK_STAINED_GLASS_PANE"));
        if (material == null) {
            material = Material.BLACK_STAINED_GLASS_PANE;
        }
        ItemStack filler = item(material, " ", List.of());
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(plugin.mm(name));
        List<Component> lines = new ArrayList<>();
        for (String line : lore) {
            lines.add(plugin.mm(line));
        }
        meta.lore(lines);
        meta.addItemFlags(ItemFlag.values());
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack head(UUID owner, String name, List<String> lore) {
        ItemStack stack = item(Material.PLAYER_HEAD, name, lore);
        if (stack.getItemMeta() instanceof SkullMeta skull) {
            skull.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            stack.setItemMeta(skull);
        }
        return stack;
    }

    private static String rankColor(int rank) {
        return switch (rank) {
            case 1 -> "<#FFD700>";
            case 2 -> "<#C0C0C0>";
            case 3 -> "<#CD7F32>";
            default -> "<gray>";
        };
    }

    private static String fmt(long number) {
        return String.format(Locale.US, "%,d", number).replace(',', ' ');
    }
}
