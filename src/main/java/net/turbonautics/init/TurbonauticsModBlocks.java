/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.turbonautics.init;

import net.turbonautics.block.JetFuelBlock;
import net.turbonautics.block.IntakeBlock;
import net.turbonautics.block.BlazeAfterburnerBlock;
import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;

import net.minecraft.world.level.block.Block;

public class TurbonauticsModBlocks {
	public static final DeferredRegister.Blocks REGISTRY = DeferredRegister.createBlocks(TurbonauticsMod.MODID);
	public static final DeferredBlock<Block> BLAZE_AFTERBURNER = REGISTRY.register("blaze_afterburner", BlazeAfterburnerBlock::new);
	public static final DeferredBlock<Block> JET_FUEL = REGISTRY.register("jet_fuel", JetFuelBlock::new);
	public static final DeferredBlock<Block> INTAKE = REGISTRY.register("intake", IntakeBlock::new);
	// Start of user code block custom blocks
	// End of user code block custom blocks
}