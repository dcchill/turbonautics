/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.turbonautics.init;

import net.turbonautics.block.display.BlazeAfterburnerDisplayItem;
import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;

import net.minecraft.world.item.Item;

public class TurbonauticsModItems {
	public static final DeferredRegister.Items REGISTRY = DeferredRegister.createItems(TurbonauticsMod.MODID);
	public static final DeferredItem<Item> BLAZE_AFTERBURNER = REGISTRY.register(TurbonauticsModBlocks.BLAZE_AFTERBURNER.getId().getPath(), () -> new BlazeAfterburnerDisplayItem(TurbonauticsModBlocks.BLAZE_AFTERBURNER.get(), new Item.Properties()));
	// Start of user code block custom items
	// End of user code block custom items
}