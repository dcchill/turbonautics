/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.turbonautics.init;

import net.turbonautics.fluid.JetFuelFluid;
import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

public class TurbonauticsModFluids {
	public static final DeferredRegister<Fluid> REGISTRY = DeferredRegister.create(BuiltInRegistries.FLUID, TurbonauticsMod.MODID);
	public static final DeferredHolder<Fluid, FlowingFluid> JET_FUEL = REGISTRY.register("jet_fuel", () -> new JetFuelFluid.Source());
	public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_JET_FUEL = REGISTRY.register("flowing_jet_fuel", () -> new JetFuelFluid.Flowing());

	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
	public static class FluidsClientSideHandler {
		@SubscribeEvent
		public static void clientSetup(FMLClientSetupEvent event) {
			ItemBlockRenderTypes.setRenderLayer(JET_FUEL.get(), RenderType.translucent());
			ItemBlockRenderTypes.setRenderLayer(FLOWING_JET_FUEL.get(), RenderType.translucent());
		}
	}
}