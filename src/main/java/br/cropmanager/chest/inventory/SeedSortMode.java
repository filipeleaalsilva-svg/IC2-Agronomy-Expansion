package br.cropmanager.chest.inventory;

public enum SeedSortMode {
    TYPE(100), TIER(101), ATTRIBUTES(102);

    private final int requestId;

    SeedSortMode(int requestId) { this.requestId = requestId; }
    public int getRequestId() { return requestId; }

    public static SeedSortMode fromRequestId(int requestId) {
        for (SeedSortMode mode : values()) if (mode.requestId == requestId) return mode;
        return null;
    }
}
