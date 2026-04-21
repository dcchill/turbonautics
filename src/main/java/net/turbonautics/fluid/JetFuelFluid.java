package net.turbonautics.fluid;

import net.turbonautics.init.TurbonauticsModItems;
import net.turbonautics.init.TurbonauticsModFluids;
import net.turbonautics.init.TurbonauticsModFluidTypes;
import net.turbonautics.init.TurbonauticsModBlocks;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleOptions;

public abstract class JetFuelFluid extends BaseFlowingFluid {
	public static final BaseFlowingFluid.Properties PROPERTIES = new BaseFlowingFluid.Properties(() -> TurbonauticsModFluidTypes.JET_FUEL_TYPE.get(), () -> TurbonauticsModFluids.JET_FUEL.get(), () -> TurbonauticsModFluids.FLOWING_JET_FUEL.get())
			.explosionResistance(100f).tickRate(8).bucket(() -> TurbonauticsModItems.JET_FUEL_BUCKET.get()).block(() -> (LiquidBlock) TurbonauticsModBlocks.JET_FUEL.get());

	private JetFuelFluid() {
		super(PROPERTIES);
	}

	@Override
	public ParticleOptions getDripParticle() {
		return ParticleTypes.DRIPPING_HONEY;
	}

	public static class Source extends JetFuelFluid {
		public int getAmount(FluidState state) {
			return 8;
		}

		public boolean isSource(FluidState state) {
			return true;
		}
	}

	public static class Flowing extends JetFuelFluid {
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		public boolean isSource(FluidState state) {
			return false;
		}
	}
}