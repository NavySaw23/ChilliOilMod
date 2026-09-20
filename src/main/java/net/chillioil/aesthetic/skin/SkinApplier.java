package net.chillioil.aesthetic.skin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.chillioil.aesthetic.armor.ArmorTagHelper;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class SkinApplier {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChilliOil/SkinApplier");

    public static boolean applySkin(ServerPlayer player, SkinData skinData) {
        if (player == null || skinData == null) {
            return false;
        }

        try {
            GameProfile oldProfile = player.getGameProfile();

            String textureValue = skinData.value();
            String signature = skinData.signature();

            // Copy existing properties into a mutable multimap, excluding existing "textures"
            com.google.common.collect.Multimap<String, Property> newMap = com.google.common.collect.LinkedHashMultimap.create();
            for (Map.Entry<String, Property> entry : oldProfile.properties().entries()) {
                if (!"textures".equals(entry.getKey())) {
                    newMap.put(entry.getKey(), entry.getValue());
                }
            }
            newMap.put("textures", new Property("textures", textureValue, signature));

            // Construct new GameProfile with the updated PropertyMap
            GameProfile newProfile = new GameProfile(oldProfile.id(), oldProfile.name(), new PropertyMap(newMap));

            // Replace gameProfile field on Player
            Field gameProfileField = null;
            for (Field f : net.minecraft.world.entity.player.Player.class.getDeclaredFields()) {
                if (f.getType().equals(GameProfile.class)) {
                    gameProfileField = f;
                    break;
                }
            }
            if (gameProfileField != null) {
                gameProfileField.setAccessible(true);
                gameProfileField.set(player, newProfile);
            } else {
                LOGGER.error("Could not find gameProfile field on Player class");
                return false;
            }

            // Record state and persist
            PlayerSkinState.setActiveSkin(player.getUUID(), skinData);
            if ("DEFAULT".equalsIgnoreCase(skinData.sourceType())) {
                ActiveSkinStorage.removeActiveSkin(player.getUUID());
            } else {
                ActiveSkinStorage.saveActiveSkin(player.getUUID(), skinData);
            }

            // Resynchronize skin with all players and self
            refreshPlayerSkin(player);
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to apply skin to player {}", player.getPlainTextName(), e);
            return false;
        }
    }

    private static String adjustModelVariantInPayload(String base64Payload, String variant) {
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Payload);
            String jsonStr = new String(decoded, StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();

            if (json.has("textures")) {
                JsonObject textures = json.getAsJsonObject("textures");
                if (textures.has("SKIN")) {
                    JsonObject skin = textures.getAsJsonObject("SKIN");
                    JsonObject metadata = skin.has("metadata") ? skin.getAsJsonObject("metadata") : new JsonObject();
                    if (SkinData.VARIANT_SLIM.equalsIgnoreCase(variant)) {
                        metadata.addProperty("model", "slim");
                        skin.add("metadata", metadata);
                    } else if (SkinData.VARIANT_CLASSIC.equalsIgnoreCase(variant)) {
                        metadata.remove("model");
                        if (metadata.size() > 0) {
                            skin.add("metadata", metadata);
                        } else {
                            skin.remove("metadata");
                        }
                    }
                    return Base64.getEncoder().encodeToString(json.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
        } catch (Exception ignored) {
        }
        return base64Payload;
    }

    public static void refreshPlayerSkin(ServerPlayer player) {
        ServerLevel level = player.level();

        // 1. Remove player from tab list for all players (including self)
        ClientboundPlayerInfoRemovePacket removePacket = new ClientboundPlayerInfoRemovePacket(List.of(player.getUUID()));
        level.getServer().getPlayerList().broadcastAll(removePacket);

        // 2. Add player back with updated GameProfile (and updated textures)
        ClientboundPlayerInfoUpdatePacket addPacket = ClientboundPlayerInfoUpdatePacket.createPlayerInitializing(List.of(player));
        level.getServer().getPlayerList().broadcastAll(addPacket);

        // 3. Re-track entity for viewers in the chunk
        refreshTrackingForViewers(player, level);

        // 4. Send respawn packet to self so local client immediately updates first-person & third-person models
        byte b = 0;
        player.connection.send(new ClientboundRespawnPacket(player.createCommonSpawnInfo(level), b));
        player.connection.teleport(player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
        level.getServer().getPlayerList().sendLevelInfo(player, level);
        level.getServer().getPlayerList().sendPlayerPermissionLevel(player);
        level.getServer().getPlayerList().sendAllPlayerInfo(player);
        level.getServer().getPlayerList().sendActivePlayerEffects(player);
        player.initInventoryMenu();
        player.setHealth(player.getHealth());
        player.onUpdateAbilities();

        // 5. Send entity metadata
        syncModelCustomisation(player);

        // Re-send equipment to restore invisible armor data components to client
        ArmorTagHelper.resendEquipment(player);
    }

    private static void syncModelCustomisation(ServerPlayer player) {
        try {
            List<SynchedEntityData.DataValue<?>> nonDefault = player.getEntityData().getNonDefaultValues();
            if (nonDefault != null && !nonDefault.isEmpty()) {
                ClientboundSetEntityDataPacket entityDataPacket = new ClientboundSetEntityDataPacket(player.getId(), nonDefault);
                player.connection.send(entityDataPacket);
                player.level().getChunkSource().sendToTrackingPlayers(player, entityDataPacket);
            }
        } catch (Exception e) {
            LOGGER.debug("Could not resync entity data for {}", player.getPlainTextName(), e);
        }
    }

    private static void refreshTrackingForViewers(ServerPlayer player, ServerLevel level) {
        try {
            ChunkMap chunkMap = level.getChunkSource().chunkMap;
            // Access entityMap in chunkMap using reflection to find TrackedEntity for player
            Field entityMapField = null;
            for (Field f : ChunkMap.class.getDeclaredFields()) {
                if (f.getType().getSimpleName().contains("Int2ObjectMap")) {
                    entityMapField = f;
                    break;
                }
            }

            if (entityMapField != null) {
                entityMapField.setAccessible(true);
                Object entityMap = entityMapField.get(chunkMap);
                Method getMethod = entityMap.getClass().getMethod("get", int.class);
                Object trackedEntity = getMethod.invoke(entityMap, player.getId());
                if (trackedEntity != null) {
                    // TrackedEntity methods: broadcastRemoved() and updatePlayers()
                    Method broadcastRemovedMethod = trackedEntity.getClass().getDeclaredMethod("broadcastRemoved");
                    broadcastRemovedMethod.setAccessible(true);
                    broadcastRemovedMethod.invoke(trackedEntity);

                    Method updatePlayersMethod = trackedEntity.getClass().getDeclaredMethod("updatePlayers", List.class);
                    updatePlayersMethod.setAccessible(true);
                    updatePlayersMethod.invoke(trackedEntity, level.players());
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not refresh chunk tracked entity directly for {}", player.getPlainTextName(), e);
        }
    }
}
