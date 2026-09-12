package dev.yoptie.client.mixin;

import dev.yoptie.YoptieConfig;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Inject(method = "allowsTelemetry", at = @At("HEAD"), cancellable = true)
	private void yoptie$disableTelemetry(CallbackInfoReturnable<Boolean> callback) {
		if (YoptieConfig.get().enabled && YoptieConfig.get().telemetry.disabled) {
			callback.setReturnValue(false);
		}
	}
}
