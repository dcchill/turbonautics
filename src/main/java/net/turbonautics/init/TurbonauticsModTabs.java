/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.turbonautics.init;

import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.Registries;

public class TurbonauticsModTabs {
	public static final DeferredRegister<CreativeModeTab> REGISTRY = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, TurbonauticsMod.MODID);
	public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TURBONAUTICS = REGISTRY.register("turbonautics",
			() -> CreativeModeTab.builder().title(Component.translatable("item_group.turbonautics.turbonautics")).icon(() -> new ItemStack(TurbonauticsModBlocks.BLAZE_AFTERBURNER.get())).displayItems((parameters, tabData) -> {
				tabData.accept(TurbonauticsModItems.JET_FUEL_BUCKET.get());
				tabData.accept(TurbonauticsModBlocks.INTAKE.get().asItem());
			}).build());
}