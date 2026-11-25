package br.cropmanager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Display-only rounding: machine decisions always use the original values. */
final class UiNumbers {
    private UiNumbers() {}
    @SideOnly(Side.CLIENT)
    private static Locale locale() {
        return net.minecraft.client.Minecraft.getMinecraft().gameSettings.language.startsWith("pt_") ? new Locale("pt", "BR") : Locale.US;
    }
    @SideOnly(Side.CLIENT)
    static String compact(double value) { return compact(value, locale()); }
    static String compact(double value, Locale locale) {
        double divisor = Math.abs(value) >= 1000 ? 1000 : 1;
        BigDecimal rounded = BigDecimal.valueOf(value / divisor).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        boolean approximate = Math.abs(rounded.doubleValue() * divisor - value) > 0.000001;
        String digits = rounded.toPlainString();
        if ("pt".equals(locale.getLanguage())) digits = digits.replace('.', ',');
        return (approximate ? "~" : "") + digits + (divisor == 1000 ? "k" : "");
    }
    @SideOnly(Side.CLIENT)
    static String exact(double value) { return exact(value, locale()); }
    static String exact(double value, Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(0);
        return format.format(value);
    }
    @SideOnly(Side.CLIENT)
    static String resource(double current, double capacity, String unit) { return compact(current) + " / " + compact(capacity) + " " + unit; }
    @SideOnly(Side.CLIENT)
    static String exactResource(double current, double capacity, String unit) { return exact(current) + " / " + exact(capacity) + " " + unit; }
}
