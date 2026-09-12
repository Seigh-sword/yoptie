package dev.yoptie.client.mixin;

import dev.yoptie.client.BlockEntityCuller;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class BlockEntityRenderDispatcherMixin {
	@Shadow
	private Vec3 cameraPos;

	@Inject(method = "tryExtractRenderState", at = @At("HEAD"), cancellable = true)
	private void yoptie$cullDistantBlockEntities(BlockEntity blockEntity, float partialTicks, ModelFeatureRenderer.CrumblingOverlay breakProgress, boolean isGloballyRendered, CallbackInfoReturnable<BlockEntityRenderState> callback) {
		if (BlockEntityCuller.shouldCull(blockEntity, this.cameraPos, isGloballyRendered)) {
			callback.setReturnValue(null);
		}
	}
}
