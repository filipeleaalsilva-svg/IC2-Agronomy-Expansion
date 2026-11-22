package br.cropmanager.chest.client;

/** Geometry only: shared by the renderer, hit testing and headless layout checks. */
public final class CropGuiLayout {
    public static final int WIDTH = 195, HEIGHT = 216;
    public static final int TEXT_LEFT = 10, TEXT_RIGHT = 90, TEXT_WIDTH = 80;
    public static final int HEADER_Y = 14, NAME_Y = 26, NAME_LINE_HEIGHT = 10;
    public static final int[] STAT_Y = {46, 56, 66};
    public static final int PRODUCES_Y = 76, PRODUCT_Y = 84, PRODUCTS_PER_ROW = 5;
    public static final int TAB_WIDTH = 22, DRAWER_WIDTH = 94, DRAWER_HEIGHT = 88;
    public static final Region PANEL = new Region(7, 11, 86, 91);
    public static final Region HEADER = new Region(TEXT_LEFT, HEADER_Y, TEXT_WIDTH, 9);
    public static final Region NAME = new Region(TEXT_LEFT, NAME_Y, TEXT_WIDTH, 19);
    public static final Region PRODUCES = new Region(TEXT_LEFT, PRODUCES_Y, TEXT_WIDTH, 8);
    public static final Region PRODUCTS = new Region(TEXT_LEFT, PRODUCT_Y, TEXT_WIDTH, 16);
    public static final Region GRID = new Region(99, 15, 90, 90);
    public static final Region PLAYER = new Region(17, 133, 162, 76);

    private CropGuiLayout() { }
    public static int guiLeft(int screenWidth, boolean tabs) {
        int centered = Math.max(0, (screenWidth - WIDTH) / 2);
        return tabs ? Math.max(0, Math.min(centered, screenWidth - WIDTH - TAB_WIDTH)) : centered;
    }
    public static Region drawer(int screenWidth, int guiLeft, int guiTop) {
        int outside = guiLeft + WIDTH + TAB_WIDTH;
        return new Region(outside + DRAWER_WIDTH <= screenWidth ? outside : guiLeft + WIDTH - DRAWER_WIDTH,
                guiTop + 8, DRAWER_WIDTH, DRAWER_HEIGHT);
    }
    public static Region tab(int guiLeft, int guiTop, int tab) {
        return new Region(guiLeft + WIDTH, guiTop + 10 + tab * 28, TAB_WIDTH, 24);
    }
    public static int productStart(int count) { return TEXT_LEFT + (TEXT_WIDTH - Math.max(0, count) * 16) / 2; }
    public static final class Region {
        public final int x, y, width, height;
        public Region(int x, int y, int width, int height) { this.x = x; this.y = y; this.width = width; this.height = height; }
        public boolean contains(int px, int py) { return px >= x && px < x + width && py >= y && py < y + height; }
        public boolean contains(Region other) { return other.x >= x && other.y >= y && other.x + other.width <= x + width && other.y + other.height <= y + height; }
        public boolean intersects(Region other) { return x < other.x + other.width && x + width > other.x && y < other.y + other.height && y + height > other.y; }
    }
}
