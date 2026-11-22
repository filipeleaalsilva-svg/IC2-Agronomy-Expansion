package br.cropmanager;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.junit.Test;
import static org.junit.Assert.*;

/** Verifies models, blockstates, textures, GUI dimensions, and translations on disk. */
public final class AssetIntegrityTest {
    private static final Path ASSETS = Paths.get("src", "main", "resources", "assets", "cropmanager");
    private static final Pattern MODEL_REF = Pattern.compile("\"(?:model|parent)\"\\s*:\\s*\"cropmanager:([^\"]+)\"");
    private static final Pattern TEXTURE_REF = Pattern.compile("\"cropmanager:((?:blocks|items)/[^\"]+)\"");
    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Test
    public void everyBlockAndItemHasModelsAndTextures() throws IOException {
        String[] blocks = {
                "advanced_crop_manager", "agricultural_monitor", "electric_composter",
                "biogas_weeder", "weed_ex_fabricator", "grin_grinder", "grin_extractor",
                "thermal_pipe", "seed_analyzer", "manual_composter", "radioactive_soil",
                "sprinkler_mk1", "sprinkler_mk2", "sprinkler_mk3", "crop_database_chest"
        };
        for (String block : blocks) {
            Path bs = ASSETS.resolve("blockstates").resolve(block + ".json");
            assertTrue("Missing blockstate: " + bs, Files.isRegularFile(bs));
            verifyJsonRefs(bs, true);

            Path itemModel = ASSETS.resolve("models").resolve("item").resolve(block + ".json");
            assertTrue("Missing item model for block: " + itemModel, Files.isRegularFile(itemModel));
            verifyJsonRefs(itemModel, false);
        }

        String[] items = {
                "agricultural_probe", "weed_killer_upgrade", "wireless_range_upgrade",
                "harvest_transfer_upgrade", "wireless_eu_upgrade", "grinding_blade",
                "organic_compost", "catalytic_fertilizer", "nitro_essence", "bio_fiber",
                "portable_crop_database", "upgrade_linker", "mutagenic_powder"
        };
        for (String item : items) {
            Path itemModel = ASSETS.resolve("models").resolve("item").resolve(item + ".json");
            assertTrue("Missing item model: " + itemModel, Files.isRegularFile(itemModel));
            verifyJsonRefs(itemModel, false);
        }
    }

    @Test
    public void guiTexturesHaveExpectedDimensions() throws IOException {
        String[] gui512 = {
                "equipment_analyzer", "equipment_composter", "equipment_extractor", "equipment_grinder",
                "equipment_legacy", "equipment_mixer", "equipment_monitor",
                "equipment_monitor_compact", "equipment_weeder_compact", "equipment_weeder_expanded",
                "manager_advanced", "manager_advanced_compact", "manager_basic"
        };
        for (String name : gui512) {
            assertPngSize(ASSETS.resolve("textures").resolve("gui").resolve(name + ".png"), 512, 512);
        }
        assertPngSize(ASSETS.resolve("textures").resolve("gui").resolve("crop_database_chest.png"), 256, 256);
        assertPngSize(ASSETS.resolve("textures").resolve("gui").resolve("crop_database_drawer.png"), 128, 128);
        assertPngSize(ASSETS.resolve("textures").resolve("gui").resolve("icons.png"), 64, 64);
        assertPngSize(ASSETS.resolve("textures").resolve("gui").resolve("ic2_widgets.png"), 256, 256);
        assertPngSize(ASSETS.resolve("textures").resolve("blocks").resolve("grin_grinder_front_active.png"), 16, 64);
        assertPngSize(ASSETS.resolve("textures").resolve("blocks").resolve("grin_extractor_front_active.png"), 16, 64);
        assertPngSize(ASSETS.resolve("textures").resolve("blocks").resolve("seed_analyzer_front_active.png"), 16, 64);
        assertTrue("Missing mcmeta for grin_grinder_front_active", Files.isRegularFile(ASSETS.resolve("textures").resolve("blocks").resolve("grin_grinder_front_active.png.mcmeta")));
        assertTrue("Missing mcmeta for grin_extractor_front_active", Files.isRegularFile(ASSETS.resolve("textures").resolve("blocks").resolve("grin_extractor_front_active.png.mcmeta")));
        assertTrue("Missing mcmeta for seed_analyzer_front_active", Files.isRegularFile(ASSETS.resolve("textures").resolve("blocks").resolve("seed_analyzer_front_active.png.mcmeta")));
        String[] customCrops = {
                "grinbloom", "bioreed", "nitrobulb", "lazuli_bloom", "irid_moss",
                "radbloom", "glowbloom", "quartzreed", "lithivine", "sulfurbud"
        };
        for (String crop : customCrops) {
            for (int stage = 1; stage <= 4; stage++) {
                assertPngSize(ASSETS.resolve("textures").resolve("blocks").resolve("crops").resolve(crop + "_" + stage + ".png"), 16, 16);
            }
        }
    }

    @Test
    public void enUsAndPtBrHaveAllIntegratedKeys() throws IOException {
        Set<String> enKeys = loadLangKeys(ASSETS.resolve("lang").resolve("en_us.lang"));
        Set<String> ptKeys = loadLangKeys(ASSETS.resolve("lang").resolve("pt_br.lang"));
        String[] required = {
                "tile.cropmanager.crop_database_chest.name",
                "item.cropmanager.portable_crop_database.name",
                "item.cropmanager.upgrade_linker.name",
                "cropmanager.linker.bound",
                "cropmanager.linker.applied",
                "cropmanager.linker.unavailable",
                "cropdatabasechest.info.discovered_by",
                "cropdatabasechest.info.scan",
                "cropdatabasechest.info.attributes",
                "cropdatabasechest.mode.chest",
                "cropdatabasechest.mode.portable",
                "cropdatabasechest.mode.linked",
                "cropmanager.extract_short",
                "tile.cropmanager.manual_composter.name",
                "tile.cropmanager.seed_analyzer.name",
                "item.cropmanager.organic_compost.name",
                "item.cropmanager.catalytic_fertilizer.name",
                "item.cropmanager.mutagenic_powder.name",
                "cropmanager.mutagenic_powder_help",
                "item.cropmanager.nitro_essence.name",
                "item.cropmanager.bio_fiber.name",
                "cropmanager.crop.grinbloom",
                "cropmanager.crop.bioreed",
                "cropmanager.crop.nitrobulb",
                "cropmanager.crop.lazuli_bloom",
                "cropmanager.crop.irid_moss",
                "cropmanager.crop.radbloom",
                "cropmanager.crop.glowbloom",
                "cropmanager.crop.quartzreed",
                "cropmanager.crop.lithivine",
                "cropmanager.crop.sulfurbud",
                "tile.cropmanager.radioactive_soil.name",
                "tile.cropmanager.sprinkler_mk1.name",
                "tile.cropmanager.sprinkler_mk2.name",
                "tile.cropmanager.sprinkler_mk3.name",
                "cropmanager.jei_crops_title"
        };
        for (String key : required) {
            assertTrue("Missing en_us key: " + key, enKeys.contains(key));
            assertTrue("Missing pt_br key: " + key, ptKeys.contains(key));
        }
    }

    @Test
    public void enUsAndPtBrHaveExactKeyParity() throws IOException {
        Set<String> enKeys = loadLangKeys(ASSETS.resolve("lang").resolve("en_us.lang"));
        Set<String> ptKeys = loadLangKeys(ASSETS.resolve("lang").resolve("pt_br.lang"));
        assertEquals("Translation keys count mismatch", enKeys.size(), ptKeys.size());
        assertEquals("Translation keys mismatch", enKeys, ptKeys);
    }

    private static void verifyJsonRefs(Path jsonFile, boolean isBlockstate) throws IOException {
        String text = new String(Files.readAllBytes(jsonFile), StandardCharsets.UTF_8);
        Matcher m = MODEL_REF.matcher(text);
        while (m.find()) {
            String target = m.group(1);
            String rel = (isBlockstate && !target.contains("/")) ? "block/" + target : target;
            Path modelFile = ASSETS.resolve("models").resolve(rel + ".json");
            assertTrue("Referenced model missing in " + jsonFile + ": " + modelFile, Files.isRegularFile(modelFile));
            verifyJsonRefs(modelFile, false);
        }
        Matcher t = TEXTURE_REF.matcher(text);
        while (t.find()) {
            String ref = t.group(1);
            Path tex = ASSETS.resolve("textures").resolve(ref + ".png");
            verifyReferencedTexture(jsonFile, ref, tex);
        }
    }

    private static void verifyReferencedTexture(Path jsonFile, String textureRef, Path tex) throws IOException {
        assertTrue("Referenced texture missing in " + jsonFile + ": " + tex, Files.isRegularFile(tex));
        int[] dims = assertPng8BitRgba(tex);
        int w = dims[0];
        int h = dims[1];
        if ("items/portable_crop_database".equals(textureRef)) {
            assertEquals("Expected 32px width for portable_crop_database: " + tex, 32, w);
            assertEquals("Expected 32px height for portable_crop_database: " + tex, 32, h);
            return;
        }
        if ("blocks/crop_database_chest_front".equals(textureRef)) {
            assertEquals("Expected 64px width for crop_database_chest_front: " + tex, 64, w);
            assertEquals("Expected 64px height for crop_database_chest_front: " + tex, 64, h);
            return;
        }
        boolean isStatic16 = w == 16 && h == 16;
        boolean isAnimatedStrip = w == 16 && h > 16 && h % 16 == 0;
        assertTrue("Non-canonical block/item texture dimensions (" + w + "x" + h + ") in " + tex
                + " (referenced by " + jsonFile + ")", isStatic16 || isAnimatedStrip);
        if (isAnimatedStrip) {
            verifyAnimatedMcmeta(tex);
        }
    }

    private static void assertPngSize(Path path, int expectedW, int expectedH) throws IOException {
        assertTrue("Missing PNG texture: " + path, Files.isRegularFile(path));
        int[] dims = assertPng8BitRgba(path);
        assertEquals("Width mismatch: " + path, expectedW, dims[0]);
        assertEquals("Height mismatch: " + path, expectedH, dims[1]);
        if (expectedW == 16 && expectedH > 16 && expectedH % 16 == 0) {
            verifyAnimatedMcmeta(path);
        }
    }

    private static int[] assertPng8BitRgba(Path path) throws IOException {
        byte[] header = new byte[26];
        try (InputStream in = Files.newInputStream(path)) {
            int offset = 0;
            while (offset < header.length) {
                int read = in.read(header, offset, header.length - offset);
                if (read < 0) break;
                offset += read;
            }
            assertEquals("Truncated PNG header: " + path, header.length, offset);
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            assertEquals("Invalid PNG signature byte " + i + ": " + path, PNG_SIGNATURE[i], header[i]);
        }
        assertEquals("Invalid IHDR chunk length: " + path, 13, readIntBE(header, 8));
        assertEquals("Missing IHDR chunk: " + path, "IHDR", new String(header, 12, 4, StandardCharsets.US_ASCII));

        int width = readIntBE(header, 16);
        int height = readIntBE(header, 20);
        int bitDepth = header[24] & 0xFF;
        int colorType = header[25] & 0xFF;

        assertEquals("PNG bit depth must be 8-bit: " + path, 8, bitDepth);
        assertEquals("PNG color type must be 6 (RGBA): " + path, 6, colorType);

        BufferedImage img = ImageIO.read(path.toFile());
        assertNotNull("Unreadable PNG: " + path, img);
        assertEquals("IHDR/ImageIO width mismatch: " + path, width, img.getWidth());
        assertEquals("IHDR/ImageIO height mismatch: " + path, height, img.getHeight());
        return new int[]{width, height};
    }

    private static void verifyAnimatedMcmeta(Path pngPath) throws IOException {
        Path mcmeta = pngPath.resolveSibling(pngPath.getFileName().toString() + ".mcmeta");
        assertTrue("Missing .mcmeta for animated texture: " + mcmeta, Files.isRegularFile(mcmeta));
        String content = new String(Files.readAllBytes(mcmeta), StandardCharsets.UTF_8);
        JsonElement rootEl = new JsonParser().parse(content);
        assertTrue("Invalid JSON object in " + mcmeta, rootEl != null && rootEl.isJsonObject());
        JsonObject root = rootEl.getAsJsonObject();
        assertTrue("Missing 'animation' object in " + mcmeta,
                root.has("animation") && root.get("animation").isJsonObject());
        JsonObject anim = root.getAsJsonObject("animation");
        assertTrue("Missing boolean 'interpolate' in " + mcmeta,
                anim.has("interpolate") && anim.get("interpolate").isJsonPrimitive()
                        && anim.get("interpolate").getAsJsonPrimitive().isBoolean());
        assertFalse("Animated texture must set \"interpolate\": false in " + mcmeta,
                anim.get("interpolate").getAsBoolean());
        assertTrue("Missing numeric 'frametime' in " + mcmeta,
                anim.has("frametime") && anim.get("frametime").isJsonPrimitive()
                        && anim.get("frametime").getAsJsonPrimitive().isNumber());
        assertTrue("Animated texture must set \"frametime\" > 0 in " + mcmeta,
                anim.get("frametime").getAsInt() > 0);
    }

    private static int readIntBE(byte[] buf, int offset) {
        return ((buf[offset] & 0xFF) << 24)
                | ((buf[offset + 1] & 0xFF) << 16)
                | ((buf[offset + 2] & 0xFF) << 8)
                | (buf[offset + 3] & 0xFF);
    }

    private static Set<String> loadLangKeys(Path lang) throws IOException {
        Set<String> keys = new HashSet<>();
        int lineNo = 0;
        for (String line : Files.readAllLines(lang, StandardCharsets.UTF_8)) {
            lineNo++;
            assertFalse("Replacement character (\\ufffd) in " + lang + " at line " + lineNo,
                    line.indexOf('\ufffd') >= 0);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
            int eq = trimmed.indexOf('=');
            assertTrue("Malformed lang line (missing '=') in " + lang + " at line " + lineNo + ": " + trimmed, eq > 0);
            String key = trimmed.substring(0, eq);
            String value = trimmed.substring(eq + 1).trim();
            assertFalse("Empty translation value for key '" + key + "' in " + lang + " at line " + lineNo,
                    value.isEmpty());
            assertTrue("Duplicate translation key '" + key + "' in " + lang + " at line " + lineNo,
                    keys.add(key));
        }
        return keys;
    }
}
