package net.turbonautics.block.listener;

import net.turbonautics.TurbonauticsMod;
import net.turbonautics.init.TurbonauticsModItems;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

@EventBusSubscriber(modid = TurbonauticsMod.MODID)
public class TooltipListener {
	@SubscribeEvent
	public static void onItemTooltip(ItemTooltipEvent event) {
		ItemStack stack = event.getItemStack();
		String infoKey = getInfoKey(stack.getItem());
		if (infoKey == null) {
			return;
		}

		boolean shiftDown = Screen.hasShiftDown();
		event.getToolTip().add(Component.translatable("info.turbonautics.shift_for_info").withStyle(shiftDown ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
		if (shiftDown) {
			event.getToolTip().add(Component.translatable(infoKey).withStyle(ChatFormatting.GRAY));
		}
	}

	private static String getInfoKey(Item item) {
		if (item == TurbonauticsModItems.BLAZE_AFTERBURNER.get()) {
			return "info.turbonautics.block.blaze_afterburner";
		}
		if (item == TurbonauticsModItems.INTAKE.get()) {
			return "info.turbonautics.block.intake";
		}
		if (item == TurbonauticsModItems.JET_FUEL_BUCKET.get()) {
			return "info.turbonautics.item.bucket_of_jetfuel";
		}
		return null;
	}
}
