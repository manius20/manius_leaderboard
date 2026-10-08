package pl.oretop;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Trzyma statystyki w pamięci i zapisuje je asynchronicznie do SQLite.
 * Metody odczytu rankingów (top/rank) wywołuj z głównego wątku.
 */
public final class StatsManager {

    public record Entry(UUID uuid, String name, long value) {
    }

    private static final String UPSERT_STAT =
            "INSERT INTO stats (uuid, stat, value) VALUES (?, ?, ?) "
                    + "ON CONFLICT(uuid, stat) DO UPDATE SET value = excluded.value";
    private static final String UPSERT_NAME =
            "INSERT INTO names (uuid, name) VALUES (?, ?) "
                    + "ON CONFLICT(uuid) DO UPDATE SET name = excluded.name";

    private final OreTopPlugin plugin;
    private final Map<Stat, Map<UUID, Long>> data = new EnumMap<>(Stat.class);
    private final Map<UUID, String> names = new ConcurrentHashMap<>();
    private final Set<String> dirtyStats = ConcurrentHashMap.newKeySet();
    private final Set<UUID> dirtyNames = ConcurrentHashMap.newKeySet();
    private final Map<Stat, List<Entry>> cache = new EnumMap<>(Stat.class);

    private long cacheStamp;
    private volatile boolean ready;
    private String url;

    public StatsManager(OreTopPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() throws Exception {
        File folder = plugin.getDataFolder();
        if (!folder.exists() && !folder.mkdirs()) {
            throw new SQLException("Nie można utworzyć folderu pluginu: " + folder);
        }
        Class.forName("org.sqlite.JDBC");
        url = "jdbc:sqlite:" + new File(folder, "stats.db").getAbsolutePath();

        for (Stat stat : Stat.values()) {
            data.put(stat, new ConcurrentHashMap<>());
        }

        try (Connection c = DriverManager.getConnection(url); Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS stats ("
                    + "uuid TEXT NOT NULL, stat TEXT NOT NULL, value INTEGER NOT NULL, "
                    + "PRIMARY KEY (uuid, stat))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS names ("
                    + "uuid TEXT PRIMARY KEY, name TEXT NOT NULL)");

            try (ResultSet rs = st.executeQuery("SELECT uuid, stat, value FROM stats")) {
                while (rs.next()) {
                    Stat stat = Stat.fromKey(rs.getString("stat"));
                    UUID uuid = parseUuid(rs.getString("uuid"));
                    if (stat != null && uuid != null) {
                        data.get(stat).put(uuid, rs.getLong("value"));
                    }
                }
            }
            try (ResultSet rs = st.executeQuery("SELECT uuid, name FROM names")) {
                while (rs.next()) {
                    UUID uuid = parseUuid(rs.getString("uuid"));
                    if (uuid != null) {
                        names.put(uuid, rs.getString("name"));
                    }
                }
            }
        }
        ready = true;
    }

    private static UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------- zapis

    public void add(Stat stat, UUID uuid, long amount) {
        data.get(stat).merge(uuid, amount, Long::sum);
        dirtyStats.add(uuid + "|" + stat.key());
    }

    public synchronized void flush() {
        if (!ready || (dirtyStats.isEmpty() && dirtyNames.isEmpty())) {
            return;
        }
        List<String> statKeys = new ArrayList<>(dirtyStats);
        List<UUID> nameKeys = new ArrayList<>(dirtyNames);
        dirtyStats.removeAll(statKeys);
        dirtyNames.removeAll(nameKeys);

        try (Connection c = DriverManager.getConnection(url)) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(UPSERT_STAT)) {
                for (String key : statKeys) {
                    int sep = key.indexOf('|');
                    UUID uuid = parseUuid(key.substring(0, sep));
                    Stat stat = Stat.fromKey(key.substring(sep + 1));
                    if (uuid == null || stat == null) {
                        continue;
                    }
                    ps.setString(1, uuid.toString());
                    ps.setString(2, stat.key());
                    ps.setLong(3, data.get(stat).getOrDefault(uuid, 0L));
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            try (PreparedStatement ps = c.prepareStatement(UPSERT_NAME)) {
                for (UUID uuid : nameKeys) {
                    String name = names.get(uuid);
                    if (name == null) {
                        continue;
                    }
                    ps.setString(1, uuid.toString());
                    ps.setString(2, name);
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            c.commit();
        } catch (SQLException ex) {
            dirtyStats.addAll(statKeys);
            dirtyNames.addAll(nameKeys);
            plugin.getLogger().log(Level.WARNING, "Nie udało się zapisać statystyk do SQLite", ex);
        }
    }

    // ---------------------------------------------------------------- nazwy

    public void setName(UUID uuid, String name) {
        String old = names.put(uuid, name);
        if (!name.equals(old)) {
            dirtyNames.add(uuid);
        }
    }

    public String name(UUID uuid) {
        return names.getOrDefault(uuid, "Nieznany");
    }

    public UUID findUuid(String name) {
        for (Map.Entry<UUID, String> e : names.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) {
                return e.getKey();
            }
        }
        return null;
    }

    public Collection<String> knownNames() {
        return names.values();
    }

    // ------------------------------------------------------------- rankingi

    public long value(Stat stat, UUID uuid) {
        return data.get(stat).getOrDefault(uuid, 0L);
    }

    /** Posortowana lista (malejąco) graczy z wynikiem większym od zera. */
    public List<Entry> top(Stat stat) {
        long now = System.currentTimeMillis();
        long ttl = plugin.getConfig().getLong("settings.cache-seconds", 5) * 1000L;
        if (now - cacheStamp > ttl) {
            cache.clear();
            cacheStamp = now;
        }
        return cache.computeIfAbsent(stat, key -> {
            List<Entry> list = new ArrayList<>();
            for (Map.Entry<UUID, Long> e : data.get(key).entrySet()) {
                if (e.getValue() > 0) {
                    list.add(new Entry(e.getKey(), name(e.getKey()), e.getValue()));
                }
            }
            list.sort(Comparator.comparingLong(Entry::value).reversed()
                    .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
            return List.copyOf(list);
        });
    }

    /** Miejsce gracza w rankingu (od 1) albo 0, jeśli nie ma jeszcze wyniku. */
    public int rank(Stat stat, UUID uuid) {
        List<Entry> list = top(stat);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }
}
