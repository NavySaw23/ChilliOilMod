package net.chillioil;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.chillioil.aesthetic.armor.ArmorCommand;
import net.chillioil.aesthetic.armor.ArmorConfig;
import net.chillioil.aesthetic.skin.PlayerSkinState;
import net.chillioil.aesthetic.skin.SkinCommand;
import net.chillioil.aesthetic.skin.SkinData;
import net.chillioil.aesthetic.skin.SkinWardrobeConfig;
import net.chillioil.aesthetic.skin.WardrobeCommand;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ChilliOil implements ModInitializer {
	public static final String MOD_ID = "chillioil";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing ChilliOil mod for Minecraft 1.21.11!");

		// Initialize aesthetic armor config
		ArmorConfig.init();

		// Initialize aesthetic skin & wardrobe config
		SkinWardrobeConfig.init();

		// Initialize management module config
		net.chillioil.management.ModuleConfig.init();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ArmorCommand.register(dispatcher, registryAccess);
			SkinCommand.register(dispatcher, registryAccess);
			WardrobeCommand.register(dispatcher, registryAccess);
			net.chillioil.management.ChilliConfigCommand.register(dispatcher, registryAccess);
		});

		// Connection events for recording player default skins and cleanup
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				GameProfile profile = player.getGameProfile();
				Collection<Property> textures = profile.properties().get("textures");
				if (textures != null && !textures.isEmpty()) {
					Property prop = textures.iterator().next();
					SkinData defaultSkin = new SkinData(
						SkinData.VARIANT_CLASSIC,
						player.getPlainTextName(),
						"DEFAULT",
						prop.value(),
						prop.signature()
					);
					PlayerSkinState.setDefaultSkin(player.getUUID(), defaultSkin);
				}
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				PlayerSkinState.onPlayerQuit(player.getUUID());
			}
		});
	}
}
