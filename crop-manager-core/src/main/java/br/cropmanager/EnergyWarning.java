package br.cropmanager;

/** Keeps a failed action's energy requirement between scan ticks. */
final class EnergyWarning {
    private double required;
    void shortage(double cost) { required = Math.max(required, cost); }
    void clear() { required = 0; }
    boolean isLow(double available, double scanCost, boolean enabled) {
        if (!enabled) { clear(); return false; }
        if (available >= required) clear();
        return available < Math.max(scanCost, required);
    }
}
