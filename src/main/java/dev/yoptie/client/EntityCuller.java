package dev.yoptie.client;

import dev.yoptie.Yoptie;
import dev.yoptie.YoptieConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class EntityCuller {
	private static boolean renderDistanceNotice;

	private EntityCuller() {
	}

	public static boolean shouldCull(Entity entity, double camX, double camY, double camZ) {
		YoptieConfig config = YoptieConfig.get();

		if (!config.enabled || !config.entities.distanceCulling) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null) {
			return false;
		}

		if (isProtected(entity, minecraft)) {
			return false;
		}

		double limit = limitFor(entity, config);

		if (limit <= 0.0) {
			return false;
		}

		return entity.distanceToSqr(camX, camY, camZ) > limit * limit;
	}

	public static boolean shouldSkipTick(Entity entity) {
		YoptieConfig config = YoptieConfig.get();

		if (!config.enabled || !config.entities.tickCulling) {
			return false;
		}

		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.player == null || minecraft.level == null) {
			return false;
		}

		if (entity == minecraft.player || isProtected(entity, minecraft)) {
			return false;
		}

		if (entity.isPassenger() || entity.isVehicle()) {
			return false;
		}

		if (entity instanceof Player && !config.entities.tickCullPlayers) {
			return false;
		}

		double limit = limitFor(entity, config);

		if (limit <= 0.0) {
			return false;
		}

		double margin = limit * 1.25;
		return entity.distanceToSqr(minecraft.player) > margin * margin;
	}

	public static void announceRenderDistanceCap(int requested, int applied) {
		if (renderDistanceNotice || requested <= applied) {
			return;
		}

		renderDistanceNotice = true;
		Yoptie.LOGGER.info("Render distance capped at {} chunks, the options ask for {}", applied, requested);
	}

	private static double limitFor(Entity entity, YoptieConfig config) {
		return entity instanceof Player ? config.entities.playerDistance : config.entities.entityDistance;
	}

	private static boolean isProtected(Entity entity, Minecraft minecraft) {
		return entity == minecraft.getCameraEntity()
				|| entity == minecraft.crosshairPickEntity
				|| entity.hasIndirectPassenger(minecraft.player);
	}
}
