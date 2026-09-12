package dev.yoptie.client.mixin;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.telemetry.TelemetryLogManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TelemetryLogManager.class)
public abstract class TelemetryLogManagerMixin {
	@Inject(method = "open", at = @At("HEAD"), cancellable = true)
	private static void yoptie$skipTelemetryLog(Path directory, CallbackInfoReturnable<CompletableFuture<Optional<TelemetryLogManager>>> callback) {
		callback.setReturnValue(CompletableFuture.completedFuture(Optional.empty()));
	}
}
