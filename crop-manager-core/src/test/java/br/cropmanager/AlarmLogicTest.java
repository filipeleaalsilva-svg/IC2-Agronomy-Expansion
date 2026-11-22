package br.cropmanager;

import net.minecraft.nbt.NBTTagCompound;
import org.junit.Test;
import static org.junit.Assert.*;

public class AlarmLogicTest {
    @Test
    public void unlinkedNuncaAlarma() {
        assertFalse(AlarmLogic.active("unlinked", new NBTTagCompound(), 63));
    }

    @Test
    public void desconexaoDisparaAlarmeComBit32() {
        assertTrue(AlarmLogic.active("unloaded", new NBTTagCompound(), 63));
        assertTrue(AlarmLogic.active("distance", new NBTTagCompound(), 63));
        assertTrue(AlarmLogic.active("dimension", new NBTTagCompound(), 63));
        assertTrue(AlarmLogic.active("removed", new NBTTagCompound(), 63));
        assertFalse(AlarmLogic.active("unloaded", new NBTTagCompound(), 31));
    }

    @Test
    public void lowEnergyEFlagsDeStatusDisparamAlarme() {
        NBTTagCompound lowEu = new NBTTagCompound();
        lowEu.setBoolean("LowEnergy", true);
        assertTrue(AlarmLogic.active("connected", lowEu, 63));

        NBTTagCompound noWater = new NBTTagCompound();
        noWater.setInteger("Status", ManagerTile.NO_WATER);
        assertTrue(AlarmLogic.active("connected", noWater, 63));
    }

    @Test
    public void normalConectadoSemFalhasNaoAlarma() {
        assertFalse(AlarmLogic.active("connected", new NBTTagCompound(), 63));
    }
}
