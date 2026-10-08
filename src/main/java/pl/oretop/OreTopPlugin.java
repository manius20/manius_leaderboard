package pl.oretop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public final class OreTopPlugin extends JavaPlugin {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private StatsManager stats;
    private MenuManager menus;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        stats = new StatsManager(this);
        try {
            stats.init();
        } catch (Exception ex) {
            getLogger().log(Level.SEVERE, "Nie udało się uruchomić bazy SQLite - wyłączam plugin", ex);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        menus = new MenuManager(this, stats);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(new TrackListener(this, stats), this);

        register("top", new TopCommand(this, stats, menus));
        register("stats", new StatsCommand(this, stats, menus));

        long ticks = Math.max(10, getConfig().getInt("settings.save-interval-seconds", 60)) * 20L;
        getServer().getScheduler().runTaskTimerAsynchronously(this, stats::flush, ticks, ticks);
    }

    @Override
    public void onDisable() {
        if (stats != null) {
            stats.flush();
        }
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning("Brak komendy w plugin.yml: " + name);
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }

    // ------------------------------------------------------------ teksty

    /** Zamienia tekst MiniMessage na komponent bez domyślnej kursywy. */
    public Component mm(String text) {
        return MM.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public String plain(String miniMessage) {
        return MM.stripTags(miniMessage);
    }

    /** Wiadomość z sekcji messages; pary: "%klucz%", "wartość". */
    public Component msg(String key, String... replacements) {
        String text = getConfig().getString("messages." + key, key);
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            text = text.replace(replacements[i], replacements[i + 1]);
        }
        return mm(text);
    }

    // ---------------------------------------------------- ustawienia kategorii

    public String statName(Stat stat) {
        return getConfig().getString("categories." + stat.key() + ".name", stat.defaultName());
    }

    public String statDescription(Stat stat) {
        return getConfig().getString("categories." + stat.key() + ".description", stat.defaultDescription());
    }

    public Material statMaterial(Stat stat) {
        String raw = getConfig().getString("categories." + stat.key() + ".material", stat.icon().name());
        Material material = Material.matchMaterial(raw);
        return material != null ? material : stat.icon();
    }

    public int statSlot(Stat stat) {
        return getConfig().getInt("categories." + stat.key() + ".slot", stat.defaultSlot());
    }

    public boolean statEnabled(Stat stat) {
        return getConfig().getBoolean("categories." + stat.key() + ".enabled", true);
    }

    public List<Stat> enabledStats() {
        List<Stat> list = new ArrayList<>();
        for (Stat stat : Stat.values()) {
            if (statEnabled(stat)) {
                list.add(stat);
            }
        }
        return list;
    }
}
