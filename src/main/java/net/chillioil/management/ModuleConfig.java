package net.chillioil.management;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModuleConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/ModuleConfig");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static class ModuleEntry {
        public boolean enabled;
        public String description;

        public ModuleEntry() {}

        public ModuleEntry(boolean enabled, String description) {
            this.enabled = enabled;
            this.description = description;
        }
    }

    private static final Map<String, ModuleEntry> MODULES = new LinkedHashMap<>();

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
        return getConfigDir().resolve("modules.json");
    }

    public static void init() {
        load();
    }

    private static void setupDefaults() {
        MODULES.clear();
        MODULES.put("armor", new ModuleEntry(true, "Aesthetic equipment and armor model visibility toggles"));
        MODULES.put("skin", new ModuleEntry(true, "Dynamic player skin fetching, restyling, and Mojang/MineSkin resolution"));
        MODULES.put("wardrobe", new ModuleEntry(true, "Personal player saved outfit wardrobe system"));
    }

    public static synchronized void load() {
        try {
            Path configDir = getConfigDir();
            Path configFile = getConfigFile();
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            if (!Files.exists(configFile)) {
                setupDefaults();
                save();
                return;
            }

            try (Reader reader = Files.newBufferedReader(configFile)) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> loaded = GSON.fromJson(reader, Map.class);
                setupDefaults(); // Ensure default keys and descriptions are preset
                if (loaded != null) {
                    for (Map.Entry<String, Map<String, Object>> entry : loaded.entrySet()) {
                        String key = entry.getKey().toLowerCase();
                        Map<String, Object> data = entry.getValue();
                        if (data != null && data.containsKey("enabled")) {
                            boolean enabled = Boolean.parseBoolean(String.valueOf(data.get("enabled")));
                            String desc = data.containsKey("description") ? String.valueOf(data.get("description")) : "";
                            if (MODULES.containsKey(key)) {
                                ModuleEntry existing = MODULES.get(key);
                                existing.enabled = enabled;
                                if (!desc.isBlank()) {
                                    existing.description = desc;
                                }
                            } else {
                                MODULES.put(key, new ModuleEntry(enabled, desc));
                            }
                        }
                    }
                }
            }
            LOGGER.info("Loaded {} modules configuration from {}", MODULES.size(), configFile);
        } catch (Exception e) {
            LOGGER.error("Failed to load modules config from {}", getConfigFile(), e);
            setupDefaults();
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
                GSON.toJson(MODULES, writer);
            }
            LOGGER.info("Saved modules configuration to {}", configFile);
        } catch (Exception e) {
            LOGGER.error("Failed to save modules configuration to {}", getConfigFile(), e);
        }
    }

    public static synchronized void resetDefaults() {
        setupDefaults();
        save();
    }

    public static synchronized boolean isModuleEnabled(String moduleName) {
        if (moduleName == null) return true;
        ModuleEntry entry = MODULES.get(moduleName.toLowerCase());
        return entry == null || entry.enabled;
    }

    public static synchronized boolean setModuleEnabled(String moduleName, boolean enabled) {
        if (moduleName == null) return false;
        String key = moduleName.toLowerCase();
        ModuleEntry entry = MODULES.get(key);
        if (entry != null) {
            entry.enabled = enabled;
            save();
            return true;
        }
        return false;
    }

    public static synchronized Map<String, ModuleEntry> getModules() {
        return new LinkedHashMap<>(MODULES);
    }
}
