package net.turbonautics.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.turbonautics.block.display.BlazeAfterburnerDisplayItem;

import net.minecraft.resources.ResourceLocation;

public class BlazeAfterburnerDisplayModel extends GeoModel<BlazeAfterburnerDisplayItem> {
	@Override
	public ResourceLocation getAnimationResource(BlazeAfterburnerDisplayItem animatable) {
		return ResourceLocation.parse("turbonautics:animations/afterburner.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BlazeAfterburnerDisplayItem animatable) {
		return ResourceLocation.parse("turbonautics:geo/afterburner.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BlazeAfterburnerDisplayItem entity) {
		return ResourceLocation.parse("turbonautics:textures/block/afterburner.png");
	}
}