package br.cropmanager;

/** Applies one GUI control to the latest server state, never a client snapshot. */
final class ManagerSettings {
    private ManagerSettings() {}

    static int[] applyControl(int control, int radius, int layers, int functions, int maxRadius) {
        if (control >= 0 && control < 4) functions ^= 1 << control;
        else if (control == 4) radius--;
        else if (control == 5) radius++;
        else if (control >= 6 && control < 9) layers ^= 1 << (control - 6);
        else return null;
        return ScanLogic.validSettings(radius, layers, functions, maxRadius)
                ? new int[] {radius, layers, functions} : null;
    }
}
