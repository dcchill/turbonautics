package net.turbonautics.block.renderer;

import software.bernie.geckolib.renderer.GeoItemRenderer;

import net.turbonautics.block.model.BlazeAfterburnerDisplayModel;
import net.turbonautics.block.display.BlazeAfterburnerDisplayItem;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class BlazeAfterburnerDisplayItemRenderer extends GeoItemRenderer<BlazeAfterburnerDisplayItem> {
	public BlazeAfterburnerDisplayItemRenderer() {
		super(new BlazeAfterburnerDisplayModel());
	}

	@Override
	public RenderType getRenderType(BlazeAfterburnerDisplayItem animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}
}
