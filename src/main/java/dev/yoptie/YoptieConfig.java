package dev.yoptie;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

public final class YoptieConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FILE_NAME = "yoptie.json";
	private static volatile YoptieConfig instance;

	public boolean enabled = true;
	public Telemetry telemetry = new Telemetry();
	public Particles particles = new Particles();
	public Entities entities = new Entities();
	public RenderDistance renderDistance = new RenderDistance();

	public static YoptieConfig get() {
		YoptieConfig current = instance;

		if (current == null) {
			current = load();
		}

		return current;
	}

	public static synchronized YoptieConfig load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
		YoptieConfig config = new YoptieConfig();

		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				YoptieConfig parsed = GSON.fromJson(reader, YoptieConfig.class);

				if (parsed != null) {
					config = parsed;
				}
			} catch (IOException | RuntimeException exception) {
				Yoptie.LOGGER.warn("Could not read {}, falling back to defaults", path, exception);
			}
		}

		config.validate();
		instance = config;
		config.save(path);
		return config;
	}

	public void save(Path path) {
		try {
			Path parent = path.getParent();

			if (parent != null) {
				Files.createDirectories(parent);
			}

			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException exception) {
			Yoptie.LOGGER.warn("Could not write {}", path, exception);
		}
	}

	public String summary() {
		return "telemetry " + (telemetry.disabled ? "off" : "on")
				+ ", particle budget " + (particles.limitTotal ? Integer.toString(particles.maxParticles) : "off")
				+ ", entity cull " + (entities.distanceCulling ? entities.entityDistance + " blocks" : "off")
				+ ", block entity cull " + (entities.blockEntityCulling ? entities.blockEntityDistance + " blocks" : "off")
				+ ", render distance cap " + (renderDistance.capEnabled ? renderDistance.maxChunks + " chunks" : "off")
				+ ", tick culling " + (entities.tickCulling ? "on" : "off");
	}

	private void validate() {
		if (telemetry == null) {
			telemetry = new Telemetry();
		}

		if (particles == null) {
			particles = new Particles();
		}

		if (entities == null) {
			entities = new Entities();
		}

		if (renderDistance == null) {
			renderDistance = new RenderDistance();
		}

		particles.maxParticles = clamp(particles.maxParticles, 0, 200000);
		entities.entityDistance = clamp(entities.entityDistance, 0.0, 1024.0);
		entities.playerDistance = clamp(entities.playerDistance, 0.0, 1024.0);
		entities.blockEntityDistance = clamp(entities.blockEntityDistance, 0.0, 1024.0);
		renderDistance.maxChunks = clamp(renderDistance.maxChunks, 2, 32);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		if (Double.isNaN(value)) {
			return max;
		}

		return Math.max(min, Math.min(max, value));
	}

	public static final class Telemetry {
		public boolean disabled = true;
	}

	public static final class Particles {
		public boolean limitTotal = true;
		public int maxParticles = 4000;
	}

	public static final class Entities {
		public boolean distanceCulling = true;
		public double entityDistance = 48.0;
		public double playerDistance = 64.0;
		public boolean blockEntityCulling = true;
		public double blockEntityDistance = 64.0;
		public boolean tickCulling = false;
		public boolean tickCullPlayers = false;
	}

	public static final class RenderDistance {
		public boolean capEnabled = true;
		public int maxChunks = 16;
	}
}
