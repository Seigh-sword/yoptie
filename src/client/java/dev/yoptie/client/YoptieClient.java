package dev.yoptie.client;

import dev.yoptie.Yoptie;
import net.fabricmc.api.ClientModInitializer;

public class YoptieClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Yoptie.LOGGER.info("Client hooks armed");
	}
}
