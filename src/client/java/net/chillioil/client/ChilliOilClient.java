package net.chillioil.client;

import net.fabricmc.api.ClientModInitializer;
import net.chillioil.ChilliOil;

public class ChilliOilClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		ChilliOil.LOGGER.info("Initializing ChilliOil Client!");
	}
}
