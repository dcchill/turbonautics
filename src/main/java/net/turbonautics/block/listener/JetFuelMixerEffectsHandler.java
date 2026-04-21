package net.turbonautics.block.listener;

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;

import net.turbonautics.TurbonauticsMod;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = TurbonauticsMod.MODID)
public class JetFuelMixerEffectsHandler {
	private static final ResourceLocation BLAZE_CAKE_ID = ResourceLocation.fromNamespaceAndPath("create", "blaze_cake");
	private static final int SOUND_COOLDOWN_TICKS = 12;
	private static final Map<ResourceKey<Level>, Set<BlockPos>> TRACKED_MIXERS = new ConcurrentHashMap<>();
	private static final Map<GlobalPos, Integer> SOUND_COOLDOWNS = new ConcurrentHashMap<>();

	@SubscribeEvent
	public static void onChunkLoad(ChunkEvent.Load event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk))
			return;
		trackMixersInChunk(level, chunk);
	}

	@SubscribeEvent
	public static void onChunkUnload(ChunkEvent.Unload event) {
		if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk))
			return;
		untrackMixersInChunk(level, chunk);
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		BlockEntity blockEntity = level.getBlockEntity(event.getPos());
		if (blockEntity instanceof MechanicalMixerBlockEntity)
			getTrackedMixers(level).add(event.getPos().immutable());
	}

	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (!(event.getLevel() instanceof ServerLevel level))
			return;
		Set<BlockPos> mixers = TRACKED_MIXERS.get(level.dimension());
		if (mixers != null)
			mixers.remove(event.getPos());
		SOUND_COOLDOWNS.remove(GlobalPos.of(level.dimension(), event.getPos()));
		SOUND_COOLDOWNS.remove(GlobalPos.of(level.dimension(), event.getPos().below()));
	}

	@SubscribeEvent
	public static void onServerTick(ServerTickEvent.Post event) {
		tickCooldowns();
		for (ServerLevel level : event.getServer().getAllLevels()) {
			Set<BlockPos> mixers = TRACKED_MIXERS.get(level.dimension());
			if (mixers == null || mixers.isEmpty())
				continue;
			Iterator<BlockPos> iterator = mixers.iterator();
			while (iterator.hasNext()) {
				BlockPos mixerPos = iterator.next();
				BlockEntity blockEntity = level.getBlockEntity(mixerPos);
				if (!(blockEntity instanceof MechanicalMixerBlockEntity mixer)) {
					iterator.remove();
					continue;
				}
				if (!mixer.running || mixer.processingTicks <= 0)
					continue;
				BlockPos basinPos = mixerPos.below();
				BlockEntity basinBlockEntity = level.getBlockEntity(basinPos);
				if (!(basinBlockEntity instanceof BasinBlockEntity basin))
					continue;
				if (!isJetFuelMixing(basin))
					continue;
				playJetFuelMixingEffects(level, basinPos);
			}
		}
	}

	private static void trackMixersInChunk(ServerLevel level, LevelChunk chunk) {
		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			if (blockEntity instanceof MechanicalMixerBlockEntity)
				getTrackedMixers(level).add(blockEntity.getBlockPos().immutable());
		}
	}

	private static void untrackMixersInChunk(ServerLevel level, LevelChunk chunk) {
		Set<BlockPos> mixers = TRACKED_MIXERS.get(level.dimension());
		if (mixers == null)
			return;
		for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
			if (blockEntity instanceof MechanicalMixerBlockEntity)
				mixers.remove(blockEntity.getBlockPos());
		}
	}

	private static Set<BlockPos> getTrackedMixers(ServerLevel level) {
		return TRACKED_MIXERS.computeIfAbsent(level.dimension(), key -> ConcurrentHashMap.newKeySet());
	}

	private static boolean isJetFuelMixing(BasinBlockEntity basin) {
		if (basin.inputTank == null || basin.inputInventory == null)
			return false;
		if (basin.inputTank.getPrimaryHandler().getFluid().getFluid() != Fluids.WATER)
			return false;
		if (basin.inputTank.getPrimaryHandler().getFluid().getAmount() < 1000)
			return false;
		for (int slot = 0; slot < basin.inputInventory.getSlots(); slot++) {
			if (BLAZE_CAKE_ID.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(basin.inputInventory.getStackInSlot(slot).getItem())))
				return true;
		}
		return false;
	}

	private static void playJetFuelMixingEffects(ServerLevel level, BlockPos basinPos) {
		RandomSource random = level.random;
		double centerX = basinPos.getX() + 0.5;
		double centerY = basinPos.getY() + 0.95;
		double centerZ = basinPos.getZ() + 0.5;
		for (int i = 0; i < 5; i++) {
			double offsetX = (random.nextDouble() - 0.5) * 0.8;
			double offsetZ = (random.nextDouble() - 0.5) * 0.8;
			double velocityX = offsetX * 0.08;
			double velocityY = 0.06 + random.nextDouble() * 0.04;
			double velocityZ = offsetZ * 0.08;
			level.sendParticles(ParticleTypes.CLOUD, centerX + offsetX, centerY, centerZ + offsetZ, 1, velocityX, velocityY, velocityZ, 0.01);
			level.sendParticles(ParticleTypes.SMOKE, centerX + offsetX * 0.75, centerY + 0.05, centerZ + offsetZ * 0.75, 1, velocityX * 0.5, velocityY * 0.8, velocityZ * 0.5, 0.01);
		}
		GlobalPos soundKey = GlobalPos.of(level.dimension(), basinPos);
		if (SOUND_COOLDOWNS.getOrDefault(soundKey, 0) > 0)
			return;
		Vec3 soundPos = Vec3.atCenterOf(basinPos).add(0, 0.35, 0);
		level.playSound(null, soundPos.x, soundPos.y, soundPos.z, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.25f, 0.8f + random.nextFloat() * 0.15f);
		SOUND_COOLDOWNS.put(soundKey, SOUND_COOLDOWN_TICKS);
	}

	private static void tickCooldowns() {
		Iterator<Map.Entry<GlobalPos, Integer>> iterator = SOUND_COOLDOWNS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<GlobalPos, Integer> entry = iterator.next();
			int updated = entry.getValue() - 1;
			if (updated <= 0) {
				iterator.remove();
				continue;
			}
			entry.setValue(updated);
		}
	}
}
