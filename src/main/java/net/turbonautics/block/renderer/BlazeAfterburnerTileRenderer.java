package net.turbonautics.block.renderer;

import software.bernie.geckolib.renderer.GeoBlockRenderer;

import net.turbonautics.block.model.BlazeAfterburnerBlockModel;
import net.turbonautics.block.entity.BlazeAfterburnerTileEntity;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class BlazeAfterburnerTileRenderer extends GeoBlockRenderer<BlazeAfterburnerTileEntity> {
	public BlazeAfterburnerTileRenderer() {
		super(new BlazeAfterburnerBlockModel());
	}

	@Override
	public RenderType getRenderType(BlazeAfterburnerTileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
		return RenderType.entityTranslucent(getTextureLocation(animatable));
	}

	@Override
	protected void rotateBlock(Direction facing, PoseStack poseStack) {
		switch (facing) {
			case UP -> {
			}
			case DOWN -> {
				poseStack.translate(0, 1, 0);
				poseStack.mulPose(Axis.XP.rotationDegrees(180));
			}
			case NORTH -> {
				poseStack.translate(0, 0.5, 0.5);
				poseStack.mulPose(Axis.XP.rotationDegrees(90));
				poseStack.mulPose(Axis.ZP.rotationDegrees(180));
			}
			case SOUTH -> {
				poseStack.translate(0, 0.5, -0.5);
			}
			case EAST -> {
				poseStack.translate(-0.5, 0.5, 0);
				poseStack.mulPose(Axis.ZN.rotationDegrees(90));
				poseStack.mulPose(Axis.YP.rotationDegrees(90));
			}
			case WEST -> {
				poseStack.translate(0.5, 0.5, 0);
				poseStack.mulPose(Axis.ZP.rotationDegrees(90));
				poseStack.mulPose(Axis.YP.rotationDegrees(-90));
			}
		}
	}
}
