import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class ConvertTextures {
    public static void main(String[] args) throws Exception {
        File srcDir = new File(args.length > 0 ? args[0] : "build/extracted_model");
        File destDir = new File("src/main/resources/assets/cropmanager/textures/blocks");
        
        File[] pngs = srcDir.listFiles((dir, name) -> name.endsWith(".png"));
        for (File f : pngs) {
            BufferedImage orig = ImageIO.read(f);
            BufferedImage rgba = new BufferedImage(orig.getWidth(), orig.getHeight(), BufferedImage.TYPE_INT_ARGB);
            rgba.getGraphics().drawImage(orig, 0, 0, null);
            File out = new File(destDir, f.getName());
            ImageIO.write(rgba, "PNG", out);
            System.out.println("Wrote: " + out.getName() + " " + rgba.getWidth() + "x" + rgba.getHeight());
        }
        // Also copy storage_steel.png to water_tower_iron_plate.png
        BufferedImage steel = ImageIO.read(new File(srcDir, "storage_steel.png"));
        BufferedImage rgbaSteel = new BufferedImage(steel.getWidth(), steel.getHeight(), BufferedImage.TYPE_INT_ARGB);
        rgbaSteel.getGraphics().drawImage(steel, 0, 0, null);
        File ironPlateOut = new File(destDir, "water_tower_iron_plate.png");
        ImageIO.write(rgbaSteel, "PNG", ironPlateOut);
        System.out.println("Wrote: " + ironPlateOut.getName() + " " + rgbaSteel.getWidth() + "x" + rgbaSteel.getHeight());
    }
}
