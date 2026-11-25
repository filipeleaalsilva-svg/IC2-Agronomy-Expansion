import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MakeGuideBookTexture {
    public static void main(String[] args) throws Exception {
        int w = 512, h = 512;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        // 1. Double page book background at (0, 0, 292, 180)
        // Outer leather binding
        g.setColor(new Color(0x1a, 0x2e, 0x1d)); // Deep forest leather
        g.fillRoundRect(0, 0, 292, 180, 16, 16);
        g.setColor(new Color(0x27, 0x44, 0x2b)); // Highlight border
        g.drawRoundRect(1, 1, 289, 177, 14, 14);

        // Gold corner accents
        g.setColor(new Color(0xd4, 0xaf, 0x37));
        g.fillRect(3, 3, 8, 3); g.fillRect(3, 3, 3, 8);
        g.fillRect(281, 3, 8, 3); g.fillRect(286, 3, 3, 8);
        g.fillRect(3, 174, 8, 3); g.fillRect(3, 169, 3, 8);
        g.fillRect(281, 174, 8, 3); g.fillRect(286, 169, 3, 8);

        // Left page parchment (6, 6, 137, 168)
        g.setColor(new Color(0xf7, 0xf3, 0xe1)); // Cream parchment
        g.fillRoundRect(6, 6, 137, 168, 6, 6);

        // Right page parchment (149, 6, 137, 168)
        g.setColor(new Color(0xf4, 0xf0, 0xdc)); // Slightly deeper cream
        g.fillRoundRect(149, 6, 137, 168, 6, 6);

        // Center spine crease / shadow
        g.setColor(new Color(0xd1, 0xc7, 0xa7));
        g.fillRect(141, 6, 5, 168);
        g.setColor(new Color(0xbc, 0xb0, 0x8d));
        g.fillRect(145, 6, 2, 168);
        g.setColor(new Color(0xc9, 0xbd, 0x9b));
        g.fillRect(147, 6, 4, 168);

        // Page border lines
        g.setColor(new Color(0xe2, 0xd8, 0xb8));
        g.drawRect(8, 8, 133, 164);
        g.drawRect(151, 8, 133, 164);

        // 2. Crafting Grid background template at (300, 0, 120, 80)
        // 3x3 slot backgrounds (18x18 each)
        g.setColor(new Color(0x8b, 0x82, 0x6e)); // Slot dark border
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int sx = 300 + col * 18;
                int sy = 0 + row * 18;
                drawSlot(g, sx, sy);
            }
        }
        // Crafting Arrow at (360, 18, 22, 15)
        drawArrow(g, 360, 18);

        // Result slot (24x24) at (388, 14)
        drawBigSlot(g, 388, 14);

        // 3. Tab buttons at (0, 200)
        // Active tab (28x24)
        g.setColor(new Color(0x27, 0x44, 0x2b));
        g.fillRoundRect(0, 200, 28, 24, 6, 6);
        g.setColor(new Color(0xf7, 0xf3, 0xe1));
        g.fillRect(6, 202, 22, 20);

        // Inactive tab (24x22)
        g.setColor(new Color(0x1a, 0x2e, 0x1d));
        g.fillRoundRect(35, 200, 24, 22, 6, 6);
        g.setColor(new Color(0xd9, 0xd2, 0xb8));
        g.fillRect(39, 202, 18, 18);

        g.dispose();

        File out = new File("src/main/resources/assets/cropmanager/textures/gui/guide_book.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("Generated guide book texture: " + out.getAbsolutePath());
    }

    private static void drawSlot(Graphics2D g, int x, int y) {
        g.setColor(new Color(0x8b, 0x8b, 0x8b));
        g.fillRect(x, y, 18, 18);
        g.setColor(new Color(0x37, 0x37, 0x37));
        g.fillRect(x, y, 17, 1);
        g.fillRect(x, y, 1, 17);
        g.setColor(new Color(0xff, 0xff, 0xff));
        g.fillRect(x + 1, y + 17, 17, 1);
        g.fillRect(x + 17, y + 1, 1, 17);
        g.setColor(new Color(0xc6, 0xc6, 0xc6));
        g.fillRect(x + 1, y + 1, 16, 16);
    }

    private static void drawBigSlot(Graphics2D g, int x, int y) {
        g.setColor(new Color(0x8b, 0x8b, 0x8b));
        g.fillRect(x, y, 26, 26);
        g.setColor(new Color(0x37, 0x37, 0x37));
        g.fillRect(x, y, 25, 2);
        g.fillRect(x, y, 2, 25);
        g.setColor(new Color(0xff, 0xff, 0xff));
        g.fillRect(x + 2, y + 24, 24, 2);
        g.fillRect(x + 24, y + 2, 2, 24);
        g.setColor(new Color(0xc6, 0xc6, 0xc6));
        g.fillRect(x + 2, y + 2, 22, 22);
    }

    private static void drawArrow(Graphics2D g, int x, int y) {
        g.setColor(new Color(0x55, 0x55, 0x55));
        g.fillRect(x, y + 5, 14, 4);
        int[] xp = {x + 14, x + 22, x + 14};
        int[] yp = {y, y + 7, y + 14};
        g.fillPolygon(xp, yp, 3);
    }
}
