package net.chillioil.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.chillioil.aesthetic.skin.ActiveSkinStorage;
import net.chillioil.aesthetic.skin.PlayerSkinState;
import net.chillioil.aesthetic.skin.SkinData;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void chilliOil$onPlaceNewPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        if (player == null) {
            return;
        }

        GameProfile originalProfile = player.getGameProfile();
        Collection<Property> textures = originalProfile.properties().get("textures");
        if (textures != null && !textures.isEmpty()) {
            Property originalProp = textures.iterator().next();
            SkinData defaultSkin = new SkinData(
                SkinData.VARIANT_CLASSIC,
                player.getPlainTextName(),
                "DEFAULT",
                originalProp.value(),
                originalProp.signature()
            );
            PlayerSkinState.setDefaultSkin(player.getUUID(), defaultSkin);
        }

        // Check if there is a saved custom skin from a previous session
        Optional<SkinData> savedActiveSkin = ActiveSkinStorage.loadActiveSkin(player.getUUID());
        if (savedActiveSkin.isPresent()) {
            SkinData customSkin = savedActiveSkin.get();
            try {
                com.google.common.collect.Multimap<String, Property> newMap = com.google.common.collect.LinkedHashMultimap.create();
                for (Map.Entry<String, Property> entry : originalProfile.properties().entries()) {
                    if (!"textures".equals(entry.getKey())) {
                        newMap.put(entry.getKey(), entry.getValue());
                    }
                }
                newMap.put("textures", new Property("textures", customSkin.value(), customSkin.signature()));

                GameProfile modifiedProfile = new GameProfile(originalProfile.id(), originalProfile.name(), new PropertyMap(newMap));

                Field gameProfileField = null;
                for (Field f : net.minecraft.world.entity.player.Player.class.getDeclaredFields()) {
                    if (f.getType().equals(GameProfile.class)) {
                        gameProfileField = f;
                        break;
                    }
                }
                if (gameProfileField != null) {
                    gameProfileField.setAccessible(true);
                    gameProfileField.set(player, modifiedProfile);
                    PlayerSkinState.setActiveSkin(player.getUUID(), customSkin);
                }
            } catch (Exception e) {
                // If anything fails during reflection, fallback gracefully
            }
        }
    }
}
