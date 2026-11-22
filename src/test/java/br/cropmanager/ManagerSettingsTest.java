package br.cropmanager;

import org.junit.Test;
import static org.junit.Assert.*;

public class ManagerSettingsTest {
    @Test public void independentChangesPreservePreviouslyAppliedChanges() {
        int[] first = ManagerSettings.applyControl(0, 4, 7, 15, 8);
        int[] second = ManagerSettings.applyControl(2, first[0], first[1], first[2], 8);
        assertArrayEquals(new int[] {4, 7, 10}, second);
        int[] range = ManagerSettings.applyControl(5, second[0], second[1], second[2], 8);
        assertArrayEquals(new int[] {5, 7, 10}, range);
    }

    @Test public void rapidRepeatedClicksApplyEachIntentOnce() {
        int[] first = ManagerSettings.applyControl(5, 4, 7, 15, 8);
        int[] second = ManagerSettings.applyControl(5, first[0], first[1], first[2], 8);
        assertEquals(6, second[0]);
        int[] toggle = ManagerSettings.applyControl(1, second[0], second[1], second[2], 8);
        int[] restored = ManagerSettings.applyControl(1, toggle[0], toggle[1], toggle[2], 8);
        assertArrayEquals(second, restored);
    }

    @Test public void serverRejectsInvalidControlBoundsAndLastLayerRemoval() {
        assertNull(ManagerSettings.applyControl(-1, 4, 7, 15, 8));
        assertNull(ManagerSettings.applyControl(9, 4, 7, 15, 8));
        assertNull(ManagerSettings.applyControl(4, 1, 7, 15, 8));
        assertNull(ManagerSettings.applyControl(5, 8, 7, 15, 8));
        assertNull(ManagerSettings.applyControl(6, 4, 1, 15, 8));
        int[] first = ManagerSettings.applyControl(6, 4, 3, 15, 8);
        assertArrayEquals(new int[] {4, 2, 15}, first);
        assertNull(ManagerSettings.applyControl(7, first[0], first[1], first[2], 8));
    }
}
