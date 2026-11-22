package br.cropmanager;

/** Pure traversal and budgeting rules, shared by the machine and its tests. */
public final class ScanLogic {
    private ScanLogic() {}

    public static int volume(int radius, int layers) {
        return (2 * radius + 1) * (2 * radius + 1) * Integer.bitCount(layers & 7);
    }

    public static int[] offset(int index, int radius, int layers) {
        int side = 2 * radius + 1;
        int plane = side * side;
        int selectedLayer = index / plane;
        int y = -1;
        for (int bit = 0; bit < 3; bit++) {
            if ((layers & (1 << bit)) != 0 && selectedLayer-- == 0) {
                y = bit - 1;
                break;
            }
        }
        return new int[] {index % side - radius, y, index % plane / side - radius};
    }

    public static double credit(double previous, double interval, int cap) {
        return Math.min(cap, previous + Math.min(cap, 1.0 / interval));
    }

    public static boolean validSettings(int radius, int layers, int functions, int maxRadius) {
        return radius >= 1 && radius <= maxRadius && layers >= 1 && layers <= 7
                && functions >= 0 && functions <= 15;
    }
}
