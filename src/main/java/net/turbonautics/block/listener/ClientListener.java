package net.turbonautics.block.listener;

import net.turbonautics.init.TurbonauticsModBlockEntities;
import net.turbonautics.block.renderer.BlazeAfterburnerTileRenderer;
import net.turbonautics.block.entity.BlazeAfterburnerTileEntity;
import net.turbonautics.TurbonauticsMod;

import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.api.distmarker.Dist;

import net.minecraft.world.level.block.entity.BlockEntityType;

@EventBusSubscriber(modid = TurbonauticsMod.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientListener {
	@OnlyIn(Dist.CLIENT)
	@SubscribeEvent
	public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
		event.registerBlockEntityRenderer((BlockEntityType<BlazeAfterburnerTileEntity>) TurbonauticsModBlockEntities.BLAZE_AFTERBURNER.get(), context -> new BlazeAfterburnerTileRenderer());
	}
}