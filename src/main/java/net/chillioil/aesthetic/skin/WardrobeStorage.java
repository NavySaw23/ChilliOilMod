package net.chillioil.aesthetic.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WardrobeStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/WardrobeStorage");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<LinkedHashMap<String, SkinData>>() {}.getType();

    // In-memory cache per player UUID
    private static final Map<UUID, LinkedHashMap<String, SkinData>> CACHE = new ConcurrentHashMap<>();

    private static Path getWardrobeDir() {
        Path baseDir;
        try {
            if (FabricLoader.getInstance() != null && FabricLoader.getInstance().getGameDir() != null) {
                baseDir = FabricLoader.getInstance().getGameDir();
            } else {
                baseDir = Path.of(".");
            }
        } catch (Throwable ignored) {
            baseDir = Path.of(".");
        }
        return baseDir.resolve("world").resolve("chillioil").resolve("wardrobes");
    }

    private static Path getPlayerFile(UUID uuid) {
        return getWardrobeDir().resolve(uuid.toString() + ".json");
    }

    public static synchronized LinkedHashMap<String, SkinData> getWardrobe(UUID uuid) {
        return CACHE.computeIfAbsent(uuid, WardrobeStorage::loadFromDisk);
    }

    private static LinkedHashMap<String, SkinData> loadFromDisk(UUID uuid) {
        Path file = getPlayerFile(uuid);
        if (!Files.exists(file)) {
            return new LinkedHashMap<>();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            LinkedHashMap<String, SkinData> map = GSON.fromJson(reader, MAP_TYPE);
            return map != null ? map : new LinkedHashMap<>();
        } catch (Exception e) {
            LOGGER.error("Failed to load wardrobe for player {}", uuid, e);
            return new LinkedHashMap<>();
        }
    }

    public static synchronized boolean saveToDisk(UUID uuid) {
        LinkedHashMap<String, SkinData> map = CACHE.get(uuid);
        if (map == null) {
            map = new LinkedHashMap<>();
        }
        try {
            Path dir = getWardrobeDir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = getPlayerFile(uuid);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(map, writer);
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to save wardrobe for player {}", uuid, e);
            return false;
        }
    }

    public static synchronized List<Map.Entry<String, SkinData>> getEntries(UUID uuid) {
        LinkedHashMap<String, SkinData> map = getWardrobe(uuid);
        return new ArrayList<>(map.entrySet());
    }

    public static synchronized Optional<SkinData> getSkin(UUID uuid, String name) {
        LinkedHashMap<String, SkinData> map = getWardrobe(uuid);
        return Optional.ofNullable(map.get(name.toLowerCase()));
    }

    public static synchronized boolean addSkin(UUID uuid, String name, SkinData skin) {
        LinkedHashMap<String, SkinData> map = getWardrobe(uuid);
        if (map.size() >= SkinWardrobeConfig.getWardrobeLimit()) {
            return false;
        }
        map.put(name.toLowerCase(), skin);
        saveToDisk(uuid);
        return true;
    }

    public static synchronized boolean removeSkin(UUID uuid, String name) {
        LinkedHashMap<String, SkinData> map = getWardrobe(uuid);
        SkinData removed = map.remove(name.toLowerCase());
        if (removed != null) {
            saveToDisk(uuid);
            return true;
        }
        return false;
    }
}
