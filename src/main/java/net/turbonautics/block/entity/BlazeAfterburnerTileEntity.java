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
import net.turbonautics.block.IntakeBlock;

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
	private static final double THRUST_PER_SIGNAL_SQUARED = 2.00d;
	private static final double AIRFLOW_PER_SIGNAL = 2.50d;
	private static final double SUPERHEATED_THRUST_MULTIPLIER = 1.75d;
	private static final double SUPERHEATED_AIRFLOW_MULTIPLIER = 1.4d;
	private static final double INTAKE_THRUST_MULTIPLIER = 2.25d;
	private static final double INTAKE_AIRFLOW_MULTIPLIER = 2.0d;
	private static final double EXHAUST_LENGTH = 2.25d;
	private static final double EXHAUST_RADIUS = 0.45d;
	private static final double INTAKE_EFFECT_MIN_SPEED = 0.12d;
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
			emitExhaustParticles(serverLevel, origin, direction, signalScale);
			emitIntakeParticles(serverLevel);
		}

		Vec3 end = origin.add(direction.scale(EXHAUST_LENGTH));
		AABB exhaustBox = new AABB(origin, end).inflate(EXHAUST_RADIUS);
		for (Entity entity : level.getEntities((Entity) null, exhaustBox, entity -> entity.isAlive() && !entity.fireImmune())) {
			entity.setRemainingFireTicks(Math.max(entity.getRemainingFireTicks(), 60));
			entity.hurt(level.damageSources().inFire(), EXHAUST_DAMAGE);
		}
	}

	private void emitExhaustParticles(ServerLevel serverLevel, Vec3 origin, Vec3 direction, double signalScale) {
		Vec3 right = direction.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1.0E-4d) {
			right = direction.cross(new Vec3(1, 0, 0));
		}
		right = right.normalize();
		Vec3 up = right.cross(direction).normalize();

		double streamRadius = 0.07d + signalScale * 0.03d;
		double smokeSpeed = 0.20d + signalScale * 0.45d;
		double cozySmokeSpeed = smokeSpeed * 0.85d;

		for (int i = 0; i < 8; i++) {
			spawnStreamParticle(serverLevel, ParticleTypes.SMOKE, origin, direction, right, up, streamRadius, smokeSpeed);
		}
		for (int i = 0; i < 3; i++) {
			spawnStreamParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, origin, direction, right, up, streamRadius * 0.75d, cozySmokeSpeed);
		}
	}

	private void spawnStreamParticle(ServerLevel serverLevel, net.minecraft.core.particles.ParticleOptions particleType, Vec3 origin, Vec3 direction, Vec3 right, Vec3 up, double radius, double speed) {
		double lateralOffset = (serverLevel.random.nextDouble() - 0.5d) * radius;
		double verticalOffset = (serverLevel.random.nextDouble() - 0.5d) * radius;
		Vec3 spawnPos = origin.add(right.scale(lateralOffset)).add(up.scale(verticalOffset));
		Vec3 velocity = direction.scale(speed)
			.add(right.scale(lateralOffset * 0.08d))
			.add(up.scale(verticalOffset * 0.08d));
		serverLevel.sendParticles(particleType, spawnPos.x, spawnPos.y, spawnPos.z, 0, velocity.x, velocity.y, velocity.z, 1.0d);
	}

	private void emitIntakeParticles(ServerLevel serverLevel) {
		BlockPos intakePos = getIntakePos();
		BlockState intakeState = serverLevel.getBlockState(intakePos);
		if (!intakeState.is(TurbonauticsModBlocks.INTAKE.get())) {
			return;
		}

		Vec3 craftVelocity = getSubLevelVelocity(intakePos);
		double speed = craftVelocity.length();
		if (speed < INTAKE_EFFECT_MIN_SPEED) {
			return;
		}

		Vec3 intakeDirection = Vec3.atLowerCornerOf(intakeState.getValue(IntakeBlock.FACING).getNormal()).normalize();
		Vec3 inwardDirection = intakeDirection.scale(-1.0d);
		Vec3 right = intakeDirection.cross(new Vec3(0, 1, 0));
		if (right.lengthSqr() < 1.0E-4d) {
			right = intakeDirection.cross(new Vec3(1, 0, 0));
		}
		right = right.normalize();
		Vec3 up = right.cross(intakeDirection).normalize();
		Vec3 mouthCenter = Vec3.atCenterOf(intakePos).add(intakeDirection.scale(0.72d));

		double streamRadius = 0.09d;
		double intakeSpeed = Math.min(0.34d, 0.06d + speed * 0.025d);
		for (int i = 0; i < 3; i++) {
			spawnStreamParticle(serverLevel, ParticleTypes.CLOUD, mouthCenter, inwardDirection, right, up, streamRadius, intakeSpeed);
		}
		for (int i = 0; i < 1; i++) {
			spawnStreamParticle(serverLevel, ParticleTypes.POOF, mouthCenter, inwardDirection, right, up, streamRadius * 0.55d, intakeSpeed * 0.55d);
		}
	}

	private Vec3 getSubLevelVelocity(BlockPos samplePos) {
		if (this.level == null) {
			return Vec3.ZERO;
		}

		try {
			Class<?> sableClass = Class.forName("dev.ryanhcode.sable.Sable");
			Object helper = sableClass.getField("HELPER").get(null);
			Object subLevel = helper.getClass().getMethod("getContaining", Level.class, net.minecraft.core.Vec3i.class).invoke(helper, this.level, samplePos);
			if (subLevel == null) {
				return Vec3.ZERO;
			}

			Vec3 center = Vec3.atCenterOf(samplePos);
			Class<?> vectorClass = Class.forName("org.joml.Vector3d");
			Object localPos = vectorClass.getConstructor(double.class, double.class, double.class).newInstance(center.x, center.y, center.z);
			Object currentPose = subLevel.getClass().getMethod("logicalPose").invoke(subLevel);
			Object previousPose = subLevel.getClass().getMethod("lastPose").invoke(subLevel);
			Object currentPos = currentPose.getClass().getMethod("transformPosition", Class.forName("org.joml.Vector3dc"), vectorClass)
				.invoke(currentPose, localPos, vectorClass.getConstructor().newInstance());
			Object previousPos = previousPose.getClass().getMethod("transformPosition", Class.forName("org.joml.Vector3dc"), vectorClass)
				.invoke(previousPose, localPos, vectorClass.getConstructor().newInstance());
			double currentX = ((Number) vectorClass.getMethod("x").invoke(currentPos)).doubleValue();
			double currentY = ((Number) vectorClass.getMethod("y").invoke(currentPos)).doubleValue();
			double currentZ = ((Number) vectorClass.getMethod("z").invoke(currentPos)).doubleValue();
			double previousX = ((Number) vectorClass.getMethod("x").invoke(previousPos)).doubleValue();
			double previousY = ((Number) vectorClass.getMethod("y").invoke(previousPos)).doubleValue();
			double previousZ = ((Number) vectorClass.getMethod("z").invoke(previousPos)).doubleValue();
			return new Vec3((currentX - previousX) * 20.0d, (currentY - previousY) * 20.0d, (currentZ - previousZ) * 20.0d);
		} catch (ReflectiveOperationException | LinkageError ignored) {
			return Vec3.ZERO;
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

		BlockPos intakePos = getIntakePos();
		return this.level.getBlockState(intakePos).is(TurbonauticsModBlocks.INTAKE.get());
	}

	private BlockPos getIntakePos() {
		return this.worldPosition.relative(getBlockDirection().getOpposite());
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
