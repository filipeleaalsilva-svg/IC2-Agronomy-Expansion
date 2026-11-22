package br.cropmanager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Locale;

/** Display-only rounding: machine decisions always use the original values. */
final class UiNumbers {
    private UiNumbers() {}
    private static Locale locale() {
        return net.minecraft.client.Minecraft.getMinecraft().gameSettings.language.startsWith("pt_") ? new Locale("pt", "BR") : Locale.US;
    }
    static String compact(double value) { return compact(value, locale()); }
    static String compact(double value, Locale locale) {
        double divisor = Math.abs(value) >= 1000 ? 1000 : 1;
        BigDecimal rounded = BigDecimal.valueOf(value / divisor).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
        boolean approximate = Math.abs(rounded.doubleValue() * divisor - value) > 0.000001;
        String digits = rounded.toPlainString();
        if ("pt".equals(locale.getLanguage())) digits = digits.replace('.', ',');
        return (approximate ? "~" : "") + digits + (divisor == 1000 ? "k" : "");
    }
    static String exact(double value) { return exact(value, locale()); }
    static String exact(double value, Locale locale) {
        NumberFormat format = NumberFormat.getNumberInstance(locale);
        format.setMaximumFractionDigits(0);
        return format.format(value);
    }
    static String resource(double current, double capacity, String unit) { return compact(current) + " / " + compact(capacity) + " " + unit; }
    static String exactResource(double current, double capacity, String unit) { return exact(current) + " / " + exact(capacity) + " " + unit; }
}
