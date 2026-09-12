package dev.yoptie.client.mixin;

import dev.yoptie.YoptieConfig;
import java.util.Map;
import java.util.Queue;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class ParticleEngineMixin {
	@Shadow
	@Final
	private Map<ParticleRenderType, ParticleGroup<?>> particles;

	@Shadow
	@Final
	private Queue<Particle> particlesToAdd;

	@Inject(method = "add", at = @At("HEAD"), cancellable = true)
	private void yoptie$enforceParticleBudget(Particle particle, CallbackInfo callback) {
		YoptieConfig config = YoptieConfig.get();

		if (!config.enabled || !config.particles.limitTotal) {
			return;
		}

		int budget = config.particles.maxParticles;

		if (budget <= 0) {
			return;
		}

		int total = this.particlesToAdd.size();

		for (ParticleGroup<?> group : this.particles.values()) {
			total += group.size();
		}

		if (total >= budget) {
			callback.cancel();
		}
	}
}
