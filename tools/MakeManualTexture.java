import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class MakeManualTexture {
    public static void main(String[] args) throws Exception {
        int w = 16, h = 16;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        int O = 0x00000000; // Transparent
        int B = 0xFF142217; // Dark border outline
        int G = 0xFF1e4c2b; // Dark forest green
        int M = 0xFF2d6c3e; // Medium green
        int L = 0xFF3c884f; // Light green
        int Y = 0xFFd6a836; // Gold corner/trim
        int K = 0xFFf5d862; // Bright gold highlight
        int D = 0xFF966f1c; // Dark gold shadow
        int P = 0xFFded8bf; // Page paper cream
        int S = 0xFF4ade80; // Vibrant sprout green

        // 16x16 Book layout angled / isometric-facing book
        int[][] pixels = {
            {O, O, O, B, B, B, B, B, B, B, B, B, B, O, O, O},
            {O, O, B, Y, Y, G, M, M, M, M, G, Y, Y, B, O, O},
            {O, B, Y, K, M, M, L, L, L, M, M, M, Y, P, B, O},
            {B, Y, K, M, M, L, L, S, L, L, M, M, M, P, P, B},
            {B, Y, M, M, L, S, S, S, S, L, M, M, M, P, P, B},
            {B, G, M, L, L, S, Y, Y, S, L, L, M, M, P, P, B},
            {B, G, M, L, S, Y, K, K, Y, S, L, M, M, P, P, B},
            {B, G, M, L, S, Y, K, K, Y, S, L, M, M, P, P, B},
            {B, G, M, L, L, S, Y, Y, S, L, L, M, M, P, P, B},
            {B, G, M, M, L, S, S, S, S, L, M, M, M, P, P, B},
            {B, Y, M, M, M, L, L, S, L, L, M, M, M, P, P, B},
            {O, B, Y, D, M, M, L, L, L, M, M, M, Y, P, B, O},
            {O, O, B, Y, Y, G, M, M, M, M, G, Y, Y, B, O, O},
            {O, O, O, B, B, P, P, P, P, P, P, B, B, O, O, O},
            {O, O, O, O, B, B, B, B, B, B, B, B, O, O, O, O},
            {O, O, O, O, O, O, O, O, O, O, O, O, O, O, O, O}
        };

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, pixels[y][x]);
            }
        }

        File out = new File("src/main/resources/assets/cropmanager/textures/items/agricultural_manual.png");
        ImageIO.write(img, "PNG", out);
        System.out.println("Generated manual texture: " + out.getAbsolutePath());
    }
}
