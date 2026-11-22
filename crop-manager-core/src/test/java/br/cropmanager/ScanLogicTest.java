package br.cropmanager;

import org.junit.Test;
import static org.junit.Assert.*;

public class ScanLogicTest {
    @Test
    public void volumeCalculaAreaECamadas() {
        assertEquals(243, ScanLogic.volume(4, 7));
        assertEquals(81, ScanLogic.volume(4, 2));
        assertEquals(27, ScanLogic.volume(1, 7));
    }

    @Test
    public void offsetMapeiaCamadasCorretamente() {
        int[] below = ScanLogic.offset(0, 1, 1);
        assertArrayEquals(new int[] {-1, -1, -1}, below);

        int[] same = ScanLogic.offset(0, 1, 2);
        assertArrayEquals(new int[] {-1, 0, -1}, same);

        int[] above = ScanLogic.offset(0, 1, 4);
        assertArrayEquals(new int[] {-1, 1, -1}, above);
    }

    @Test
    public void creditAcumulaERespeitaTeto() {
        assertEquals(1.0, ScanLogic.credit(0.5, 2.0, 8), 1e-9);
        assertEquals(8.0, ScanLogic.credit(7.5, 1.0, 8), 1e-9);
    }

    @Test
    public void validSettingsRejeitaLimitesInvalidos() {
        assertTrue(ScanLogic.validSettings(4, 7, 15, 8));
        assertFalse(ScanLogic.validSettings(0, 7, 15, 8));
        assertFalse(ScanLogic.validSettings(9, 7, 15, 8));
        assertFalse(ScanLogic.validSettings(4, 0, 15, 8));
        assertFalse(ScanLogic.validSettings(4, 8, 15, 8));
        assertFalse(ScanLogic.validSettings(4, 7, 16, 8));
    }
}
