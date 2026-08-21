package me.justbecause.fastpaintings.painting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaintingFootprintTest {

    private static final Direction[] HORIZONTAL_FACINGS = new Direction[]{
            Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
    };

    @ParameterizedTest(name = "{0}x{1} on {2}")
    @CsvSource({
            "1, 1, NORTH", "1, 1, SOUTH", "1, 1, WEST", "1, 1, EAST",
            "2, 1, NORTH", "2, 1, SOUTH", "2, 1, WEST", "2, 1, EAST",
            "1, 2, NORTH", "1, 2, SOUTH", "1, 2, WEST", "1, 2, EAST",
            "2, 2, NORTH", "2, 2, SOUTH", "2, 2, WEST", "2, 2, EAST",
            "4, 3, NORTH", "4, 3, SOUTH", "4, 3, WEST", "4, 3, EAST",
            "4, 4, NORTH", "4, 4, SOUTH", "4, 4, WEST", "4, 4, EAST",
            "16, 16, NORTH", "16, 16, SOUTH", "16, 16, WEST", "16, 16, EAST"
    })
    @DisplayName("Footprint dimensions and cells match expectations")
    void testFootprintCells(int width, int height, Direction facing) {
        BlockPos anchor = new BlockPos(100, 64, -200);
        PaintingFootprint footprint = PaintingFootprint.of(anchor, facing, width, height);

        List<BlockPos> occupied = footprint.occupiedCells();
        List<BlockPos> support = footprint.supportCells();

        assertEquals(width * height, occupied.size(), "Occupied cells count must equal width * height");
        assertEquals(width * height, support.size(), "Support cells count must equal width * height");

        assertTrue(occupied.contains(anchor), "Anchor block must be in occupied cells");

        // Every support cell must be exactly 1 block behind its corresponding occupied cell
        Direction back = facing.getOpposite();
        for (BlockPos occ : occupied) {
            BlockPos sup = occ.relative(back);
            assertTrue(support.contains(sup), "Backing cell " + sup + " must be in support cells");
        }
    }

    @Test
    @DisplayName("1x1 painting occupies only anchor and single backing block")
    void testOneByOne() {
        BlockPos anchor = new BlockPos(10, 64, 10);
        PaintingFootprint footprint = PaintingFootprint.of(anchor, Direction.NORTH, 1, 1);

        assertEquals(List.of(anchor), footprint.occupiedCells());
        assertEquals(List.of(anchor.relative(Direction.SOUTH)), footprint.supportCells());
    }

    @Test
    @DisplayName("2x2 painting odd/even centering matches vanilla")
    void testTwoByTwoCentering() {
        BlockPos anchor = new BlockPos(0, 64, 0);
        // Facing SOUTH: left is EAST (+X), up is +Y
        // Even width (2) shifts 0.5 towards left (+X).
        // Even height (2) shifts 0.5 UP (+Y).
        PaintingFootprint footprint = PaintingFootprint.of(anchor, Direction.SOUTH, 2, 2);

        List<BlockPos> occupied = footprint.occupiedCells();
        assertEquals(4, occupied.size());

        // Facing SOUTH: painting is on the north face of the wall (Z=0, wall at Z=-1).
        // Occupied cells should be at (0,64,0), (1,64,0), (0,65,0), (1,65,0).
        assertTrue(occupied.contains(new BlockPos(0, 64, 0)));
        assertTrue(occupied.contains(new BlockPos(1, 64, 0)));
        assertTrue(occupied.contains(new BlockPos(0, 65, 0)));
        assertTrue(occupied.contains(new BlockPos(1, 65, 0)));
    }

    @Test
    @DisplayName("Offset round-trip recovers anchor and cell positions for all 1..16 dimensions and facings")
    void testOffsetMappingAllSizes() {
        BlockPos anchor = new BlockPos(50, 70, -80);
        for (Direction facing : HORIZONTAL_FACINGS) {
            for (int w = 1; w <= 16; w++) {
                for (int h = 1; h <= 16; h++) {
                    PaintingFootprint footprint = PaintingFootprint.of(anchor, facing, w, h);
                    for (BlockPos cell : footprint.occupiedCells()) {
                        int offsetX = PaintingFootprint.getOffsetX(cell, anchor, facing);
                        int offsetY = PaintingFootprint.getOffsetY(cell, anchor);

                        assertTrue(offsetX >= 0 && offsetX <= 15,
                                "offsetX must be in [0, 15], got: " + offsetX + " for " + w + "x" + h);
                        assertTrue(offsetY >= 0 && offsetY <= 15,
                                "offsetY must be in [0, 15], got: " + offsetY + " for " + w + "x" + h);

                        BlockPos recoveredAnchor = PaintingFootprint.getAnchorPos(cell, facing, offsetX, offsetY);
                        assertEquals(anchor, recoveredAnchor,
                                "Recovered anchor must match original anchor for " + w + "x" + h);

                        BlockPos recoveredCell = PaintingFootprint.getCellPos(anchor, facing, offsetX, offsetY);
                        assertEquals(cell, recoveredCell,
                                "Recovered cell must match original cell for " + w + "x" + h);
                    }
                }
            }
        }
    }
}
