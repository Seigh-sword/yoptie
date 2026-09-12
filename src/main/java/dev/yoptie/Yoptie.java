package dev.yoptie;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Yoptie implements ModInitializer {
	public static final String MOD_ID = "yoptie";
	public static final String MOD_NAME = "Yoptie";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		YoptieConfig config = YoptieConfig.load();
		String version = FabricLoader.getInstance()
				.getModContainer(MOD_ID)
				.map(container -> container.getMetadata().getVersion().getFriendlyString())
				.orElse("development");

		LOGGER.info("{} {} loaded: {}", MOD_NAME, version, config.summary());
	}
}
