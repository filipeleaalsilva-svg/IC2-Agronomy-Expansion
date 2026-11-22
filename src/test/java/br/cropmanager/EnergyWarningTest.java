package br.cropmanager;

import org.junit.Test;
import static org.junit.Assert.*;

public class EnergyWarningTest {
    @Test public void emptyEnergyStaysLowBetweenProcessingTicks() {
        EnergyWarning warning = new EnergyWarning();
        double progress = 0;
        for (int tick = 0; tick < 20; tick++) {
            progress = ScanLogic.credit(progress, 2, 8);
            if (progress >= 1) progress = 0;
            assertTrue(warning.isLow(0, 1, true));
        }
        assertFalse(warning.isLow(10, 1, true));
    }
    @Test public void failedActionPersistsUntilEnoughEnergyOrDisabled() {
        EnergyWarning warning = new EnergyWarning();
        warning.shortage(20);
        for (int tick = 0; tick < 10; tick++) assertTrue(warning.isLow(5, 1, true));
        assertFalse(warning.isLow(20, 1, true));
        assertFalse(warning.isLow(5, 1, true));
        warning.shortage(20);
        assertFalse(warning.isLow(0, 1, false));
        assertFalse(warning.isLow(5, 1, true));
    }
}
