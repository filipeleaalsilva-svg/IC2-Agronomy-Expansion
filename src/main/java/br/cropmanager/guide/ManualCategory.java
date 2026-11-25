package br.cropmanager.guide;

import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ManualCategory {
    private final String id;
    private final String titleKey;
    private final ItemStack icon;
    private final List<ManualEntry> entries = new ArrayList<>();

    public ManualCategory(String id, String titleKey, ItemStack icon) {
        this.id = id;
        this.titleKey = titleKey;
        this.icon = icon;
    }

    public String getId() { return id; }
    public String getTitleKey() { return titleKey; }
    public ItemStack getIcon() { return icon; }
    public List<ManualEntry> getEntries() { return entries; }

    public void addEntry(ManualEntry entry) {
        entries.add(entry);
    }
}
