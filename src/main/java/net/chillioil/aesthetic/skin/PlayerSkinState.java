package net.chillioil.aesthetic.skin;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerSkinState {
    // Original skin of player when they connected (default)
    private static final Map<UUID, SkinData> DEFAULT_SKINS = new ConcurrentHashMap<>();

    // Currently active applied skin
    private static final Map<UUID, SkinData> ACTIVE_SKINS = new ConcurrentHashMap<>();

    // Last time the player switched skins (cooldown tracking)
    private static final Map<UUID, Instant> LAST_SWITCH_TIME = new ConcurrentHashMap<>();

    public static void setDefaultSkin(UUID uuid, SkinData skin) {
        DEFAULT_SKINS.putIfAbsent(uuid, skin);
        ACTIVE_SKINS.putIfAbsent(uuid, skin);
    }

    public static SkinData getDefaultSkin(UUID uuid) {
        return DEFAULT_SKINS.get(uuid);
    }

    public static SkinData getActiveSkin(UUID uuid) {
        return ACTIVE_SKINS.get(uuid);
    }

    public static void setActiveSkin(UUID uuid, SkinData skin) {
        ACTIVE_SKINS.put(uuid, skin);
        LAST_SWITCH_TIME.put(uuid, Instant.now());
    }

    public static int getRemainingCooldown(UUID uuid) {
        Instant last = LAST_SWITCH_TIME.get(uuid);
        if (last == null) {
            return 0;
        }
        int cooldownSec = SkinWardrobeConfig.getSwitchCooldownSeconds();
        long passedSec = java.time.Duration.between(last, Instant.now()).toSeconds();
        return (int) Math.max(0, cooldownSec - passedSec);
    }

    public static int getRemainingWardrobeCooldown(UUID uuid) {
        Instant last = LAST_SWITCH_TIME.get(uuid);
        if (last == null) {
            return 0;
        }
        int cooldownSec = SkinWardrobeConfig.getWardrobeSwitchCooldownSeconds();
        if (cooldownSec <= 0) {
            return 0;
        }
        long passedSec = java.time.Duration.between(last, Instant.now()).toSeconds();
        return (int) Math.max(0, cooldownSec - passedSec);
    }

    public static void onPlayerQuit(UUID uuid) {
        LAST_SWITCH_TIME.remove(uuid);
        DEFAULT_SKINS.remove(uuid);
        ACTIVE_SKINS.remove(uuid);
    }
}
