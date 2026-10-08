package pl.oretop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** /top [kategoria|reload] */
public final class TopCommand implements TabExecutor {

    private final OreTopPlugin plugin;
    private final StatsManager stats;
    private final MenuManager menus;

    public TopCommand(OreTopPlugin plugin, StatsManager stats, MenuManager menus) {
        this.plugin = plugin;
        this.stats = stats;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("oretop.admin")) {
                sender.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(plugin.msg("reloaded"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("only-player"));
            return true;
        }

        if (args.length == 0) {
            menus.openMain(player);
            return true;
        }

        Stat stat = Stat.fromKey(args[0]);
        if (stat == null || !plugin.statEnabled(stat)) {
            List<String> keys = new ArrayList<>();
            for (Stat s : plugin.enabledStats()) {
                keys.add(s.key());
            }
            sender.sendMessage(plugin.msg("unknown-category", "%list%", String.join(", ", keys)));
            return true;
        }
        menus.openTop(player, stat, 0);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        for (Stat stat : plugin.enabledStats()) {
            if (stat.key().startsWith(prefix)) {
                options.add(stat.key());
            }
        }
        if (sender.hasPermission("oretop.admin") && "reload".startsWith(prefix)) {
            options.add("reload");
        }
        return options;
    }
}
