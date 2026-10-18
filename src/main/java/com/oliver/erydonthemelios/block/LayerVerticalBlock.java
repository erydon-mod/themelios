package com.oliver.erydonthemelios.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

@SuppressWarnings("deprecation")
public class LayerVerticalBlock extends Block implements Waterloggable {
    public static final IntProperty LAYERS = Properties.LAYERS;
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;
    public static final DirectionProperty FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape[][] SHAPES = new VoxelShape[4][8];

    static {
        for (int layers = 1; layers <= 8; layers++) {
            int depth = layers * 2;
            SHAPES[index(Direction.NORTH)][layers - 1] = Block.createCuboidShape(0, 0, 0, 16, 16, depth);
            SHAPES[index(Direction.SOUTH)][layers - 1] = Block.createCuboidShape(0, 0, 16 - depth, 16, 16, 16);
            SHAPES[index(Direction.WEST)][layers - 1] = Block.createCuboidShape(0, 0, 0, depth, 16, 16);
            SHAPES[index(Direction.EAST)][layers - 1] = Block.createCuboidShape(16 - depth, 0, 0, 16, 16, 16);
        }
    }

    public LayerVerticalBlock(Settings settings) {
        super(settings.nonOpaque());
        setDefaultState(getDefaultState()
                .with(LAYERS, 1)
                .with(WATERLOGGED, false)
                .with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, WATERLOGGED, FACING);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[index(state.get(FACING))][state.get(LAYERS) - 1];
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        if (context.getStack().isOf(asItem())) {
            return LayerPlacement.canMerge(state.get(LAYERS), context,
                    LayerPlacement.verticalLayerFaces(state.get(FACING)));
        }
        return false;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        BlockState existing = context.getWorld().getBlockState(pos);
        if (existing.getBlock() == this) {
            if (!canReplace(existing, context)) {
                return null;
            }
            return existing.with(LAYERS, existing.get(LAYERS) + 1);
        }

        Direction side = context.getSide();
        PlayerEntity player = context.getPlayer();
        Direction facing = side.getAxis().isHorizontal()
                ? side.getOpposite()
                : player == null ? Direction.NORTH : player.getHorizontalFacing();
        FluidState fluid = context.getWorld().getFluidState(pos);

        return getDefaultState()
                .with(FACING, facing)
                .with(LAYERS, 1)
                .with(WATERLOGGED, fluid.getFluid() == Fluids.WATER);
    }


    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    private static int index(Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case WEST -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }
}
