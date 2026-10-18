package com.oliver.erydonthemelios.client.model;

import net.minecraft.util.math.Direction;

/** Shared world-aligned 6x6 repeat-tile selection. */
final class SynapheiaTileGrid {
    private SynapheiaTileGrid() {
    }

    static int repeatTileIndex(int x, int y, int z, Direction face) {
        Direction safeFace = face == null ? Direction.UP : face;
        int tileX;
        int tileY;
        switch (safeFace) {
            case DOWN -> {
                tileX = x;
                tileY = -z - 1;
            }
            case UP -> {
                tileX = x;
                tileY = z;
            }
            case NORTH -> {
                tileX = -x - 1;
                tileY = -y;
            }
            case SOUTH -> {
                tileX = x;
                tileY = -y;
            }
            case WEST -> {
                tileX = z;
                tileY = -y;
            }
            case EAST -> {
                tileX = -z - 1;
                tileY = -y;
            }
            default -> {
                tileX = 0;
                tileY = 0;
            }
        }
        return Math.floorMod(tileY, 6) * 6 + Math.floorMod(tileX, 6);
    }
}
