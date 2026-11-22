package br.cropmanager;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.nbt.NBTTagCompound;

final class FarmDisplay {
    private FarmDisplay() {}
    static void diagnostics(FontRenderer font, NBTTagCompound state, int x, int y, int color) {
        diagnostics(font, state, x, y, color, 164);
    }
    static void diagnostics(FontRenderer font, NBTTagCompound state, int x, int y, int color, int width) {
        NBTTagCompound report = state.getCompoundTag("Report");
        long age = state.getLong("ReportAge");
        if (age < 0) { font.drawSplitString(ManagerGui.tr("no_report"), x, y, width, color); return; }
        int[] counts = report.getIntArray("Counts");
        String[] names = {"planted", "mature", "weeds", "unanalyzed", "rejected", "unloaded_positions"};
        for (int i = 0; i < names.length; i++) {
            String value = Integer.toString(counts.length > i ? counts[i] : 0);
            int valueWidth = font.getStringWidth(value);
            String label = font.trimStringToWidth(ManagerGui.tr(names[i] + "_short"), width - valueWidth - 6);
            font.drawString(label, x, y + i * 10, color);
            font.drawString(value, x + font.getStringWidth(label) + 6, y + i * 10, color);
        }
        font.drawString(ManagerGui.tr("report_age_short") + ": " + age / 20 + " s", x, y + 64, color);
        font.drawString(ManagerGui.tr(state.hasKey("State") ? state.getString("State") : state.getBoolean("Paused") ? "paused" : "running"), x, y + 76, color);
        font.drawString(ManagerGui.tr("scan_progress_short") + ": " + state.getInteger("Cursor"), x, y + 88, color);
        if (counts.length > 5 && counts[5] > 0) font.drawString(ManagerGui.tr("incomplete_report"), x, y + 100, color);
        int status = state.getInteger("Status");
        String[] warnings = {"no_energy", "no_fertilizer", "no_water", "no_weed", "no_space"};
        int line = 0;
        for (int i = 0; i < 5; i++) if ((status & 1 << i) != 0) {
            if (line < 2) font.drawString(ManagerGui.tr(warnings[i]), x, y + 112 + line++ * 10, color);
        }
    }
}
