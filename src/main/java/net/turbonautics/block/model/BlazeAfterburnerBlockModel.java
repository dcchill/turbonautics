package net.turbonautics.block.model;

import software.bernie.geckolib.model.GeoModel;

import net.turbonautics.block.entity.BlazeAfterburnerTileEntity;

import net.minecraft.resources.ResourceLocation;

public class BlazeAfterburnerBlockModel extends GeoModel<BlazeAfterburnerTileEntity> {
	@Override
	public ResourceLocation getAnimationResource(BlazeAfterburnerTileEntity animatable) {
		return ResourceLocation.parse("turbonautics:animations/afterburner.animation.json");
	}

	@Override
	public ResourceLocation getModelResource(BlazeAfterburnerTileEntity animatable) {
		return ResourceLocation.parse("turbonautics:geo/afterburner.geo.json");
	}

	@Override
	public ResourceLocation getTextureResource(BlazeAfterburnerTileEntity animatable) {
		return animatable.getCurrentTexture();
	}
}
