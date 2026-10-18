package com.oliver.erydonthemelios.block;

import net.minecraft.item.ItemPlacementContext;
import net.minecraft.util.math.Direction;

/** Chooses in-place growth without intercepting ordinary adjacent BlockItem placement. */
final class LayerPlacement {
    private LayerPlacement() {
    }

    static boolean canMerge(int layers, ItemPlacementContext context, int growthFaces) {
        return canMerge(layers, context.shouldCancelInteraction(), context.canReplaceExisting(),
                context.getSide(), growthFaces);
    }

    static boolean canMerge(int layers, boolean crouching, boolean clickedBlock,
                            Direction hitFace, int growthFaces) {
        return layers >= 1 && layers < 8 && !crouching && clickedBlock
                && (growthFaces & face(hitFace)) != 0;
    }

    static int layerFaces(boolean top) {
        return face(top ? Direction.DOWN : Direction.UP);
    }

    static int verticalLayerFaces(Direction facing) {
        return face(facing.getOpposite());
    }

    static int horizontalSliceFaces(Direction facing, boolean top) {
        return verticalLayerFaces(facing) | layerFaces(top);
    }

    static int verticalSliceFaces(Direction facing) {
        return switch (facing) {
            case NORTH -> face(Direction.SOUTH) | face(Direction.EAST);
            case EAST -> face(Direction.SOUTH) | face(Direction.WEST);
            case SOUTH -> face(Direction.NORTH) | face(Direction.WEST);
            case WEST -> face(Direction.NORTH) | face(Direction.EAST);
            default -> 0;
        };
    }

    static int postFaces(Direction.Axis axis) {
        return switch (axis) {
            case X -> face(Direction.UP) | face(Direction.DOWN) | face(Direction.NORTH) | face(Direction.SOUTH);
            case Y -> face(Direction.NORTH) | face(Direction.SOUTH) | face(Direction.EAST) | face(Direction.WEST);
            case Z -> face(Direction.UP) | face(Direction.DOWN) | face(Direction.EAST) | face(Direction.WEST);
        };
    }

    private static int face(Direction direction) {
        return 1 << direction.getId();
    }
}
