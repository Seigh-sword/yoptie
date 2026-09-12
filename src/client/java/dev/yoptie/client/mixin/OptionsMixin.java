package dev.yoptie.client.mixin;

import dev.yoptie.YoptieConfig;
import dev.yoptie.client.EntityCuller;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Options.class)
public abstract class OptionsMixin {
	@Inject(method = "getEffectiveRenderDistance", at = @At("RETURN"), cancellable = true)
	private void yoptie$capRenderDistance(CallbackInfoReturnable<Integer> callback) {
		YoptieConfig config = YoptieConfig.get();
		YoptieConfig.RenderDistance settings = config.renderDistance;

		if (!config.enabled || !settings.capEnabled || settings.maxChunks <= 0) {
			return;
		}

		int requested = callback.getReturnValue();

		if (requested <= settings.maxChunks) {
			return;
		}

		callback.setReturnValue(settings.maxChunks);
		EntityCuller.announceRenderDistanceCap(requested, settings.maxChunks);
	}
}
