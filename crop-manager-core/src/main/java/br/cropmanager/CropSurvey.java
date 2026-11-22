package br.cropmanager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/** One completed scan's planted species and real IC2 environment samples. */
final class CropSurvey {
    private final Samples total = new Samples();
    private final Map<String, Samples> species = new HashMap<>();

    // Order: terrain nutrients, terrain humidity, terrain air quality, light level.
    void add(String owner, String id, boolean identified, boolean weed, int[] environment) {
        if (weed) return;
        String key = identified ? owner + ":" + id : "";
        total.add(environment);
        species.computeIfAbsent(key, ignored -> new Samples()).add(environment);
    }

    NBTTagCompound write() {
        NBTTagCompound result = total.write();
        List<Map.Entry<String, Samples>> entries = new ArrayList<>(species.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Samples>>comparingInt(e -> e.getValue().count)
                .reversed().thenComparing(Map.Entry::getKey));
        NBTTagList list = new NBTTagList();
        for (Map.Entry<String, Samples> entry : entries) {
            NBTTagCompound row = entry.getValue().write();
            String key = entry.getKey(); int separator = key.indexOf(':');
            row.setString("Owner", separator < 0 ? "" : key.substring(0, separator));
            row.setString("Id", separator < 0 ? "" : key.substring(separator + 1));
            list.appendTag(row);
        }
        result.setTag("Species", list);
        return result;
    }

    private static final class Samples {
        int count;
        final int[] sum = new int[4], min = new int[4], max = new int[4];
        void add(int[] values) {
            for (int i = 0; i < 4; i++) {
                sum[i] += values[i];
                if (count == 0 || values[i] < min[i]) min[i] = values[i];
                if (count == 0 || values[i] > max[i]) max[i] = values[i];
            }
            count++;
        }
        NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("Count", count);
            // NBT arrays must be copied: a published result cannot change mid-cycle.
            tag.setIntArray("Sum", sum.clone()); tag.setIntArray("Min", min.clone()); tag.setIntArray("Max", max.clone());
            return tag;
        }
    }
}
