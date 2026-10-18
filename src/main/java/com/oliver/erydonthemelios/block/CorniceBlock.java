package com.oliver.erydonthemelios.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.Waterloggable;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;

import java.util.EnumMap;
import java.util.Map;

/** A top-edge cornice that can hold one run or two perpendicular runs. */
@SuppressWarnings("deprecation")
public final class CorniceBlock extends Block implements Waterloggable {
    public static final EnumProperty<CorniceShape> SHAPE =
            EnumProperty.of("shape", CorniceShape.class);
    public static final BooleanProperty WATERLOGGED = Properties.WATERLOGGED;

    private static final Map<Size, Map<CorniceShape, VoxelShape>> SHAPES = createShapes();

    private final Size size;

    public CorniceBlock(Settings settings, Size size) {
        super(settings.nonOpaque());
        this.size = size;
        setDefaultState(stateManager.getDefaultState()
                .with(SHAPE, CorniceShape.NORTH)
                .with(WATERLOGGED, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(SHAPE, WATERLOGGED);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos,
                                      ShapeContext context) {
        return SHAPES.get(size).get(state.get(SHAPE));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos,
                                        ShapeContext context) {
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    public boolean canReplace(BlockState state, ItemPlacementContext context) {
        if (!context.getStack().isOf(asItem())) {
            return false;
        }
        CorniceShape shape = state.get(SHAPE);
        return shape.isStraight() && shape.add(edgeFor(context, true)) != null;
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState existing = context.getWorld().getBlockState(context.getBlockPos());
        boolean waterlogged = context.getWorld().getFluidState(context.getBlockPos()).getFluid()
                == Fluids.WATER;

        if (!existing.isOf(this)) {
            return getDefaultState()
                    .with(SHAPE, CorniceShape.of(edgeFor(context, false)))
                    .with(WATERLOGGED, waterlogged);
        }

        CorniceShape combined = existing.get(SHAPE).add(edgeFor(context, true));
        return combined == null ? null : existing.with(SHAPE, combined);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStill(false) : super.getFluidState(state);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                BlockState neighborState, WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        if (state.get(WATERLOGGED)) {
            world.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(SHAPE, state.get(SHAPE).rotate(rotation));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.with(SHAPE, state.get(SHAPE).mirror(mirror));
    }

    private static Direction edgeFor(ItemPlacementContext context, boolean replacingCornice) {
        if (!replacingCornice && context.getSide().getAxis().isHorizontal()) {
            return context.getSide().getOpposite();
        }

        BlockPos pos = context.getBlockPos();
        Vec3d hit = context.getHitPos();
        double x = clamp(hit.x - pos.getX());
        double z = clamp(hit.z - pos.getZ());
        double closest = z;
        Direction edge = Direction.NORTH;
        if (1.0D - x < closest) {
            closest = 1.0D - x;
            edge = Direction.EAST;
        }
        if (1.0D - z < closest) {
            closest = 1.0D - z;
            edge = Direction.SOUTH;
        }
        if (x < closest) {
            edge = Direction.WEST;
        }
        return edge;
    }

    private static double clamp(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static Map<Size, Map<CorniceShape, VoxelShape>> createShapes() {
        Map<Size, Map<CorniceShape, VoxelShape>> bySize = new EnumMap<>(Size.class);
        for (Size size : Size.values()) {
            Map<Direction, VoxelShape> runs = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.Type.HORIZONTAL) {
                runs.put(direction, createRun(size.boxes, direction));
            }

            Map<CorniceShape, VoxelShape> shapes = new EnumMap<>(CorniceShape.class);
            for (CorniceShape shape : CorniceShape.values()) {
                VoxelShape result = runs.get(shape.first);
                if (shape.second != null) {
                    result = VoxelShapes.union(result, runs.get(shape.second));
                }
                shapes.put(shape, result.simplify());
            }
            bySize.put(size, Map.copyOf(shapes));
        }
        return Map.copyOf(bySize);
    }

    private static VoxelShape createRun(double[][] boxes, Direction direction) {
        VoxelShape result = VoxelShapes.empty();
        for (double[] box : boxes) {
            double minX = box[0];
            double minY = box[1];
            double minZ = box[2];
            double maxX = box[3];
            double maxY = box[4];
            double maxZ = box[5];
            VoxelShape part = switch (direction) {
                case EAST -> createCuboidShape(16.0D - maxZ, minY, minX,
                        16.0D - minZ, maxY, maxX);
                case SOUTH -> createCuboidShape(16.0D - maxX, minY, 16.0D - maxZ,
                        16.0D - minX, maxY, 16.0D - minZ);
                case WEST -> createCuboidShape(minZ, minY, 16.0D - maxX,
                        maxZ, maxY, 16.0D - minX);
                default -> createCuboidShape(minX, minY, minZ, maxX, maxY, maxZ);
            };
            result = VoxelShapes.union(result, part);
        }
        return result.simplify();
    }

    public enum Size {
        SMALL(new double[][]{
                {-2.0D, 13.0D, 0.0D, 18.0D, 15.0D, 2.0D},
                {-3.0D, 15.0D, 0.0D, 19.0D, 16.0D, 3.0D},
                {-1.0D, 10.0D, 0.0D, 17.0D, 13.0D, 1.0D}
        }),
        LARGE(new double[][]{
                {-6.0D, 10.0D, 0.0D, 22.0D, 14.0D, 6.0D},
                {-8.0D, 14.0D, 0.0D, 24.0D, 16.0D, 8.0D},
                {-4.0D, 0.0D, 0.0D, 20.0D, 10.0D, 4.0D}
        });

        private final double[][] boxes;

        Size(double[][] boxes) {
            this.boxes = boxes;
        }
    }

    public enum CorniceShape implements StringIdentifiable {
        NORTH("north", Direction.NORTH, null),
        EAST("east", Direction.EAST, null),
        SOUTH("south", Direction.SOUTH, null),
        WEST("west", Direction.WEST, null),
        NORTH_EAST("north_east", Direction.NORTH, Direction.EAST),
        EAST_SOUTH("east_south", Direction.EAST, Direction.SOUTH),
        SOUTH_WEST("south_west", Direction.SOUTH, Direction.WEST),
        WEST_NORTH("west_north", Direction.WEST, Direction.NORTH);

        private final String name;
        private final Direction first;
        private final Direction second;

        CorniceShape(String name, Direction first, Direction second) {
            this.name = name;
            this.first = first;
            this.second = second;
        }

        public boolean isStraight() {
            return second == null;
        }

        public CorniceShape add(Direction direction) {
            if (!isStraight() || direction == first || direction.getAxis() == first.getAxis()) {
                return null;
            }
            return of(first, direction);
        }

        CorniceShape rotate(BlockRotation rotation) {
            return of(rotation.rotate(first), second == null ? null : rotation.rotate(second));
        }

        CorniceShape mirror(BlockMirror mirror) {
            return of(mirror.apply(first), second == null ? null : mirror.apply(second));
        }

        static CorniceShape of(Direction direction) {
            return of(direction, null);
        }

        private static CorniceShape of(Direction first, Direction second) {
            if (second == null || first == second) {
                return switch (first) {
                    case EAST -> EAST;
                    case SOUTH -> SOUTH;
                    case WEST -> WEST;
                    default -> NORTH;
                };
            }
            if (contains(first, second, Direction.NORTH, Direction.EAST)) {
                return NORTH_EAST;
            }
            if (contains(first, second, Direction.EAST, Direction.SOUTH)) {
                return EAST_SOUTH;
            }
            if (contains(first, second, Direction.SOUTH, Direction.WEST)) {
                return SOUTH_WEST;
            }
            if (contains(first, second, Direction.WEST, Direction.NORTH)) {
                return WEST_NORTH;
            }
            throw new IllegalArgumentException("Cornice edges must be adjacent");
        }

        private static boolean contains(Direction first, Direction second,
                                        Direction expectedFirst, Direction expectedSecond) {
            return first == expectedFirst && second == expectedSecond
                    || first == expectedSecond && second == expectedFirst;
        }

        @Override
        public String asString() {
            return name;
        }
    }
}
