package net.turbonautics.block.entity;

import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.GeoBlockEntity;

import dev.ryanhcode.sable.api.block.propeller.BlockEntityPropeller;
import dev.ryanhcode.sable.api.block.propeller.BlockEntitySubLevelPropellerActor;

import net.turbonautics.init.TurbonauticsModBlockEntities;
import net.turbonautics.init.TurbonauticsModBlocks;
import net.turbonautics.init.TurbonauticsModFluids;
import net.turbonautics.block.BlazeAfterburnerBlock;

import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;

import javax.annotation.Nullable;

import java.util.stream.IntStream;

public class BlazeAfterburnerTileEntity extends RandomizableContainerBlockEntity implements GeoBlockEntity, WorldlyContainer, BlockEntityPropeller, BlockEntitySubLevelPropellerActor {
	private static final int FUEL_SLOT = 0;
	private static final int FLUID_PER_BURN = 1000;
	private static final int TANK_CAPACITY = 4000;
	private static final int LAVA_BURN_TIME = 20000;
	private static final int SUPERHEATED_BURN_TIME = 3200;
	private static final double THRUST_PER_SIGNAL_SQUARED = 3.50d;
	private static final double AIRFLOW_PER_SIGNAL = 4.50d;
	private static final double SUPERHEATED_THRUST_MULTIPLIER = 1.75d;
	private static final double SUPERHEATED_AIRFLOW_MULTIPLIER = 1.4d;
	private static final double INTAKE_THRUST_MULTIPLIER = 1.35d;
	private static final double INTAKE_AIRFLOW_MULTIPLIER = 1.2d;
	private static final double EXHAUST_LENGTH = 2.25d;
	private static final double EXHAUST_RADIUS = 0.45d;
	private static final float EXHAUST_DAMAGE = 3.0f;
	private static final ResourceLocation BLAZE_CAKE_ID = ResourceLocation.fromNamespaceAndPath("create", "blaze_cake");
	private static final ResourceLocation JET_FUEL_BUCKET_ID = ResourceLocation.fromNamespaceAndPath("turbonautics", "jet_fuel_bucket");
	private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
	private NonNullList<ItemStack> stacks = NonNullList.<ItemStack>withSize(9, ItemStack.EMPTY);
	private final SidedInvWrapper handler = new SidedInvWrapper(this, null);
	private final FluidTank lavaTank = new FluidTank(TANK_CAPACITY, fluid -> fluid.getFluid() == Fluids.LAVA || fluid.getFluid() == TurbonauticsModFluids.JET_FUEL.get()) {
		@Override
		protected void onContentsChanged() {
			setChanged();
		}
	};
	private boolean wasAnimating;
	private int burnTimeRemaining;
	private HeatState heatState = HeatState.OFF;

	public BlazeAfterburnerTileEntity(BlockPos pos, BlockState state) {
		super(TurbonauticsModBlockEntities.BLAZE_AFTERBURNER.get(), pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, BlazeAfterburnerTileEntity blockEntity) {
		boolean changed = blockEntity.tryAbsorbLavaBucket();
		boolean powered = blockEntity.getRedstoneSignal() > 0;

		if (powered && blockEntity.burnTimeRemaining <= 0) {
			changed |= blockEntity.tryConsumeFuel();
		}

		if (powered && blockEntity.burnTimeRemaining > 0) {
			blockEntity.burnTimeRemaining--;
			blockEntity.setChanged();
		}

		if (blockEntity.burnTimeRemaining <= 0 && blockEntity.heatState != HeatState.OFF) {
			blockEntity.heatState = HeatState.OFF;
			changed = true;
		}

		if (blockEntity.isActive()) {
			blockEntity.applyExhaustEffects(level);
		}

		changed |= blockEntity.updateAnimationState();

		if (changed) {
			blockEntity.setChanged();
		}
	}

	private PlayState animationPredicate(AnimationState<BlazeAfterburnerTileEntity> event) {
		AnimationController<BlazeAfterburnerTileEntity> controller = event.getController();

		if (isActive()) {
			if (!this.wasAnimating) {
				controller.forceAnimationReset();
				this.wasAnimating = true;
			}
			controller.setAnimation(RawAnimation.begin().thenPlay("1").thenLoop("2"));
			return PlayState.CONTINUE;
		}

		this.wasAnimating = false;
		controller.setAnimation(RawAnimation.begin().thenLoop("0"));
		return PlayState.CONTINUE;
	}

	@Override
	public void registerControllers(AnimatableManager.ControllerRegistrar data) {
		data.add(new AnimationController<BlazeAfterburnerTileEntity>(this, "controller", 0, this::animationPredicate).setAnimationSpeedHandler(animatable -> animatable.isActive() ? animatable.getAnimationSpeed() : 1d));
	}

	@Override
	public AnimatableInstanceCache getAnimatableInstanceCache() {
		return this.cache;
	}

	@Override
	public void loadAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.loadAdditional(compound, lookupProvider);
		if (!this.tryLoadLootTable(compound))
			this.stacks = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
		ContainerHelper.loadAllItems(compound, this.stacks, lookupProvider);
		this.lavaTank.readFromNBT(lookupProvider, compound.getCompound("LavaTank"));
		this.burnTimeRemaining = compound.getInt("BurnTimeRemaining");
		this.heatState = HeatState.byName(compound.getString("HeatState"));
	}

	@Override
	public void saveAdditional(CompoundTag compound, HolderLookup.Provider lookupProvider) {
		super.saveAdditional(compound, lookupProvider);
		if (!this.trySaveLootTable(compound)) {
			ContainerHelper.saveAllItems(compound, this.stacks, lookupProvider);
		}
		compound.put("LavaTank", this.lavaTank.writeToNBT(lookupProvider, new CompoundTag()));
		compound.putInt("BurnTimeRemaining", this.burnTimeRemaining);
		compound.putString("HeatState", this.heatState.name);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.Provider lookupProvider) {
		return this.saveWithFullMetadata(lookupProvider);
	}

	@Override
	public int getContainerSize() {
		return stacks.size();
	}

	@Override
	public boolean isEmpty() {
		for (ItemStack itemstack : this.stacks)
			if (!itemstack.isEmpty())
				return false;
		return true;
	}

	@Override
	public Component getDefaultName() {
		return Component.literal("blaze_afterburner");
	}

	@Override
	public int getMaxStackSize() {
		return 64;
	}

	@Override
	public AbstractContainerMenu createMenu(int id, Inventory inventory) {
		return ChestMenu.threeRows(id, inventory);
	}

	@Override
	public Component getDisplayName() {
		return Component.literal("Blaze Afterburner");
	}

	@Override
	protected NonNullList<ItemStack> getItems() {
		return this.stacks;
	}

	@Override
	protected void setItems(NonNullList<ItemStack> stacks) {
		this.stacks = stacks;
	}

	@Override
	public boolean canPlaceItem(int index, ItemStack stack) {
		return index == FUEL_SLOT && (stack.is(Items.LAVA_BUCKET) || isJetFuelBucket(stack) || stack.getBurnTime(null) > 0);
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		return new int[]{FUEL_SLOT};
	}

	@Override
	public boolean canPlaceItemThroughFace(int index, ItemStack stack, @Nullable Direction direction) {
		return this.canPlaceItem(index, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int index, ItemStack stack, Direction direction) {
		return true;
	}

	public SidedInvWrapper getItemHandler() {
		return handler;
	}

	public FluidTank getFluidHandler() {
		return this.lavaTank;
	}

	@Override
	public Direction getBlockDirection() {
		return getBlockState().getValue(BlazeAfterburnerBlock.FACING);
	}

	@Override
	public double getAirflow() {
		return isActive() ? getRedstoneSignal() * AIRFLOW_PER_SIGNAL * getHeatAirflowMultiplier() * getIntakeAirflowMultiplier() : 0;
	}

	@Override
	public double getThrust() {
		int signal = getRedstoneSignal();
		return isActive() ? signal * signal * THRUST_PER_SIGNAL_SQUARED * getHeatThrustMultiplier() * getIntakeThrustMultiplier() : 0;
	}

	@Override
	public boolean isActive() {
		return this.level != null && this.burnTimeRemaining > 0 && getRedstoneSignal() > 0;
	}

	@Override
	public BlockEntityPropeller getPropeller() {
		return this;
	}

	private int getRedstoneSignal() {
		return this.level == null ? 0 : this.level.getBestNeighborSignal(this.worldPosition);
	}

	private double getSignalScale() {
		return getRedstoneSignal() / 15d;
	}

	private double getAnimationSpeed() {
		double scale = getSignalScale();
		return 0.75d + (scale * scale) * 2.25d;
	}

	public boolean tryFuelFromInteraction(Player player, InteractionHand hand, ItemStack stack) {
		if (this.level == null || this.level.isClientSide || stack.isEmpty()) {
			return false;
		}

		if (stack.is(Items.LAVA_BUCKET)) {
			if (this.lavaTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) < 1000) {
				return false;
			}

			this.lavaTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
			this.burnTimeRemaining = Math.max(this.burnTimeRemaining, LAVA_BURN_TIME);
			this.heatState = HeatState.NORMAL;
			if (!player.getAbilities().instabuild) {
				player.setItemInHand(hand, new ItemStack(Items.BUCKET));
			}
			syncState();
			return true;
		}

		if (isBlazeCake(stack)) {
			if (!player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			this.burnTimeRemaining += SUPERHEATED_BURN_TIME;
			this.heatState = HeatState.SUPERHEATED;
			syncState();
			return true;
		}

		if (isJetFuelBucket(stack)) {
			if (!player.getAbilities().instabuild) {
				player.setItemInHand(hand, new ItemStack(Items.BUCKET));
			}
			this.burnTimeRemaining += SUPERHEATED_BURN_TIME;
			this.heatState = HeatState.SUPERHEATED;
			syncState();
			return true;
		}

		int burnTime = stack.getBurnTime(null);
		if (burnTime <= 0) {
			return false;
		}

		if (!player.getAbilities().instabuild) {
			stack.shrink(1);
		}
		this.burnTimeRemaining += burnTime;
		if (this.heatState == HeatState.OFF) {
			this.heatState = HeatState.NORMAL;
		}
		syncState();
		return true;
	}

	public ResourceLocation getCurrentTexture() {
		return ResourceLocation.fromNamespaceAndPath("turbonautics", "textures/block/" + this.heatState.texture + ".png");
	}

	private void applyExhaustEffects(Level level) {
		Vec3 direction = Vec3.atLowerCornerOf(getBlockDirection().getNormal()).normalize();
		Vec3 origin = Vec3.atCenterOf(this.worldPosition).add(direction.scale(0.7));
		double signalScale = getSignalScale();

		if (level instanceof ServerLevel serverLevel) {
			double speed = 0.15d + signalScale * 0.45d;
			Vec3 velocity = direction.scale(speed);
			serverLevel.sendParticles(ParticleTypes.SMOKE, origin.x, origin.y, origin.z, 6, velocity.x, velocity.y, velocity.z, 0.02);
			serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, origin.x, origin.y, origin.z, 2, velocity.x, velocity.y, velocity.z, 0.01);
		}

		Vec3 end = origin.add(direction.scale(EXHAUST_LENGTH));
		AABB exhaustBox = new AABB(origin, end).inflate(EXHAUST_RADIUS);
		for (Entity entity : level.getEntities((Entity) null, exhaustBox, entity -> entity.isAlive() && !entity.fireImmune())) {
			entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 60));
			entity.hurt(level.damageSources().inFire(), EXHAUST_DAMAGE);
		}
	}

	private boolean tryConsumeFuel() {
		if (this.lavaTank.getFluidAmount() >= FLUID_PER_BURN) {
			FluidStack storedFluid = this.lavaTank.getFluid();
			this.lavaTank.drain(FLUID_PER_BURN, IFluidHandler.FluidAction.EXECUTE);
			if (storedFluid.getFluid() == TurbonauticsModFluids.JET_FUEL.get()) {
				this.burnTimeRemaining = SUPERHEATED_BURN_TIME;
				this.heatState = HeatState.SUPERHEATED;
			} else {
				this.burnTimeRemaining = LAVA_BURN_TIME;
				this.heatState = HeatState.NORMAL;
			}
			return true;
		}

		ItemStack stack = this.stacks.get(FUEL_SLOT);
		if (isJetFuelBucket(stack)) {
			this.stacks.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
			this.burnTimeRemaining = SUPERHEATED_BURN_TIME;
			this.heatState = HeatState.SUPERHEATED;
			return true;
		}

		int burnTime = stack.getBurnTime(null);
		if (burnTime <= 0) {
			return false;
		}

		stack.shrink(1);
		if (stack.isEmpty()) {
			this.stacks.set(FUEL_SLOT, ItemStack.EMPTY);
		}
		this.burnTimeRemaining = burnTime;
		this.heatState = HeatState.NORMAL;
		return true;
	}

	private boolean tryAbsorbLavaBucket() {
		ItemStack stack = this.stacks.get(FUEL_SLOT);
		if (!stack.is(Items.LAVA_BUCKET) || this.lavaTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) < 1000) {
			return false;
		}

		this.lavaTank.fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.EXECUTE);
		this.stacks.set(FUEL_SLOT, new ItemStack(Items.BUCKET));
		return true;
	}

	private boolean updateAnimationState() {
		if (this.level == null) {
			return false;
		}

		int animationValue = isActive() ? 1 : 0;
		BlockState state = getBlockState();
		if (state.getValue(BlazeAfterburnerBlock.ANIMATION) == animationValue) {
			return false;
		}

		this.level.setBlock(this.worldPosition, state.setValue(BlazeAfterburnerBlock.ANIMATION, animationValue), 3);
		return true;
	}

	private boolean isBlazeCake(ItemStack stack) {
		Item blazeCake = BuiltInRegistries.ITEM.get(BLAZE_CAKE_ID);
		return blazeCake != Items.AIR && stack.is(blazeCake);
	}

	private boolean isJetFuelBucket(ItemStack stack) {
		Item jetFuelBucket = BuiltInRegistries.ITEM.get(JET_FUEL_BUCKET_ID);
		return jetFuelBucket != Items.AIR && stack.is(jetFuelBucket);
	}

	private double getHeatThrustMultiplier() {
		return this.heatState == HeatState.SUPERHEATED ? SUPERHEATED_THRUST_MULTIPLIER : 1d;
	}

	private double getHeatAirflowMultiplier() {
		return this.heatState == HeatState.SUPERHEATED ? SUPERHEATED_AIRFLOW_MULTIPLIER : 1d;
	}

	private double getIntakeThrustMultiplier() {
		return hasIntakeOnInletSide() ? INTAKE_THRUST_MULTIPLIER : 1d;
	}

	private double getIntakeAirflowMultiplier() {
		return hasIntakeOnInletSide() ? INTAKE_AIRFLOW_MULTIPLIER : 1d;
	}

	private boolean hasIntakeOnInletSide() {
		if (this.level == null) {
			return false;
		}

		BlockPos intakePos = this.worldPosition.relative(getBlockDirection().getOpposite());
		return this.level.getBlockState(intakePos).is(TurbonauticsModBlocks.INTAKE.get());
	}

	private void syncState() {
		setChanged();
		if (this.level != null) {
			BlockState state = getBlockState();
			this.level.sendBlockUpdated(this.worldPosition, state, state, 3);
		}
	}

	private enum HeatState {
		OFF("off", "afterburner_off"),
		NORMAL("normal", "afterburner"),
		SUPERHEATED("superheated", "super_afterburner");

		private final String name;
		private final String texture;

		HeatState(String name, String texture) {
			this.name = name;
			this.texture = texture;
		}

		private static HeatState byName(String name) {
			for (HeatState state : values()) {
				if (state.name.equals(name)) {
					return state;
				}
			}
			return OFF;
		}
	}
}
