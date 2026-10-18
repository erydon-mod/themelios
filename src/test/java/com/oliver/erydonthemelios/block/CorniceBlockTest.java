package com.oliver.erydonthemelios.block;

import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.Direction;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class CorniceBlockTest {
    @Test
    void combinesOnlyPerpendicularStraightEdges() {
        assertEquals(CorniceBlock.CorniceShape.NORTH_EAST,
                CorniceBlock.CorniceShape.NORTH.add(Direction.EAST));
        assertEquals(CorniceBlock.CorniceShape.WEST_NORTH,
                CorniceBlock.CorniceShape.NORTH.add(Direction.WEST));
        assertNull(CorniceBlock.CorniceShape.NORTH.add(Direction.NORTH));
        assertNull(CorniceBlock.CorniceShape.NORTH.add(Direction.SOUTH));
        assertNull(CorniceBlock.CorniceShape.NORTH_EAST.add(Direction.SOUTH));
    }

    @Test
    void rotatesStraightAndCornerShapes() {
        assertEquals(CorniceBlock.CorniceShape.EAST,
                CorniceBlock.CorniceShape.NORTH.rotate(BlockRotation.CLOCKWISE_90));
        assertEquals(CorniceBlock.CorniceShape.EAST_SOUTH,
                CorniceBlock.CorniceShape.NORTH_EAST.rotate(BlockRotation.CLOCKWISE_90));
        assertEquals(CorniceBlock.CorniceShape.SOUTH_WEST,
                CorniceBlock.CorniceShape.NORTH_EAST.rotate(BlockRotation.CLOCKWISE_180));
    }

    @Test
    void exposesEightDistinctBlockstateNames() {
        assertEquals(8, CorniceBlock.CorniceShape.values().length);
        assertEquals(8, Arrays.stream(CorniceBlock.CorniceShape.values())
                .map(CorniceBlock.CorniceShape::asString)
                .distinct()
                .count());
    }
}
