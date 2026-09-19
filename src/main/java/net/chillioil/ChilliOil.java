package net.chillioil;

import net.chillioil.aesthetic.armor.ArmorCommand;
import net.chillioil.aesthetic.armor.ArmorConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChilliOil implements ModInitializer {
	public static final String MOD_ID = "chillioil";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing ChilliOil mod for Minecraft 1.21.11!");

		// Initialize aesthetic armor config
		ArmorConfig.init();

		// Register commands
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			ArmorCommand.register(dispatcher, registryAccess);
		});
	}
}
