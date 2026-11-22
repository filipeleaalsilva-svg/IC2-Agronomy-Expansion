package br.cropmanager;

import net.minecraft.nbt.NBTTagCompound;

final class AlarmLogic {
    private AlarmLogic() {}
    static boolean active(String connection, NBTTagCompound snapshot, int mask) {
        if ("unlinked".equals(connection)) return false;
        if (!"connected".equals(connection)) return (mask & 32) != 0;
        int problems = snapshot.getInteger("Status") & 30;
        if (snapshot.getBoolean("LowEnergy")) problems |= 1;
        return (problems & mask & 31) != 0;
    }
}
