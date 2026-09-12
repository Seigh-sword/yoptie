package dev.yoptie;

import static org.junit.jupiter.api.Assertions.assertFalse;

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
	void targetsExist() throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		List<String> problems = new ArrayList<>();

		for (String[] entry : METHODS) {
			Class<?> owner = load(loader, entry[0], problems);

			if (owner == null) {
				continue;
			}

			if (find(owner, entry[1], Integer.parseInt(entry[2])) == null) {
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

			if (findField(owner, entry[1], entry[2]) == null) {
				problems.add(entry[0] + "#" + entry[1] + " of type " + entry[2] + " is missing");
			} else {
				notice("shadow " + entry[0] + "#" + entry[1] + " present");
			}
		}

		for (String entry : HANDLERS) {
			String[] parts = entry.split("#");
			Class<?> owner = load(loader, parts[0], problems);

			if (owner != null) {
				notice("handler " + entry + (find(owner, parts[1]) == null ? " not visible" : " visible"));
			}
		}

		if (!problems.isEmpty()) {
			throw new AssertionError(String.join("; ", problems));
		}
	}

	@Test
	void telemetryMixinIsLive() throws Exception {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft", false, loader);
		Object minecraft = allocate(minecraftClass);
		Method allowsTelemetry = minecraftClass.getMethod("allowsTelemetry");
		allowsTelemetry.setAccessible(true);
		Object result = allowsTelemetry.invoke(minecraft);
		notice("allowsTelemetry returned " + result + " on an uninitialised client");
		assertFalse((Boolean) result, "telemetry should be refused by the mixin");
	}

	private static Object allocate(Class<?> type) throws Exception {
		Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
		Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
		theUnsafe.setAccessible(true);
		Object unsafe = theUnsafe.get(null);
		Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);
		return allocateInstance.invoke(unsafe, type);
	}

	private static Class<?> load(ClassLoader loader, String name, List<String> problems) {
		try {
			return Class.forName(name, false, loader);
		} catch (ClassNotFoundException | LinkageError error) {
			problems.add(name + " could not be loaded: " + error);
			return null;
		}
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
