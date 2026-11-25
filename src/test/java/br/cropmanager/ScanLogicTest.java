package br.cropmanager;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class ScanLogicTest {
    @Test public void visitsEveryPositionExactlyOnceForEverySupportedArea() {
        for (int radius = 1; radius <= 8; radius++) {
            for (int layers = 1; layers <= 7; layers++) {
                Set<String> seen = new HashSet<>();
                int count = ScanLogic.volume(radius, layers);
                for (int i = 0; i < count; i++) {
                    int[] p = ScanLogic.offset(i, radius, layers);
                    assertTrue(Math.abs(p[0]) <= radius);
                    assertTrue(Math.abs(p[2]) <= radius);
                    assertTrue((layers & (1 << (p[1] + 1))) != 0);
                    assertTrue(seen.add(p[0] + "," + p[1] + "," + p[2]));
                }
                assertEquals(count, seen.size());
            }
        }
    }

    @Test public void baseSpeedAndOverclockedSpeedAreBounded() {
        assertEquals(10, operations(2, 20));
        assertEquals(40, operations(0.5, 20));
        assertEquals(160, operations(1e-40, 20));
        assertEquals(8, ScanLogic.credit(100, 2, 8), 0);
    }

    private int operations(double interval, int ticks) {
        double credit = 0;
        int count = 0;
        for (int i = 0; i < ticks; i++) {
            credit = ScanLogic.credit(credit, interval, 8);
            while (credit >= 1) { credit--; count++; }
        }
        return count;
    }

    @Test public void rejectsInvalidOrEmptySettings() {
        assertFalse(ScanLogic.validSettings(0, 7, 15, 8));
        assertFalse(ScanLogic.validSettings(9, 7, 15, 8));
        assertFalse(ScanLogic.validSettings(4, 0, 15, 8));
        assertFalse(ScanLogic.validSettings(4, 8, 15, 8));
        assertFalse(ScanLogic.validSettings(4, 7, 16, 8));
        assertTrue(ScanLogic.validSettings(4, 1, 0, 8));
    }

    @Test public void offsetConstantsAndValidationEnforceBounds() {
        assertEquals(16, ScanLogic.MAX_HORIZONTAL_OFFSET);
        assertEquals(5, ScanLogic.MAX_VERTICAL_OFFSET);
        assertTrue(ScanLogic.validOffsets(0, 0, 0));
        assertTrue(ScanLogic.validOffsets(16, 5, -16));
        assertTrue(ScanLogic.validOffsets(-16, -5, 16));
        assertFalse(ScanLogic.validOffsets(17, 0, 0));
        assertFalse(ScanLogic.validOffsets(-17, 0, 0));
        assertFalse(ScanLogic.validOffsets(0, 6, 0));
        assertFalse(ScanLogic.validOffsets(0, -6, 0));
        assertFalse(ScanLogic.validOffsets(0, 0, 17));
        assertFalse(ScanLogic.validOffsets(0, 0, -17));
    }

    @Test public void offsetWithShiftTranslatesEveryScannedPosition() {
        int radius = 3;
        int layers = 5;
        int offsetX = -4;
        int offsetY = 2;
        int offsetZ = 6;
        int count = ScanLogic.volume(radius, layers);
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < count; i++) {
            int[] base = ScanLogic.offset(i, radius, layers);
            int[] shifted = ScanLogic.offset(i, radius, layers, offsetX, offsetY, offsetZ);
            assertArrayEquals(new int[] {base[0] + offsetX, base[1] + offsetY, base[2] + offsetZ}, shifted);
            assertTrue(seen.add(shifted[0] + "," + shifted[1] + "," + shifted[2]));
        }
        assertEquals(count, seen.size());
    }
}
