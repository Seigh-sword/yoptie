package dev.yoptie;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class MixinTargetsTest {
	private static final String[] HANDLERS = {
			"net.minecraft.client.Minecraft#yoptie$disableTelemetry",
			"net.minecraft.client.Options#yoptie$capRenderDistance",
			"net.minecraft.client.particle.ParticleEngine#yoptie$enforceParticleBudget",
			"net.minecraft.client.renderer.entity.EntityRenderDispatcher#yoptie$cullDistantEntities",
			"net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher#yoptie$cullDistantBlockEntities",
			"net.minecraft.client.multiplayer.ClientLevel#yoptie$skipDistantTicks"
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

	@Test
	void targetsAndHandlersExist() throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		List<String> problems = new ArrayList<>();
		report(loader);

		for (String entry : HANDLERS) {
			String[] parts = entry.split("#");
			Class<?> owner = load(loader, parts[0], problems);

			if (owner == null) {
				continue;
			}

			Method handler = find(owner, parts[1]);

			if (handler == null) {
				problems.add(entry + " is missing, the mixin was not applied");
			} else {
				notice("handler " + entry + " applied");
			}
		}

		for (String[] entry : METHODS) {
			Class<?> owner = load(loader, entry[0], problems);

			if (owner == null) {
				continue;
			}

			Method method = find(owner, entry[1], Integer.parseInt(entry[2]));

			if (method == null) {
				problems.add(entry[0] + "#" + entry[1] + " with " + entry[2] + " parameters is missing");
			} else {
				notice("target " + entry[0] + "#" + entry[1] + " present");
			}
		}

		for (String[] entry : FIELDS) {
			Class<?> owner = load(loader, entry[0], problems);

			if (owner == null) {
				continue;
			}

			Field field = findField(owner, entry[1], entry[2]);

			if (field == null) {
				problems.add(entry[0] + "#" + entry[1] + " of type " + entry[2] + " is missing");
			} else {
				notice("shadow " + entry[0] + "#" + entry[1] + " present");
			}
		}

		if (!problems.isEmpty()) {
			throw new AssertionError(String.join("; ", problems));
		}
	}

	private static Class<?> load(ClassLoader loader, String name, List<String> problems) {
		try {
			return Class.forName(name, false, loader);
		} catch (ClassNotFoundException | LinkageError error) {
			problems.add(name + " could not be loaded: " + error);
			return null;
		}
	}

	private static void report(ClassLoader loader) {
		notice("side " + System.getProperty("fabric.side") + " development " + System.getProperty("fabric.development"));
		notice("mods " + modIds());
		notice("mixin config visible " + (loader.getResource("yoptie.client.mixins.json") != null));
		notice("mod json visible " + (loader.getResource("fabric.mod.json") != null));
		notice("classpath " + classpathEntries());

		try {
			Class<?> particles = Class.forName("net.minecraft.client.particle.ParticleEngine", false, loader);
			StringBuilder names = new StringBuilder();

			for (Method method : particles.getDeclaredMethods()) {
				if (method.getName().contains("yoptie")) {
					names.append(method.getName()).append(' ');
				}
			}

			notice("injected methods on ParticleEngine [" + names.toString().trim() + "]");
		} catch (Throwable error) {
			notice("ParticleEngine probe failed " + error);
		}
	}

	private static String modIds() {
		try {
			Class<?> loaderClass = Class.forName("net.fabricmc.loader.api.FabricLoader");
			Class<?> containerClass = Class.forName("net.fabricmc.loader.api.ModContainer");
			Class<?> metadataClass = Class.forName("net.fabricmc.loader.api.metadata.ModMetadata");
			Object loader = loaderClass.getMethod("getInstance").invoke(null);
			Iterable<?> mods = (Iterable<?>) loaderClass.getMethod("getAllMods").invoke(loader);
			StringBuilder builder = new StringBuilder();

			for (Object mod : mods) {
				Object metadata = containerClass.getMethod("getMetadata").invoke(mod);
				builder.append(metadataClass.getMethod("getId").invoke(metadata)).append(' ');
			}

			return builder.toString().trim();
		} catch (Throwable error) {
			return "unknown: " + error;
		}
	}

	private static String classpathEntries() {
		StringBuilder builder = new StringBuilder();

		for (String entry : System.getProperty("java.class.path", "").split(java.io.File.pathSeparator)) {
			if (entry.contains("yoptie") || entry.contains("build/classes") || entry.contains("build/resources")) {
				builder.append(entry.substring(entry.lastIndexOf('/') + 1)).append(' ');
			}
		}

		return builder.toString().trim();
	}

	private static Method find(Class<?> owner, String name) {
		for (Method method : owner.getDeclaredMethods()) {
			if (method.getName().equals(name)) {
				return method;
			}
		}

		return null;
	}

	private static Method find(Class<?> owner, String name, int parameterCount) {
		for (Method method : owner.getDeclaredMethods()) {
			if (method.getName().equals(name) && method.getParameterCount() == parameterCount) {
				return method;
			}
		}

		return null;
	}

	private static Field findField(Class<?> owner, String name, String typeName) {
		try {
			Field field = owner.getDeclaredField(name);

			if (field.getType().getName().equals(typeName)) {
				return field;
			}
		} catch (NoSuchFieldException ignored) {
		}

		return null;
	}

	private static void notice(String message) {
		System.out.println("::notice title=yoptie-verify::" + message);
	}
}
