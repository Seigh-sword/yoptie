package dev.yoptie.client;

import dev.yoptie.YoptieConfig;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ConduitBlockEntity;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.entity.TheEndPortalBlockEntity;
import net.minecraft.world.phys.Vec3;

public final class BlockEntityCuller {
	private BlockEntityCuller() {
	}

	public static boolean shouldCull(BlockEntity blockEntity, Vec3 cameraPos, boolean isGloballyRendered) {
		YoptieConfig config = YoptieConfig.get();

		if (!config.enabled || !config.entities.blockEntityCulling || isGloballyRendered || cameraPos == null) {
			return false;
		}

		if (isLongRange(blockEntity)) {
			return false;
		}

		double limit = config.entities.blockEntityDistance;

		if (limit <= 0.0) {
			return false;
		}

		return blockEntity.getBlockPos().distToCenterSqr(cameraPos.x(), cameraPos.y(), cameraPos.z()) > limit * limit;
	}

	private static boolean isLongRange(BlockEntity blockEntity) {
		return blockEntity instanceof BeaconBlockEntity
				|| blockEntity instanceof ConduitBlockEntity
				|| blockEntity instanceof TheEndPortalBlockEntity
				|| blockEntity instanceof TheEndGatewayBlockEntity;
	}
}
