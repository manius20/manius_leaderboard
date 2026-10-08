package pl.oretop;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /stats [gracz] */
public final class StatsCommand implements TabExecutor {

    private final OreTopPlugin plugin;
    private final StatsManager stats;
    private final MenuManager menus;

    public StatsCommand(OreTopPlugin plugin, StatsManager stats, MenuManager menus) {
        this.plugin = plugin;
        this.stats = stats;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("only-player"));
            return true;
        }

        UUID target = player.getUniqueId();
        if (args.length > 0) {
            UUID found = stats.findUuid(args[0]);
            if (found == null) {
                sender.sendMessage(plugin.msg("player-not-found", "%player%", args[0]));
                return true;
            }
            target = found;
        }
        menus.openStats(player, target);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return List.of();
        }
        String prefix = args[0].toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        for (String name : stats.knownNames()) {
            if (name.toLowerCase(Locale.ROOT).startsWith(prefix)) {
                options.add(name);
            }
        }
        return options;
    }
}
