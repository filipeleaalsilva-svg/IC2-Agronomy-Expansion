import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;

/**
 * Generates the neutral #1C1C1D Dark Theme resource pack for all Crop Manager GUIs.
 */
public final class BuildDarkGuiPack {
    private static final String[] GUI_FILES = {
        "agricultural_reservoir.png",
        "agricultural_silo.png",
        "crop_database_chest.png",
        "crop_database_drawer.png",
        "equipment_analyzer.png",
        "equipment_composter.png",
        "equipment_extractor.png",
        "equipment_grinder.png",
        "equipment_hydroponic.png",
        "equipment_legacy.png",
        "equipment_mixer.png",
        "equipment_monitor.png",
        "equipment_monitor_compact.png",
        "equipment_weeder_compact.png",
        "equipment_weeder_expanded.png",
        "fluid_tank.png",
        "guide_book.png",
        "manager_advanced.png",
        "manager_advanced_compact.png",
        "manager_basic.png"
    };

    public static void main(String[] args) throws IOException {
        Path root = Paths.get("").toAbsolutePath();
        Path lightDir = root.resolve("src/main/resources/assets/cropmanager/textures/gui");
        Path packDir = root.resolve("dist/CropManager-GUI-Dark-0.5.0-alpha11");
        Path darkGuiDir = packDir.resolve("assets/cropmanager/textures/gui");
        Files.createDirectories(darkGuiDir);

        for (String name : GUI_FILES) {
            Path src = lightDir.resolve(name);
            if (!Files.isRegularFile(src)) {
                throw new IOException("Missing light GUI texture: " + src);
            }
            BufferedImage img = ImageIO.read(src.toFile());
            BufferedImage dark = toDark1C1C1D(img, "guide_book.png".equals(name));
            ImageIO.write(dark, "PNG", darkGuiDir.resolve(name).toFile());
        }

        String mcmeta = "{\"pack\":{\"pack_format\":3,\"description\":\"IC2: Agronomy Expansion - Dark Theme (#1C1C1D)\"}}";
        Files.write(packDir.resolve("pack.mcmeta"), mcmeta.getBytes(StandardCharsets.UTF_8));

        // Copy pack.png if available
        Path iconSource = root.resolve("dist/CropManager-GUI-Dark-0.5.0-alpha9/pack.png");
        if (!Files.isRegularFile(iconSource)) {
            iconSource = root.resolve("src/main/resources/pack.png");
        }
        if (Files.isRegularFile(iconSource)) {
            Files.copy(iconSource, packDir.resolve("pack.png"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }

        // Verify #1C1C1D body pixel on equipment_monitor.png
        BufferedImage check = ImageIO.read(darkGuiDir.resolve("equipment_monitor.png").toFile());
        int rgb = check.getRGB(8, 8) & 0xFFFFFF;
        if (rgb != 0x1C1C1D) {
            throw new IllegalStateException(String.format("Expected #1C1C1D body pixel, got #%06X", rgb));
        }

        // Build ZIP archives
        Path[] targets = {
            root.resolve("dist/IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip"),
            root.resolve("dist/CropManager-GUI-Dark-0.5.0-alpha11.zip"),
            root.resolve("IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip"),
            root.resolve("CropManager-GUI-Dark-0.5.0-alpha11.zip"),
            root.resolve("build/libs/IC2-Agronomy-Expansion-GUI-Dark-0.5.0-alpha11.zip")
        };

        for (Path zipPath : targets) {
            if (zipPath.getParent() != null) Files.createDirectories(zipPath.getParent());
            try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
                addToZip(zos, packDir, packDir.resolve("pack.mcmeta"));
                if (Files.isRegularFile(packDir.resolve("pack.png"))) {
                    addToZip(zos, packDir, packDir.resolve("pack.png"));
                }
                for (String name : GUI_FILES) {
                    addToZip(zos, packDir, darkGuiDir.resolve(name));
                }
            }
            System.out.println("Generated: " + zipPath);
        }

        System.out.println("SUCCESS: Generated " + GUI_FILES.length + " dark GUI textures (#1C1C1D) in all target resource packs.");
    }

    private static BufferedImage toDark1C1C1D(BufferedImage src, boolean isGuideBook) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                if (a == 0) {
                    out.setRGB(x, y, 0);
                    continue;
                }
                int r = (argb >>> 16) & 0xFF;
                int g = (argb >>> 8) & 0xFF;
                int b = argb & 0xFF;
                int max = Math.max(r, Math.max(g, b));
                int min = Math.min(r, Math.min(g, b));
                int spread = max - min;

                if (spread <= 10 || (isGuideBook && spread <= 48 && r >= g && g >= b && max >= 140)) {
                    int lum = (r + g + b) / 3;
                    int mapped;
                    if (lum >= 230) {
                        mapped = 0x4E4E52; // highlight bevel
                    } else if (lum >= 186) {
                        mapped = 0x1C1C1D; // main GUI body (#1C1C1D)
                    } else if (lum >= 165) {
                        mapped = 0x181819; // subpanel / mid-dark panel
                    } else if (lum >= 132) {
                        mapped = 0x121213; // slot / seam recess (#8B8B8B)
                    } else if (lum >= 100) {
                        mapped = 0x151516; // mid recess
                    } else if (lum >= 70) {
                        mapped = 0x0D0D0E; // shadow (#555555)
                    } else if (lum >= 30) {
                        mapped = 0x080809; // deep shadow / rivet (#373737)
                    } else {
                        mapped = 0x050506; // outer border (#000000)
                    }
                    out.setRGB(x, y, (a << 24) | mapped);
                } else {
                    out.setRGB(x, y, argb);
                }
            }
        }
        return out;
    }

    private static void addToZip(ZipOutputStream zos, Path root, Path file) throws IOException {
        String entryName = root.relativize(file).toString().replace('\\', '/');
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        Files.copy(file, buf);
        zos.write(buf.toByteArray());
        zos.closeEntry();
    }
}
