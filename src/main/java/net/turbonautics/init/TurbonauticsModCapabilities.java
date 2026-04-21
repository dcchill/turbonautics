package net.turbonautics.init;

import net.turbonautics.TurbonauticsMod;
import net.turbonautics.block.entity.BlazeAfterburnerTileEntity;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@EventBusSubscriber(modid = TurbonauticsMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class TurbonauticsModCapabilities {
	@SubscribeEvent
	public static void registerCapabilities(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TurbonauticsModBlockEntities.BLAZE_AFTERBURNER.get(), (blockEntity, side) -> ((BlazeAfterburnerTileEntity) blockEntity).getFluidHandler());
	}
}
