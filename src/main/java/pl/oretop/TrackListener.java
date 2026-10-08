package pl.oretop;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Zlicza kopanie, zabójstwa i śmierci. */
public final class TrackListener implements Listener {

    private record BlockKey(UUID world, int x, int y, int z) {
    }

    private static final int MAX_TRACKED_PLACED = 200_000;

    private final OreTopPlugin plugin;
    private final StatsManager stats;
    private final Set<BlockKey> placedOres = ConcurrentHashMap.newKeySet();

    public TrackListener(OreTopPlugin plugin, StatsManager stats) {
        this.plugin = plugin;
        this.stats = stats;
    }

    private static BlockKey key(Block block) {
        return new BlockKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        stats.setName(player.getUniqueId(), player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Block block = event.getBlockPlaced();
        if (Stat.byOre(block.getType()) != null) {
            if (placedOres.size() > MAX_TRACKED_PLACED) {
                placedOres.clear();
            }
            placedOres.add(key(block));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Stat ore = Stat.byOre(block.getType());
        boolean wasPlaced = ore != null && placedOres.remove(key(block));

        if (plugin.getConfig().getBoolean("settings.ignore-creative", true)
                && player.getGameMode() == GameMode.CREATIVE) {
            return;
        }

        UUID id = player.getUniqueId();
        stats.setName(id, player.getName());
        stats.add(Stat.BLOCKS, id, 1);

        boolean skipPlaced = wasPlaced && plugin.getConfig().getBoolean("settings.ignore-player-placed-ores", true);
        if (ore != null && !skipPlaced) {
            stats.add(ore, id, 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        stats.setName(victim.getUniqueId(), victim.getName());
        stats.add(Stat.DEATHS, victim.getUniqueId(), 1);

        Player killer = victim.getKiller();
        if (killer != null && !killer.getUniqueId().equals(victim.getUniqueId())) {
            stats.setName(killer.getUniqueId(), killer.getName());
            stats.add(Stat.KILLS, killer.getUniqueId(), 1);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMobDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) {
            return;
        }
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            stats.setName(killer.getUniqueId(), killer.getName());
            stats.add(Stat.MOBS, killer.getUniqueId(), 1);
        }
    }
}
