package net.chillioil.aesthetic.skin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public class ActiveSkinStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/ActiveSkinStorage");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path getActiveSkinsDir() {
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
        return baseDir.resolve("world").resolve("chillioil").resolve("activeskins");
    }

    private static Path getPlayerFile(UUID uuid) {
        return getActiveSkinsDir().resolve(uuid.toString() + ".json");
    }

    public static synchronized Optional<SkinData> loadActiveSkin(UUID uuid) {
        Path file = getPlayerFile(uuid);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        try (Reader reader = Files.newBufferedReader(file)) {
            SkinData skin = GSON.fromJson(reader, SkinData.class);
            return Optional.ofNullable(skin);
        } catch (Exception e) {
            LOGGER.error("Failed to load active skin for player {}", uuid, e);
            return Optional.empty();
        }
    }

    public static synchronized void saveActiveSkin(UUID uuid, SkinData skin) {
        try {
            Path dir = getActiveSkinsDir();
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path file = getPlayerFile(uuid);
            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(skin, writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to save active skin for player {}", uuid, e);
        }
    }

    public static synchronized void removeActiveSkin(UUID uuid) {
        try {
            Path file = getPlayerFile(uuid);
            if (Files.exists(file)) {
                Files.delete(file);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to delete active skin for player {}", uuid, e);
        }
    }
}
