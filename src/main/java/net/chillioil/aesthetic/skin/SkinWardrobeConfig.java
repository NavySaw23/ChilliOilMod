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

public class SkinWardrobeConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/SkinWardrobeConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ConfigData {
        public String apiKey = "";
        public int wardrobeLimit = 20;
        public int switchCooldownSeconds = 10;
        public int wardrobeSwitchCooldownSeconds = 0;
        public int freeTierDelaySeconds = 5;
    }

    private static ConfigData data = new ConfigData();

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
        return getConfigDir().resolve("skin-wardrobe.json");
    }

    public static void init() {
        load();
    }

    public static synchronized void load() {
        try {
            Path configDir = getConfigDir();
            Path configFile = getConfigFile();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                data = new ConfigData();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
                if (loaded != null) {
                    data = loaded;
                }
            }
            LOGGER.info("Loaded skin-wardrobe config from {}", configFile);
        } catch (Exception e) {
            LOGGER.error("Failed to load skin-wardrobe config from {}", getConfigFile(), e);
            data = new ConfigData();
        }
    }

    public static synchronized void save() {
        try {
            Path configDir = getConfigDir();
            Path configFile = getConfigFile();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }
            try (Writer writer = Files.newBufferedWriter(configFile)) {
                GSON.toJson(data, writer);
            }
            LOGGER.info("Saved skin-wardrobe config to {}", configFile);
        } catch (Exception e) {
            LOGGER.error("Failed to save skin-wardrobe config to {}", getConfigFile(), e);
        }
    }

    public static String getApiKey() {
        return data.apiKey != null ? data.apiKey.trim() : "";
    }

    public static int getWardrobeLimit() {
        return data.wardrobeLimit > 0 ? data.wardrobeLimit : 20;
    }

    public static int getSwitchCooldownSeconds() {
        return Math.max(0, data.switchCooldownSeconds);
    }

    public static int getWardrobeSwitchCooldownSeconds() {
        return Math.max(0, data.wardrobeSwitchCooldownSeconds);
    }

    public static int getFreeTierDelaySeconds() {
        return Math.max(1, data.freeTierDelaySeconds);
    }

    public static synchronized void resetDefaults() {
        data = new ConfigData();
        save();
    }
}

