package net.chillioil.aesthetic.armor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ArmorConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/ArmorConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type SET_TYPE = new TypeToken<LinkedHashSet<String>>() {}.getType();

    private static Path getConfigDir() {
        try {
            if (FabricLoader.getInstance() != null && FabricLoader.getInstance().getConfigDir() != null) {
                return FabricLoader.getInstance().getConfigDir().resolve("ChilliOil");
            }
        } catch (Throwable ignored) {
        }
        return Path.of("config", "ChilliOil");
    }

    private static Path getConfigFile() {
        return getConfigDir().resolve("armor.json");
    }

    private static final Set<Identifier> ALLOWED_ITEMS = Collections.synchronizedSet(new LinkedHashSet<>());

    public static void init() {
        load();
    }

    public static synchronized void load() {
        ALLOWED_ITEMS.clear();
        try {
            Path configDir = getConfigDir();
            Path configFile = getConfigFile();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                loadDefaults();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                Set<String> ids = GSON.fromJson(reader, SET_TYPE);
                if (ids != null) {
                    for (String idStr : ids) {
                        Identifier id = Identifier.tryParse(idStr);
                        if (id != null) {
                            ALLOWED_ITEMS.add(id);
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} armor items from {}", ALLOWED_ITEMS.size(), configFile);
        } catch (Exception e) {
            LOGGER.error("Failed to load armor config from {}", getConfigFile(), e);
            loadDefaults();
        }
    }

    public static synchronized void save() {
        try {
            Path configDir = getConfigDir();
            Path configFile = getConfigFile();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            List<String> list = new ArrayList<>();
            for (Identifier id : ALLOWED_ITEMS) {
                list.add(id.toString());
            }
            Collections.sort(list);
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(list, writer);
            }
            LOGGER.info("Saved {} armor items to {}", list.size(), configFile);
        } catch (Exception e) {
            LOGGER.error("Failed to save armor config to {}", getConfigFile(), e);
        }
    }

    public static boolean contains(Identifier id) {
        return ALLOWED_ITEMS.contains(id);
    }

    public static synchronized boolean add(Identifier id) {
        if (ALLOWED_ITEMS.add(id)) {
            save();
            return true;
        }
        return false;
    }

    public static synchronized boolean remove(Identifier id) {
        if (ALLOWED_ITEMS.remove(id)) {
            save();
            return true;
        }
        return false;
    }

    public static List<Identifier> getItems() {
        List<Identifier> copy = new ArrayList<>(ALLOWED_ITEMS);
        copy.sort((a, b) -> a.toString().compareTo(b.toString()));
        return copy;
    }

    private static void loadDefaults() {
        ALLOWED_ITEMS.clear();

        // Standard Armors (Helmet, Chestplate, Leggings, Boots)
        String[] materials = {"leather", "chainmail", "iron", "golden", "diamond", "netherite"};
        String[] slots = {"helmet", "chestplate", "leggings", "boots"};
        for (String material : materials) {
            for (String slot : slots) {
                ALLOWED_ITEMS.add(Identifier.withDefaultNamespace(material + "_" + slot));
            }
        }

        // Turtle Helmet & Elytra
        ALLOWED_ITEMS.add(Identifier.withDefaultNamespace("turtle_helmet"));
        ALLOWED_ITEMS.add(Identifier.withDefaultNamespace("elytra"));

        // Horse Armors
        String[] horseTiers = {"leather", "iron", "golden", "diamond"};
        for (String tier : horseTiers) {
            ALLOWED_ITEMS.add(Identifier.withDefaultNamespace(tier + "_horse_armor"));
        }

        // Wolf Armor
        ALLOWED_ITEMS.add(Identifier.withDefaultNamespace("wolf_armor"));

        // Saddles
        ALLOWED_ITEMS.add(Identifier.withDefaultNamespace("saddle"));

        // Ghast Harnesses (all 16 colors)
        String[] colors = {
            "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"
        };
        for (String color : colors) {
            ALLOWED_ITEMS.add(Identifier.withDefaultNamespace(color + "_harness"));
        }
    }
}
