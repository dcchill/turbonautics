/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.turbonautics.init;

import net.turbonautics.item.JetFuelItem;
import net.turbonautics.block.display.BlazeAfterburnerDisplayItem;
import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;

public class TurbonauticsModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(TurbonauticsMod.MODID);
	public static final DeferredItem<Item> BLAZE_AFTERBURNER = REGISTRY.register(TurbonauticsModBlocks.BLAZE_AFTERBURNER.getId().getPath(), () -> new BlazeAfterburnerDisplayItem(TurbonauticsModBlocks.BLAZE_AFTERBURNER.get(), new Item.Properties()));
	public static final DeferredItem<Item> JET_FUEL_BUCKET = REGISTRY.register("jet_fuel_bucket", JetFuelItem::new);
	public static final DeferredItem<Item> INTAKE = block(TurbonauticsModBlocks.INTAKE);

	// Start of user code block custom items
	// End of user code block custom items
	private static DeferredItem<Item> block(DeferredHolder<Block, Block> block) {
		return REGISTRY.register(block.getId().getPath(), () -> new BlockItem(block.get(), new Item.Properties()));
	}
}