package dev.yoptie.client.mixin;

import dev.yoptie.client.EntityCuller;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
	private void yoptie$cullDistantEntities(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> callback) {
		if (EntityCuller.shouldCull(entity, camX, camY, camZ)) {
			callback.setReturnValue(false);
		}
	}
}
