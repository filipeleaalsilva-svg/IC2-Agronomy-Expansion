package br.cropmanager;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;
public class AlarmLogicTest {
    @Test public void categoriesAreIndependentAndDisconnectedIsNotStaleData() {
        NBTTagCompound s = new NBTTagCompound(); s.setInteger("Status", 30);
        assertFalse(AlarmLogic.active("connected", s, 1));
        for (int bit = 2; bit <= 16; bit <<= 1) assertTrue(AlarmLogic.active("connected", s, bit));
        assertFalse(AlarmLogic.active("connected", s, 32));
        assertFalse(AlarmLogic.active("unlinked", s, 63));
        assertFalse(AlarmLogic.active("unloaded", s, 31));
        assertTrue(AlarmLogic.active("removed", s, 32));
        assertFalse(AlarmLogic.active("dimension", s, 0));
    }
    @Test public void lowEnergyIsIndependentOfPausedStatus() {
        NBTTagCompound s = new NBTTagCompound(); s.setInteger("Status", 0); s.setBoolean("LowEnergy", true);
        assertTrue(AlarmLogic.active("connected", s, 1));
        assertFalse(AlarmLogic.active("connected", s, 62));
    }
}
