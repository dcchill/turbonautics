/*
 * MCreator note: This file will be REGENERATED on each build.
 */
package net.turbonautics.init;

import net.turbonautics.fluid.types.JetFuelFluidType;
import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.fluids.FluidType;

public class TurbonauticsModFluidTypes {
	public static final DeferredRegister<FluidType> REGISTRY = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, TurbonauticsMod.MODID);
	public static final DeferredHolder<FluidType, FluidType> JET_FUEL_TYPE = REGISTRY.register("jet_fuel", () -> new JetFuelFluidType());
}