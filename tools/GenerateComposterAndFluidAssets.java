import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/** Deterministic Java asset generator for Manual Composter (half-height) and Fluid Logistics blocks. */
public final class GenerateComposterAndFluidAssets {
    private static final Path ASSETS = Paths.get("src", "main", "resources", "assets", "cropmanager");
    private static final Path TEX_BLOCKS = ASSETS.resolve("textures").resolve("blocks");
    private static final Path BLOCKSTATES = ASSETS.resolve("blockstates");
    private static final Path MODELS_BLOCK = ASSETS.resolve("models").resolve("block");
    private static final Path MODELS_ITEM = ASSETS.resolve("models").resolve("item");

    private static final Map<Character, Integer> PAL = new HashMap<>();
    static {
        PAL.put('.', 0x00000000);
        PAL.put('k', 0xFF1E130B);
        PAL.put('w', 0xFF3B2414);
        PAL.put('W', 0xFF583820);
        PAL.put('m', 0xFF754D2E);
        PAL.put('M', 0xFF946741);
        PAL.put('t', 0xFFB48656);
    }

    public static void main(String[] args) throws Exception {
        String target = args.length > 0 ? args[0] : "all";
        if ("all".equals(target) || "composter".equals(target)) {
            generateManualComposter();
        }
        if ("all".equals(target) || "fluid_pipe".equals(target)) {
            generateFluidPipe();
        }
        if ("all".equals(target) || "fluid_restrictor".equals(target)) {
            generateFluidRestrictor();
        }
        if ("all".equals(target) || "fluid_router".equals(target)) {
            generateFluidRouter();
        }
    }

    private static BufferedImage fromGrid(String[] rows) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            String r = rows[y];
            for (int x = 0; x < 16; x++) {
                img.setRGB(x, y, PAL.get(r.charAt(x)));
            }
        }
        return img;
    }

    private static void savePng(Path path, BufferedImage img) throws IOException {
        Files.createDirectories(path.getParent());
        // Ensure at least one pixel has alpha=254 if all are 255? Wait: ImageIO TYPE_INT_ARGB always writes colorType=6!
        ImageIO.write(img, "PNG", path.toFile());
    }

    private static void saveJson(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
        Files.write(path, (content.trim() + "\n").getBytes(StandardCharsets.UTF_8));
    }

    private static void generateManualComposter() throws IOException {
        BufferedImage wood = fromGrid(new String[] {
                "wWWWWWWWWWWWWWWw",
                "WttMtMMttMMtMttW",
                "WMmMMmmMMmMMmmMW",
                "WmmWmmmWWmmmWmmW",
                "WWWWWWWWWWWWWWWW",
                "wttttMMttMMttttw",
                "WMmmMMmMMmMMmmMW",
                "WmmWWmmWWmmWWmmW",
                "WWWWWWWWWWWWWWWW",
                "wttMMtMttMtMMttw",
                "WMmMMmmMMmmMMmMW",
                "WmmmWmmWWmmWmmmW",
                "WWWWWWWWWWWWWWWW",
                "wtMttMMttMMttMtw",
                "WMmmWmmMMmmWmmMW",
                "wwwwwwwwwwwwwwww"
        });
        savePng(TEX_BLOCKS.resolve("manual_composter_wood.png"), wood);

        BufferedImage rim = fromGrid(new String[] {
                "kkkkkkkkkkkkkkkk",
                "kMMMMMMMMMMMMMMw",
                "kMtWWWWWWWWWWtWw",
                "kMWmmmmmmmmmmWWw",
                "kMWmWWWWWWWWmWWw",
                "kMWmWwwwwwwWmWWw",
                "kMWmWwWWWWwWmWWw",
                "kMWmWwWmmWwWmWWw",
                "kMWmWwWmmWwWmWWw",
                "kMWmWwWWWWwWmWWw",
                "kMWmWwwwwwwWmWWw",
                "kMWmWWWWWWWWmWWw",
                "kMWmmmmmmmmmmWWw",
                "kMtWWWWWWWWWWtWw",
                "kwwwwwwwwwwwwwww",
                "kkkkkkkkkkkkkkkk"
        });
        savePng(TEX_BLOCKS.resolve("manual_composter_rim.png"), rim);
        Files.deleteIfExists(TEX_BLOCKS.resolve("manual_composter_steel.png"));

        double[] topYs = {-1, 3.2, 4.8, 6.2, 7.2, 7.2};
        String[] fillTexs = {
                null,
                "cropmanager:blocks/manual_composter_raw",
                "cropmanager:blocks/manual_composter_raw",
                "cropmanager:blocks/manual_composter_raw",
                "cropmanager:blocks/manual_composter_fermenting",
                "cropmanager:blocks/manual_composter_ready"
        };

        StringBuilder bs = new StringBuilder("{\n  \"variants\": {\n");
        for (int level = 0; level <= 5; level++) {
            StringBuilder m = new StringBuilder();
            m.append("{\n  \"parent\": \"block/block\",\n  \"textures\": {\n");
            m.append("    \"wood\": \"cropmanager:blocks/manual_composter_wood\",\n");
            m.append("    \"rim\": \"cropmanager:blocks/manual_composter_rim\",\n");
            m.append("    \"particle\": \"cropmanager:blocks/manual_composter_wood\"");
            if (fillTexs[level] != null) {
                m.append(",\n    \"fill\": \"").append(fillTexs[level]).append("\"");
            }
            m.append("\n  },\n  \"elements\": [\n");
            // Floor + 4 corner posts + 2 plank tiers + top lip (all y <= 8.0)
            m.append(boxAll(2, 0.5, 2, 14, 1.5, 14, "wood", "0, 0, 16, 16")).append(",\n");
            m.append(boxAll(1, 0, 1, 3, 8, 3, "rim", "0, 0, 4, 16")).append(",\n");
            m.append(boxAll(13, 0, 1, 15, 8, 3, "rim", "12, 0, 16, 16")).append(",\n");
            m.append(boxAll(1, 0, 13, 3, 8, 15, "rim", "0, 0, 4, 16")).append(",\n");
            m.append(boxAll(13, 0, 13, 15, 8, 15, "rim", "12, 0, 16, 16")).append(",\n");
            m.append(plankNs(1.0, 4.0, 1.5, 2.5)).append(",\n");
            m.append(plankNs(1.0, 4.0, 13.5, 14.5)).append(",\n");
            m.append(plankEw(1.0, 4.0, 1.5, 2.5)).append(",\n");
            m.append(plankEw(1.0, 4.0, 13.5, 14.5)).append(",\n");
            m.append(plankNs(4.2, 7.0, 1.5, 2.5)).append(",\n");
            m.append(plankNs(4.2, 7.0, 13.5, 14.5)).append(",\n");
            m.append(plankEw(4.2, 7.0, 1.5, 2.5)).append(",\n");
            m.append(plankEw(4.2, 7.0, 13.5, 14.5)).append(",\n");
            m.append(boxAll(3, 7.0, 1, 13, 8.0, 2.6, "rim", "2, 0, 14, 3")).append(",\n");
            m.append(boxAll(3, 7.0, 13.4, 13, 8.0, 15, "rim", "2, 0, 14, 3")).append(",\n");
            m.append(boxAll(1, 7.0, 3, 2.6, 8.0, 13, "rim", "0, 2, 3, 14")).append(",\n");
            m.append(boxAll(13.4, 7.0, 3, 15, 8.0, 13, "rim", "13, 2, 16, 14"));
            if (topYs[level] > 0) {
                m.append(",\n    {\n      \"from\": [2.5, 1.5, 2.5],\n      \"to\": [13.5, ").append(topYs[level]).append(", 13.5],\n");
                m.append("      \"faces\": {\n");
                m.append("        \"up\": {\"texture\": \"#fill\", \"uv\": [1, 1, 15, 15]},\n");
                m.append("        \"north\": {\"texture\": \"#fill\", \"uv\": [1, 4, 15, 12]},\n");
                m.append("        \"south\": {\"texture\": \"#fill\", \"uv\": [1, 4, 15, 12]},\n");
                m.append("        \"east\": {\"texture\": \"#fill\", \"uv\": [1, 4, 15, 12]},\n");
                m.append("        \"west\": {\"texture\": \"#fill\", \"uv\": [1, 4, 15, 12]}\n      }\n    }");
            }
            m.append("\n  ],\n  \"display\": {\n");
            m.append("    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 1.5, 0], \"scale\": [0.65, 0.65, 0.65]},\n");
            m.append("    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 0], \"scale\": [0.25, 0.25, 0.25]},\n");
            m.append("    \"fixed\": {\"rotation\": [0, 0, 0], \"translation\": [0, 1, 0], \"scale\": [0.5, 0.5, 0.5]}\n  }\n}");
            saveJson(MODELS_BLOCK.resolve("manual_composter_" + level + ".json"), m.toString());

            bs.append("    \"fill=").append(level).append("\": {\"model\": \"cropmanager:manual_composter_").append(level).append("\"}");
            bs.append(level < 5 ? ",\n" : "\n");
        }
        bs.append("  }\n}");
        saveJson(BLOCKSTATES.resolve("manual_composter.json"), bs.toString());
        saveJson(MODELS_ITEM.resolve("manual_composter.json"), "{\"parent\": \"cropmanager:block/manual_composter_5\"}");
        System.out.println("Generated Manual Composter half-height assets.");
    }

    private static String boxAll(double x0, double y0, double z0, double x1, double y1, double z1, String tex, String uv) {
        String face = "{\"texture\": \"#" + tex + "\", \"uv\": [" + uv + "]}";
        return "    {\n      \"from\": [" + x0 + ", " + y0 + ", " + z0 + "],\n      \"to\": [" + x1 + ", " + y1 + ", " + z1 + "],\n"
                + "      \"faces\": {\"up\": " + face + ", \"down\": " + face + ", \"north\": " + face
                + ", \"south\": " + face + ", \"east\": " + face + ", \"west\": " + face + "}\n    }";
    }

    private static String plankNs(double y0, double y1, double z0, double z1) {
        return "    {\n      \"from\": [3, " + y0 + ", " + z0 + "],\n      \"to\": [13, " + y1 + ", " + z1 + "],\n"
                + "      \"faces\": {\n"
                + "        \"north\": {\"texture\": \"#wood\", \"uv\": [0, 0, 16, 8]},\n"
                + "        \"south\": {\"texture\": \"#wood\", \"uv\": [0, 0, 16, 8]},\n"
                + "        \"up\": {\"texture\": \"#wood\", \"uv\": [0, 0, 16, 2]},\n"
                + "        \"down\": {\"texture\": \"#wood\", \"uv\": [0, 14, 16, 16]}\n      }\n    }";
    }

    private static String plankEw(double y0, double y1, double x0, double x1) {
        return "    {\n      \"from\": [" + x0 + ", " + y0 + ", 3],\n      \"to\": [" + x1 + ", " + y1 + ", 13],\n"
                + "      \"faces\": {\n"
                + "        \"east\": {\"texture\": \"#wood\", \"uv\": [0, 0, 16, 8]},\n"
                + "        \"west\": {\"texture\": \"#wood\", \"uv\": [0, 0, 16, 8]},\n"
                + "        \"up\": {\"texture\": \"#wood\", \"uv\": [0, 0, 16, 2]},\n"
                + "        \"down\": {\"texture\": \"#wood\", \"uv\": [0, 14, 16, 16]}\n      }\n    }";
    }

    private static void drawBasePipe(BufferedImage im, int collarOuter, int collarInner, int stripeDark, int stripeHi) {
        Graphics2D g = im.createGraphics();
        fill(g, 0, 0, 15, 15, 0xFF1C2226); // shadow
        fill(g, 2, 2, 13, 13, 0xFF707A80); // steel
        fill(g, 4, 4, 11, 11, 0xFF5B656B); // mid
        fill(g, 5, 0, 10, 15, 0xFF465056); // edge
        fill(g, 6, 0, 9, 15, 0xFF707A80);  // steel tube
        fill(g, 6, 0, 6, 15, 0xFFB0B8BC);  // highlight
        fill(g, 5, 1, 10, 2, collarOuter);
        fill(g, 5, 13, 10, 14, collarOuter);
        fill(g, 6, 1, 9, 1, collarInner);
        fill(g, 6, 14, 9, 14, collarInner);
        fill(g, 7, 4, 8, 11, stripeDark);
        fill(g, 7, 5, 7, 10, stripeHi);
        g.dispose();
    }

    private static void fill(Graphics2D g, int x0, int y0, int x1, int y1, int argb) {
        g.setColor(new Color(argb, true));
        g.fillRect(x0, y0, (x1 - x0) + 1, (y1 - y0) + 1);
    }

    private static void generateFluidPipe() throws IOException {
        // Exact same metallic/copper layout as thermal_pipe.png, with subtle water-cyan stripe & collar accent
        BufferedImage pipe = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        drawBasePipe(pipe, 0xFF945B2A, 0xFFC89D41, 0xFF1F78B4, 0xFF73C1E4);
        // Subtle cyan water droplet highlight at center (7..8, 7..8) to distinguish from thermal_pipe in inventory
        pipe.setRGB(7, 7, 0xFF8DEEF7);
        pipe.setRGB(8, 8, 0xFF38B6C8);
        savePng(TEX_BLOCKS.resolve("fluid_pipe.png"), pipe);

        BufferedImage collar = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = collar.createGraphics();
        fill(g, 0, 0, 15, 15, 0xFF1C2226);
        fill(g, 1, 1, 14, 14, 0xFF2D7EA4);
        fill(g, 2, 2, 13, 13, 0xFF61B6CF);
        fill(g, 3, 3, 12, 12, 0xFF465056);
        fill(g, 4, 4, 11, 11, 0xFF2B3237);
        fill(g, 6, 6, 9, 9, 0xFF707A80);
        g.dispose();
        collar.setRGB(7, 7, 0xFF8DEEF7);
        savePng(TEX_BLOCKS.resolve("fluid_pipe_collar.png"), collar);

        saveJson(BLOCKSTATES.resolve("fluid_pipe.json"), "{\n"
                + "  \"multipart\": [\n"
                + "    { \"apply\": { \"model\": \"cropmanager:fluid_pipe_core\" } },\n"
                + "    { \"when\": { \"north\": true }, \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\" } },\n"
                + "    { \"when\": { \"east\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 90 } },\n"
                + "    { \"when\": { \"south\": true }, \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 180 } },\n"
                + "    { \"when\": { \"west\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 270 } },\n"
                + "    { \"when\": { \"up\": true },    \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"x\": 270 } },\n"
                + "    { \"when\": { \"down\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"x\": 90 } }\n"
                + "  ]\n}");
        saveJson(MODELS_BLOCK.resolve("fluid_pipe_core.json"), pipeCoreJson("cropmanager:blocks/fluid_pipe"));
        saveJson(MODELS_BLOCK.resolve("fluid_pipe_arm.json"), pipeArmJson("cropmanager:blocks/fluid_pipe"));
        saveJson(MODELS_BLOCK.resolve("fluid_pipe.json"), pipeInventoryJson("cropmanager:blocks/fluid_pipe", "cropmanager:blocks/fluid_pipe_collar"));
        saveJson(MODELS_ITEM.resolve("fluid_pipe.json"), "{\n"
                + "  \"parent\": \"cropmanager:block/fluid_pipe\",\n"
                + "  \"display\": {\n"
                + "    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.625, 0.625, 0.625]},\n"
                + "    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 0], \"scale\": [0.25, 0.25, 0.25]},\n"
                + "    \"thirdperson_righthand\": {\"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": [0.375, 0.375, 0.375]},\n"
                + "    \"firstperson_righthand\": {\"rotation\": [0, 45, 0], \"translation\": [0, 0, 0], \"scale\": [0.4, 0.4, 0.4]}\n"
                + "  }\n}");
        System.out.println("Generated Fluid Pipe assets.");
    }

    private static void generateFluidRestrictor() throws IOException {
        for (boolean on : new boolean[] {false, true}) {
            BufferedImage core = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            int ringOuter = on ? 0xFFC93424 : 0xFF4A1818;
            int ringInner = on ? 0xFFFF5555 : 0xFF6E2222;
            int center = on ? 0xFF8DEEF7 : 0xFF2B3237;
            drawBasePipe(core, ringOuter, ringInner, on ? 0xFFC93424 : 0xFF4A1818, on ? 0xFFFF7766 : 0xFF6E2222);
            Graphics2D g = core.createGraphics();
            fill(g, 5, 5, 10, 10, 0xFF2B3237);
            fill(g, 6, 6, 9, 9, ringOuter);
            fill(g, 7, 7, 8, 8, center);
            if (on) {
                core.setRGB(6, 6, 0xFFFF9988);
            }
            g.dispose();
            savePng(TEX_BLOCKS.resolve(on ? "fluid_restrictor_on.png" : "fluid_restrictor_off.png"), core);
        }

        saveJson(BLOCKSTATES.resolve("fluid_restrictor.json"), "{\n"
                + "  \"multipart\": [\n"
                + "    { \"when\": { \"powered\": false }, \"apply\": { \"model\": \"cropmanager:fluid_restrictor_core_off\" } },\n"
                + "    { \"when\": { \"powered\": true },  \"apply\": { \"model\": \"cropmanager:fluid_restrictor_core_on\" } },\n"
                + "    { \"when\": { \"north\": true }, \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\" } },\n"
                + "    { \"when\": { \"east\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 90 } },\n"
                + "    { \"when\": { \"south\": true }, \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 180 } },\n"
                + "    { \"when\": { \"west\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 270 } },\n"
                + "    { \"when\": { \"up\": true },    \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"x\": 270 } },\n"
                + "    { \"when\": { \"down\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"x\": 90 } }\n"
                + "  ]\n}");
        saveJson(MODELS_BLOCK.resolve("fluid_restrictor_core_off.json"), pipeCoreJson("cropmanager:blocks/fluid_restrictor_off"));
        saveJson(MODELS_BLOCK.resolve("fluid_restrictor_core_on.json"), pipeCoreJson("cropmanager:blocks/fluid_restrictor_on"));
        saveJson(MODELS_BLOCK.resolve("fluid_restrictor.json"), pipeInventoryJson("cropmanager:blocks/fluid_restrictor_off", "cropmanager:blocks/fluid_restrictor_on"));
        saveJson(MODELS_ITEM.resolve("fluid_restrictor.json"), "{\n"
                + "  \"parent\": \"cropmanager:block/fluid_restrictor\",\n"
                + "  \"display\": {\n"
                + "    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.625, 0.625, 0.625]},\n"
                + "    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 0], \"scale\": [0.25, 0.25, 0.25]},\n"
                + "    \"thirdperson_righthand\": {\"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": [0.375, 0.375, 0.375]},\n"
                + "    \"firstperson_righthand\": {\"rotation\": [0, 45, 0], \"translation\": [0, 0, 0], \"scale\": [0.4, 0.4, 0.4]}\n"
                + "  }\n}");
        System.out.println("Generated Fluid Restrictor assets.");
    }

    private static void generateFluidRouter() throws IOException {
        // Side texture with crisp cyan/gold arrow pointing UP (uv-rotated in model toward North / output face)
        BufferedImage side = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gs = side.createGraphics();
        fill(gs, 0, 0, 15, 15, 0xFF1C2226);
        fill(gs, 1, 1, 14, 14, 0xFF707A80);
        fill(gs, 2, 2, 13, 13, 0xFF465056);
        fill(gs, 3, 3, 12, 12, 0xFF2B3237);
        // Upward directional arrow in center (x=5..10, y=4..11) -> points toward North when mapped on UP/DOWN or rotated on sides
        fill(gs, 7, 4, 8, 11, 0xFF38B6C8);
        fill(gs, 6, 5, 9, 6, 0xFF8DEEF7);
        fill(gs, 5, 6, 10, 7, 0xFFF2C94C);
        gs.dispose();
        savePng(TEX_BLOCKS.resolve("fluid_router_side.png"), side);

        // Output nozzle face (gold + bright cyan discharge port)
        BufferedImage out = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D go = out.createGraphics();
        fill(go, 0, 0, 15, 15, 0xFF1C2226);
        fill(go, 2, 2, 13, 13, 0xFFC89D41);
        fill(go, 3, 3, 12, 12, 0xFFF2C94C);
        fill(go, 5, 5, 10, 10, 0xFF111619);
        fill(go, 6, 6, 9, 9, 0xFF38B6C8);
        fill(go, 7, 7, 8, 8, 0xFF8DEEF7);
        go.dispose();
        savePng(TEX_BLOCKS.resolve("fluid_router_out.png"), out);

        // Input hub face (steel + dark blue intake)
        BufferedImage in = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gi = in.createGraphics();
        fill(gi, 0, 0, 15, 15, 0xFF1C2226);
        fill(gi, 2, 2, 13, 13, 0xFF707A80);
        fill(gi, 4, 4, 11, 11, 0xFF465056);
        fill(gi, 6, 6, 9, 9, 0xFF2D7EA4);
        gi.dispose();
        savePng(TEX_BLOCKS.resolve("fluid_router_in.png"), in);

        // Core hub [4,4,4]..[12,12,12] + output nozzle [5,5,0]..[11,11,4] pointing North (z=0)
        String routerCore = "{\n"
                + "  \"ambientocclusion\": true,\n"
                + "  \"textures\": {\n"
                + "    \"side\": \"cropmanager:blocks/fluid_router_side\",\n"
                + "    \"out\": \"cropmanager:blocks/fluid_router_out\",\n"
                + "    \"in\": \"cropmanager:blocks/fluid_router_in\",\n"
                + "    \"particle\": \"cropmanager:blocks/fluid_router_side\"\n"
                + "  },\n"
                + "  \"elements\": [\n"
                + "    {\n"
                + "      \"from\": [4, 4, 4],\n"
                + "      \"to\": [12, 12, 12],\n"
                + "      \"faces\": {\n"
                + "        \"north\": { \"uv\": [2, 2, 14, 14], \"texture\": \"#out\" },\n"
                + "        \"south\": { \"uv\": [2, 2, 14, 14], \"texture\": \"#in\" },\n"
                + "        \"up\":    { \"uv\": [2, 2, 14, 14], \"texture\": \"#side\" },\n"
                + "        \"down\":  { \"uv\": [2, 2, 14, 14], \"texture\": \"#side\", \"rotation\": 180 },\n"
                + "        \"west\":  { \"uv\": [2, 2, 14, 14], \"texture\": \"#side\", \"rotation\": 270 },\n"
                + "        \"east\":  { \"uv\": [2, 2, 14, 14], \"texture\": \"#side\", \"rotation\": 90 }\n"
                + "      }\n"
                + "    },\n"
                + "    {\n"
                + "      \"from\": [5, 5, 0],\n"
                + "      \"to\": [11, 11, 4],\n"
                + "      \"faces\": {\n"
                + "        \"north\": { \"uv\": [2, 2, 14, 14], \"texture\": \"#out\" },\n"
                + "        \"up\":    { \"uv\": [3, 2, 13, 6],  \"texture\": \"#out\" },\n"
                + "        \"down\":  { \"uv\": [3, 2, 13, 6],  \"texture\": \"#out\" },\n"
                + "        \"west\":  { \"uv\": [3, 2, 13, 6],  \"texture\": \"#out\" },\n"
                + "        \"east\":  { \"uv\": [3, 2, 13, 6],  \"texture\": \"#out\" }\n"
                + "      }\n"
                + "    }\n"
                + "  ],\n"
                + "  \"display\": {\n"
                + "    \"gui\": {\"rotation\": [30, 225, 0], \"translation\": [0, 0, 0], \"scale\": [0.65, 0.65, 0.65]},\n"
                + "    \"ground\": {\"rotation\": [0, 0, 0], \"translation\": [0, 3, 0], \"scale\": [0.3, 0.3, 0.3]},\n"
                + "    \"thirdperson_righthand\": {\"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": [0.4, 0.4, 0.4]},\n"
                + "    \"firstperson_righthand\": {\"rotation\": [0, 45, 0], \"translation\": [0, 0, 0], \"scale\": [0.45, 0.45, 0.45]}\n"
                + "  }\n}";
        saveJson(MODELS_BLOCK.resolve("fluid_router_core.json"), routerCore);

        saveJson(BLOCKSTATES.resolve("fluid_router.json"), "{\n"
                + "  \"multipart\": [\n"
                + "    { \"when\": { \"facing\": \"north\" }, \"apply\": { \"model\": \"cropmanager:fluid_router_core\" } },\n"
                + "    { \"when\": { \"facing\": \"east\" },  \"apply\": { \"model\": \"cropmanager:fluid_router_core\", \"y\": 90 } },\n"
                + "    { \"when\": { \"facing\": \"south\" }, \"apply\": { \"model\": \"cropmanager:fluid_router_core\", \"y\": 180 } },\n"
                + "    { \"when\": { \"facing\": \"west\" },  \"apply\": { \"model\": \"cropmanager:fluid_router_core\", \"y\": 270 } },\n"
                + "    { \"when\": { \"facing\": \"up\" },    \"apply\": { \"model\": \"cropmanager:fluid_router_core\", \"x\": 270 } },\n"
                + "    { \"when\": { \"facing\": \"down\" },  \"apply\": { \"model\": \"cropmanager:fluid_router_core\", \"x\": 90 } },\n"
                + "    { \"when\": { \"north\": true }, \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\" } },\n"
                + "    { \"when\": { \"east\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 90 } },\n"
                + "    { \"when\": { \"south\": true }, \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 180 } },\n"
                + "    { \"when\": { \"west\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"y\": 270 } },\n"
                + "    { \"when\": { \"up\": true },    \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"x\": 270 } },\n"
                + "    { \"when\": { \"down\": true },  \"apply\": { \"model\": \"cropmanager:fluid_pipe_arm\", \"x\": 90 } }\n"
                + "  ]\n}");
        saveJson(MODELS_ITEM.resolve("fluid_router.json"), "{\"parent\": \"cropmanager:block/fluid_router_core\"}");
        System.out.println("Generated Fluid Router assets.");
    }

    private static String pipeCoreJson(String tex) {
        return "{\n  \"ambientocclusion\": true,\n  \"textures\": {\n"
                + "    \"pipe\": \"" + tex + "\",\n    \"particle\": \"" + tex + "\"\n  },\n"
                + "  \"elements\": [\n    {\n      \"from\": [5, 5, 5],\n      \"to\": [11, 11, 11],\n"
                + "      \"faces\": {\n"
                + "        \"down\":  { \"uv\": [5, 5, 11, 11], \"texture\": \"#pipe\" },\n"
                + "        \"up\":    { \"uv\": [5, 5, 11, 11], \"texture\": \"#pipe\" },\n"
                + "        \"north\": { \"uv\": [5, 5, 11, 11], \"texture\": \"#pipe\" },\n"
                + "        \"south\": { \"uv\": [5, 5, 11, 11], \"texture\": \"#pipe\" },\n"
                + "        \"west\":  { \"uv\": [5, 5, 11, 11], \"texture\": \"#pipe\" },\n"
                + "        \"east\":  { \"uv\": [5, 5, 11, 11], \"texture\": \"#pipe\" }\n"
                + "      }\n    }\n  ]\n}";
    }

    private static String pipeArmJson(String tex) {
        return "{\n  \"ambientocclusion\": true,\n  \"textures\": {\n"
                + "    \"pipe\": \"" + tex + "\",\n    \"particle\": \"" + tex + "\"\n  },\n"
                + "  \"elements\": [\n    {\n      \"from\": [6, 6, 0],\n      \"to\": [10, 10, 5],\n"
                + "      \"faces\": {\n"
                + "        \"down\":  { \"uv\": [6, 0, 10, 5], \"texture\": \"#pipe\" },\n"
                + "        \"up\":    { \"uv\": [6, 0, 10, 5], \"texture\": \"#pipe\" },\n"
                + "        \"north\": { \"uv\": [6, 6, 10, 10], \"texture\": \"#pipe\", \"cullface\": \"north\" },\n"
                + "        \"west\":  { \"uv\": [0, 6, 5, 10], \"texture\": \"#pipe\" },\n"
                + "        \"east\":  { \"uv\": [11, 6, 16, 10], \"texture\": \"#pipe\" }\n"
                + "      }\n    }\n  ]\n}";
    }

    private static String pipeInventoryJson(String pipeTex, String collarTex) {
        return "{\n  \"ambientocclusion\": true,\n  \"textures\": {\n"
                + "    \"pipe\": \"" + pipeTex + "\",\n"
                + "    \"collar\": \"" + collarTex + "\",\n"
                + "    \"particle\": \"" + pipeTex + "\"\n  },\n"
                + "  \"elements\": [\n"
                + "    { \"from\": [5, 5, 5], \"to\": [11, 11, 11], \"faces\": {"
                + "\"down\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"up\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"north\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"south\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"west\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"east\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}} },\n"
                + "    { \"from\": [6, 6, 0], \"to\": [10, 10, 5], \"faces\": {"
                + "\"down\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\"}, "
                + "\"up\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\"}, "
                + "\"north\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"south\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"west\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\", \"rotation\": 90}, "
                + "\"east\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\", \"rotation\": 90}} },\n"
                + "    { \"from\": [6, 6, 11], \"to\": [10, 10, 16], \"faces\": {"
                + "\"down\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\"}, "
                + "\"up\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\"}, "
                + "\"north\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"south\": {\"uv\": [0,0,16,16], \"texture\": \"#collar\"}, "
                + "\"west\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\", \"rotation\": 90}, "
                + "\"east\": {\"uv\": [0,0,16,16], \"texture\": \"#pipe\", \"rotation\": 90}} }\n"
                + "  ]\n}";
    }
}
