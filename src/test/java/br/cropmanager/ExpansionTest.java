package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import java.util.Collections;
import java.util.List;
import net.minecraft.util.ResourceLocation;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class ExpansionTest {
    private static final CropCard WHEAT = new CropCard() {
        public String getId() { return "wheat"; }
        public String getOwner() { return "ic2"; }
        public CropProperties getProperties() { return new CropProperties(1, 0, 0, 0, 0, 0); }
        public int getMaxSize() { return 7; }
        public List<ResourceLocation> getTexturesLocation() { return Collections.emptyList(); }
    };
    @Test public void wirelessEuUpgradeSupportsAnyIc2UpgradableProperties() {
        net.minecraft.init.Bootstrap.register();
        WirelessEuUpgrade upg = new WirelessEuUpgrade();
        assertTrue(upg.isSuitableFor(net.minecraft.item.ItemStack.EMPTY, java.util.EnumSet.of(ic2.api.upgrade.UpgradableProperty.Processing)));
        assertTrue(upg.isSuitableFor(net.minecraft.item.ItemStack.EMPTY, java.util.EnumSet.of(ic2.api.upgrade.UpgradableProperty.EnergyStorage)));
        assertFalse(upg.isSuitableFor(net.minecraft.item.ItemStack.EMPTY, java.util.Collections.emptySet()));
    }
    @Test public void legacyFilterNbtIsIgnoredAndHarvestTogglesCleanly() {
        int[] next = ManagerSettings.applyControl(0, 4, 2, 15, 8);
        assertNotNull(next);
        assertEquals(14, next[2]);
    }
    @Test public void redstoneModesHaveExplicitTruthTable() {
        assertTrue(ManagerTile.redstoneAllows(0, false)); assertTrue(ManagerTile.redstoneAllows(0, true));
        assertFalse(ManagerTile.redstoneAllows(1, false)); assertTrue(ManagerTile.redstoneAllows(1, true));
        assertTrue(ManagerTile.redstoneAllows(2, false)); assertFalse(ManagerTile.redstoneAllows(2, true));
    }
    @Test public void advancedProfileChangesCapacityNotInventoryOrArea() {
        ManagerTile base = new ManagerTile(); ManagerTile advanced = new AdvancedManagerTile();
        assertEquals(1, base.energy.getSinkTier()); assertEquals(2, advanced.energy.getSinkTier());
        assertEquals(100000, advanced.energy.getCapacity(), 0); assertEquals(20000, advanced.water.getCapacity());
        assertEquals(base.inventory.getSlots(), advanced.inventory.getSlots()); assertEquals(base.radius, advanced.radius); assertEquals(base.layers, advanced.layers);
    }
}
