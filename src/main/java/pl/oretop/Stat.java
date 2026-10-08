package pl.oretop;

import org.bukkit.Material;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/** Wszystkie śledzone statystyki. Rudy mają przypisane bloki, za które są liczone. */
public enum Stat {
    KILLS("kills", Material.DIAMOND_SWORD, "<red><bold>Zabójstwa graczy", 10, "Ilu graczy zabiłeś"),
    DEATHS("deaths", Material.SKELETON_SKULL, "<dark_gray><bold>Śmierci", 12, "Ile razy zginąłeś"),
    MOBS("mobs", Material.ROTTEN_FLESH, "<green><bold>Zabite moby", 14, "Ile potworów i zwierząt zabiłeś"),
    BLOCKS("blocks", Material.DIAMOND_PICKAXE, "<gold><bold>Wykopane bloki", 16, "Ile bloków zniszczyłeś"),

    DIAMOND("diamond", Material.DIAMOND, "<aqua><bold>Diamenty", 20, "Wykopane rudy diamentu",
            Material.DIAMOND_ORE, Material.DEEPSLATE_DIAMOND_ORE),
    EMERALD("emerald", Material.EMERALD, "<green><bold>Szmaragdy", 21, "Wykopane rudy szmaragdu",
            Material.EMERALD_ORE, Material.DEEPSLATE_EMERALD_ORE),
    NETHERITE("netherite", Material.NETHERITE_SCRAP, "<dark_purple><bold>Netheryt", 22, "Wykopany ancient debris",
            Material.ANCIENT_DEBRIS),
    GOLD("gold", Material.GOLD_INGOT, "<yellow><bold>Złoto", 23, "Wykopane rudy złota",
            Material.GOLD_ORE, Material.DEEPSLATE_GOLD_ORE, Material.NETHER_GOLD_ORE),
    IRON("iron", Material.IRON_INGOT, "<white><bold>Żelazo", 24, "Wykopane rudy żelaza",
            Material.IRON_ORE, Material.DEEPSLATE_IRON_ORE),
    COPPER("copper", Material.COPPER_INGOT, "<#E07A3F><bold>Miedź", 29, "Wykopane rudy miedzi",
            Material.COPPER_ORE, Material.DEEPSLATE_COPPER_ORE),
    REDSTONE("redstone", Material.REDSTONE, "<red><bold>Redstone", 30, "Wykopane rudy redstone",
            Material.REDSTONE_ORE, Material.DEEPSLATE_REDSTONE_ORE),
    LAPIS("lapis", Material.LAPIS_LAZULI, "<blue><bold>Lapis", 31, "Wykopane rudy lapis lazuli",
            Material.LAPIS_ORE, Material.DEEPSLATE_LAPIS_ORE),
    COAL("coal", Material.COAL, "<dark_gray><bold>Węgiel", 32, "Wykopane rudy węgla",
            Material.COAL_ORE, Material.DEEPSLATE_COAL_ORE),
    QUARTZ("quartz", Material.QUARTZ, "<white><bold>Kwarc", 33, "Wykopane rudy kwarcu",
            Material.NETHER_QUARTZ_ORE);

    private static final Map<Material, Stat> BY_ORE = new EnumMap<>(Material.class);
    private static final Map<String, Stat> BY_KEY = new HashMap<>();

    static {
        for (Stat stat : values()) {
            BY_KEY.put(stat.key, stat);
            for (Material ore : stat.ores) {
                BY_ORE.put(ore, stat);
            }
        }
    }

    private final String key;
    private final Material icon;
    private final String defaultName;
    private final int defaultSlot;
    private final String defaultDescription;
    private final Material[] ores;

    Stat(String key, Material icon, String defaultName, int defaultSlot, String defaultDescription, Material... ores) {
        this.key = key;
        this.icon = icon;
        this.defaultName = defaultName;
        this.defaultSlot = defaultSlot;
        this.defaultDescription = defaultDescription;
        this.ores = ores;
    }

    public String key() {
        return key;
    }

    public Material icon() {
        return icon;
    }

    public String defaultName() {
        return defaultName;
    }

    public int defaultSlot() {
        return defaultSlot;
    }

    public String defaultDescription() {
        return defaultDescription;
    }

    /** Zwraca statystykę rudy odpowiadającą blokowi albo null, jeśli to nie ruda. */
    public static Stat byOre(Material material) {
        return BY_ORE.get(material);
    }

    public static Stat fromKey(String key) {
        return key == null ? null : BY_KEY.get(key.toLowerCase());
    }
}
