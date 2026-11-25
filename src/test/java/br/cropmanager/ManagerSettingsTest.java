package br.cropmanager;

import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ManagerSettingsTest {
    @BeforeClass public static void bootstrapVanilla() {
        Bootstrap.register();
        if (TileEntity.getKey(ManagerTile.class) == null) {
            TileEntity.register("cropmanager:crop_manager", ManagerTile.class);
        }
    }

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

    @Test public void managerTilePersistsAndClampsOffsetsInNbt() {
        ManagerTile source = new ManagerTile();
        source.settings(4, 7, 15, 7, -3, 12);
        assertEquals(7, source.offsetX);
        assertEquals(-3, source.offsetY);
        assertEquals(12, source.offsetZ);

        source.settings(4, 7, 15, 17, 0, 0);
        source.settings(4, 7, 15, 0, -6, 0);
        assertEquals(7, source.offsetX);
        assertEquals(-3, source.offsetY);
        assertEquals(12, source.offsetZ);

        NBTTagCompound saved = source.writeToNBT(new NBTTagCompound());
        ManagerTile restored = new ManagerTile();
        restored.readFromNBT(saved);
        assertEquals(7, restored.offsetX);
        assertEquals(-3, restored.offsetY);
        assertEquals(12, restored.offsetZ);

        saved.setInteger("OffsetX", 99);
        saved.setInteger("OffsetY", -99);
        saved.setInteger("OffsetZ", -50);
        restored.readFromNBT(saved);
        assertEquals(ScanLogic.MAX_HORIZONTAL_OFFSET, restored.offsetX);
        assertEquals(-ScanLogic.MAX_VERTICAL_OFFSET, restored.offsetY);
        assertEquals(-ScanLogic.MAX_HORIZONTAL_OFFSET, restored.offsetZ);
    }

    @Test public void managerTileSyncsOffsetsViaGuiStateAndControls() {
        ManagerTile server = new ManagerTile();
        assertTrue(server.control(241, null));
        assertEquals(1, server.offsetX);
        assertTrue(server.control(240, null));
        assertEquals(0, server.offsetX);
        assertTrue(server.control(243, null));
        assertEquals(1, server.offsetY);
        assertTrue(server.control(242, null));
        assertEquals(0, server.offsetY);
        assertTrue(server.control(245, null));
        assertEquals(1, server.offsetZ);
        assertTrue(server.control(244, null));
        assertEquals(0, server.offsetZ);

        server.settings(5, 3, 15, -4, 2, 9);
        NBTTagCompound state = server.guiState();
        ManagerTile client = new ManagerTile();
        client.acceptGuiState(state);
        assertEquals(5, client.radius);
        assertEquals(3, client.layers);
        assertEquals(-4, client.offsetX);
        assertEquals(2, client.offsetY);
        assertEquals(9, client.offsetZ);

        assertTrue(server.control(215, null));
        assertEquals(0, server.offsetX);
        assertEquals(0, server.offsetY);
        assertEquals(0, server.offsetZ);
    }
}
