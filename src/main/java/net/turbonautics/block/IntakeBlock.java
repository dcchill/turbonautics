package net.turbonautics.block;

import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

public class IntakeBlock extends Block {
	public static final DirectionProperty FACING = DirectionalBlock.FACING;

	public IntakeBlock() {
		super(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(2f, 10f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
		this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
	}

	@Override
	public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
		return true;
	}

	@Override
	public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
		return 0;
	}

	@Override
	public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			default -> Shapes.or(box(0, 0, 0, 16, 16, 8), box(0, 12, 8, 16, 16, 16), box(0.01, -0.11149, 6.59345, 15.99, 12.86851, 11.57345));
			case NORTH -> Shapes.or(box(0, 0, 8, 16, 16, 16), box(0, 12, 0, 16, 16, 8), box(0.01, -0.11149, 4.42655, 15.99, 12.86851, 9.40655));
			case EAST -> Shapes.or(box(0, 0, 0, 8, 16, 16), box(8, 12, 0, 16, 16, 16), box(6.59345, -0.11149, 0.01, 11.57345, 12.86851, 15.99));
			case WEST -> Shapes.or(box(8, 0, 0, 16, 16, 16), box(0, 12, 0, 8, 16, 16), box(4.42655, -0.11149, 0.01, 9.40655, 12.86851, 15.99));
			case UP -> Shapes.or(box(0, 0, 0, 16, 8, 16), box(0, 8, 12, 16, 16, 16), box(0.01, 6.59345, -0.11149, 15.99, 11.57345, 12.86851));
			case DOWN -> Shapes.or(box(0, 8, 0, 16, 16, 16), box(0, 0, 0, 16, 8, 4), box(0.01, 4.42655, 3.13149, 15.99, 9.40655, 16.11149));
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(FACING);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return super.getStateForPlacement(context).setValue(FACING, context.getClickedFace());
	}

	public BlockState rotate(BlockState state, Rotation rot) {
		return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
	}

	public BlockState mirror(BlockState state, Mirror mirrorIn) {
		return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
	}
}