package dev.yoptie;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MixinTargetsTest {
	private static final String CONFIG = "yoptie.client.mixins.json";

	private static final String[] MIXINS = {
			"dev.yoptie.client.mixin.BlockEntityRenderDispatcherMixin",
			"dev.yoptie.client.mixin.ClientLevelMixin",
			"dev.yoptie.client.mixin.EntityRenderDispatcherMixin",
			"dev.yoptie.client.mixin.MinecraftMixin",
			"dev.yoptie.client.mixin.OptionsMixin",
			"dev.yoptie.client.mixin.ParticleEngineMixin"
	};

	private static final String[][] METHODS = {
			{"net.minecraft.client.Minecraft", "allowsTelemetry", "0"},
			{"net.minecraft.client.Options", "getEffectiveRenderDistance", "0"},
			{"net.minecraft.client.particle.ParticleEngine", "add", "1"},
			{"net.minecraft.client.particle.ParticleEngine", "tick", "0"},
			{"net.minecraft.client.particle.ParticleGroup", "size", "0"},
			{"net.minecraft.client.renderer.entity.EntityRenderDispatcher", "shouldRender", "5"},
			{"net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher", "tryExtractRenderState", "4"},
			{"net.minecraft.client.multiplayer.ClientLevel", "tickNonPassenger", "1"}
	};

	private static final String[][] FIELDS = {
			{"net.minecraft.client.particle.ParticleEngine", "particles", "java.util.Map"},
			{"net.minecraft.client.particle.ParticleEngine", "particlesToAdd", "java.util.Queue"},
			{"net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher", "cameraPos", "net.minecraft.world.phys.Vec3"}
	};

	private static final String[][] HANDLERS = {
			{"net.minecraft.client.Minecraft", "yoptie$disableTelemetry"},
			{"net.minecraft.client.Options", "yoptie$capRenderDistance"},
			{"net.minecraft.client.particle.ParticleEngine", "yoptie$enforceParticleBudget"},
			{"net.minecraft.client.renderer.entity.EntityRenderDispatcher", "yoptie$cullDistantEntities"},
			{"net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher", "yoptie$cullDistantBlockEntities"},
			{"net.minecraft.client.multiplayer.ClientLevel", "yoptie$skipDistantTicks"}
	};

	private static final ClassLoader GAME_LOADER = gameLoader();

	static {
		register();
	}

	@Test
	void targetsExist() throws Exception {
		ClassLoader loader = GAME_LOADER;
		List<String> problems = new ArrayList<>();
		List<String> missingHandlers = new ArrayList<>();

		diagnostic("context loader " + name(Thread.currentThread().getContextClassLoader()));
		diagnostic("game loader " + name(loader));

		for (String[] entry : METHODS) {
			Class<?> owner = load(loader, entry[0], problems);

			if (owner != null && find(owner, entry[1], Integer.parseInt(entry[2])) == null) {
				problems.add(entry[0] + "#" + entry[1] + " with " + entry[2] + " parameters is missing");
			}
		}

		for (String[] entry : FIELDS) {
			Class<?> owner = load(loader, entry[0], problems);

			if (owner != null && findField(owner, entry[1], entry[2]) == null) {
				problems.add(entry[0] + "#" + entry[1] + " of type " + entry[2] + " is missing");
			}
		}

		for (String[] entry : HANDLERS) {
			Class<?> owner = load(loader, entry[0], problems);

			if (owner != null) {
				Method handler = findHandler(owner, entry[1]);
				diagnostic(entry[0] + " " + entry[1] + " -> " + (handler == null ? "missing" : handler));

				if (handler == null) {
					missingHandlers.add(entry[0] + "#" + entry[1]);
				}
			}
		}

		for (String mixin : MIXINS) {
			String resource = mixin.replace('.', '/') + ".class";

			if (loader.getResource(resource) == null) {
				problems.add(resource + " is missing");
				diagnostic(resource + " missing");
			}
		}

		diagnostic("declared config in mod json " + modJsonDeclaresConfig(loader));

		if (!missingHandlers.isEmpty()) {
			problems.add("handlers not applied [" + String.join(" ", missingHandlers) + "]");
		}

		if (!problems.isEmpty()) {
			throw new AssertionError("VERIFY " + String.join(" | ", problems));
		}

		System.out.println("::notice title=yoptie-verify::all targets and handlers are live");
	}

	@Test
	void configBehaviour() throws Exception {
		List<String> problems = new ArrayList<>();
		YoptieConfig config = new YoptieConfig();

		check(config.enabled, "enabled default", problems);
		check(config.telemetry.disabled, "telemetry disabled by default", problems);
		check(config.particles.limitTotal, "particle budget on by default", problems);
		check(config.particles.maxParticles == 4000, "particle budget default", problems);
		check(config.entities.distanceCulling, "entity culling on by default", problems);
		check(config.entities.entityDistance == 48.0, "entity distance default", problems);
		check(config.entities.playerDistance == 64.0, "player distance default", problems);
		check(config.entities.blockEntityCulling, "block entity culling on by default", problems);
		check(config.entities.blockEntityDistance == 64.0, "block entity distance default", problems);
		check(!config.entities.tickCulling, "tick culling off by default", problems);
		check(!config.entities.tickCullPlayers, "player tick culling off by default", problems);
		check(config.renderDistance.capEnabled, "render distance cap on by default", problems);
		check(config.renderDistance.maxChunks == 16, "render distance cap default", problems);

		config.particles.maxParticles = -5;
		config.entities.entityDistance = 99999.0;
		config.entities.playerDistance = Double.NaN;
		config.entities.blockEntityDistance = -1.0;
		config.renderDistance.maxChunks = 999;
		invokeValidate(config);

		check(config.particles.maxParticles == 0, "particle count clamp", problems);
		check(config.entities.entityDistance == 1024.0, "entity distance clamp", problems);
		check(config.entities.playerDistance == 1024.0, "player distance clamp", problems);
		check(config.entities.blockEntityDistance == 0.0, "block entity distance clamp", problems);
		check(config.renderDistance.maxChunks == 32, "render distance clamp", problems);

		config.particles.maxParticles = 900000;
		config.renderDistance.maxChunks = 1;
		invokeValidate(config);

		check(config.particles.maxParticles == 200000, "particle count upper clamp", problems);
		check(config.renderDistance.maxChunks == 2, "render distance lower clamp", problems);

		Path file = Files.createTempFile("yoptie", ".json");

		try {
			config.save(file);
			String written = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
			check(written.contains("\"maxChunks\": 2"), "config file written", problems);
		} finally {
			Files.deleteIfExists(file);
		}

		if (!problems.isEmpty()) {
			throw new AssertionError("VERIFY " + String.join(" | ", problems));
		}

		System.out.println("::notice title=yoptie-verify::config defaults, clamps and file output are correct");
	}

	private static ClassLoader gameLoader() {
		try {
			Class<?> launcherBase = Class.forName("net.fabricmc.loader.impl.launch.FabricLauncherBase");
			Object launcher = launcherBase.getMethod("getLauncher").invoke(null);
			Object loader = launcher.getClass().getMethod("getTargetClassLoader").invoke(launcher);

			if (loader instanceof ClassLoader) {
				return (ClassLoader) loader;
			}
		} catch (Throwable error) {
			diagnostic("game loader unavailable " + error);
		}

		return Thread.currentThread().getContextClassLoader();
	}

	private static void register() {
		try {
			Class<?> mixins = Class.forName("org.spongepowered.asm.mixin.Mixins", true, GAME_LOADER);
			diagnostic("mixin class shared " + (mixins == Class.forName("org.spongepowered.asm.mixin.Mixins", true, Thread.currentThread().getContextClassLoader())));
			diagnostic("mixin configs before " + configNames(mixins));

			if (!configNames(mixins).contains(CONFIG)) {
				mixins.getMethod("addConfiguration", String.class).invoke(null, CONFIG);
				diagnostic("mixin configs after " + configNames(mixins));
			}
		} catch (Throwable error) {
			diagnostic("mixin registration failed " + error);
		}
	}

	private static String name(ClassLoader loader) {
		return loader == null ? "bootstrap" : loader.getClass().getName();
	}

	private static String configNames(Class<?> mixins) {
		try {
			StringBuilder builder = new StringBuilder();

			for (Object config : (Iterable<?>) mixins.getMethod("getConfigs").invoke(null)) {
				Method name = config.getClass().getMethod("getName");
				builder.append(name.invoke(config)).append(' ');
			}

			return builder.toString().trim();
		} catch (Throwable error) {
			return "unavailable " + error;
		}
	}

	private static String modJsonDeclaresConfig(ClassLoader loader) {
		try (InputStream stream = loader.getResourceAsStream("fabric.mod.json")) {
			if (stream == null) {
				return "mod json missing";
			}

			byte[] bytes = new byte[4096];
			int length = stream.read(bytes);

			if (length <= 0) {
				return "mod json empty";
			}

			return Boolean.toString(new String(bytes, 0, length, StandardCharsets.UTF_8).contains(CONFIG));
		} catch (Throwable error) {
			return "unavailable " + error;
		}
	}

	private static void invokeValidate(YoptieConfig config) throws Exception {
		Method validate = YoptieConfig.class.getDeclaredMethod("validate");
		validate.setAccessible(true);
		validate.invoke(config);
	}

	private static void check(boolean condition, String label, List<String> problems) {
		if (!condition) {
			problems.add(label + " is wrong");
		}
	}

	private static Class<?> load(ClassLoader loader, String name, List<String> problems) {
		try {
			return Class.forName(name, false, loader);
		} catch (Throwable error) {
			problems.add(name + " is not loadable: " + error);
			diagnostic(name + " not loadable " + error);
			return null;
		}
	}

	private static Method find(Class<?> owner, String name, int parameters) {
		for (Method method : owner.getDeclaredMethods()) {
			if (method.getName().equals(name) && method.getParameterCount() == parameters) {
				return method;
			}
		}

		return null;
	}

	private static Method findHandler(Class<?> owner, String name) {
		for (Method method : owner.getDeclaredMethods()) {
			if (method.getName().equals(name) || method.getName().endsWith("$" + name)) {
				return method;
			}
		}

		return null;
	}

	private static Field findField(Class<?> owner, String name, String type) {
		for (Class<?> current = owner; current != null; current = current.getSuperclass()) {
			for (Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name) && field.getType().getName().equals(type)) {
					return field;
				}
			}
		}

		return null;
	}

	private static void diagnostic(String message) {
		System.out.println("YOPTIE-DIAGNOSTIC " + message);
	}
}
