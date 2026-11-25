package br.cropmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

/** Renders a multiblock OBJ/MTL mesh in controller-relative coordinates with a single-column glass level fill. */
@SideOnly(Side.CLIENT)
final class MultiblockObjModel {
    private final String modelPath;
    private final double fillMinY;
    private final double fillMaxY;
    private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();
    private final Map<String, List<Vertex[]>> groups = new LinkedHashMap<>();
    private boolean loaded;

    MultiblockObjModel(String modelPath, double fillMinY, double fillMaxY) {
        this.modelPath = modelPath;
        this.fillMinY = fillMinY;
        this.fillMaxY = fillMaxY;
    }

    void render(double fillFraction, int fillRgb) {
        if (!loaded) {
            loaded = true;
            try {
                readMaterials();
                readMesh();
            } catch (IOException | RuntimeException error) {
                CropManager.log.error("Could not load multiblock OBJ: " + modelPath, error);
                groups.clear();
            }
        }
        GlStateManager.disableLighting();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        for (Map.Entry<String, List<Vertex[]>> group : groups.entrySet()) {
            boolean glass = "glass".equals(group.getKey());
            ResourceLocation tex = textures.get(group.getKey());
            if (tex == null) continue;
            Minecraft.getMinecraft().getTextureManager().bindTexture(tex);
            if (glass) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (Vertex[] face : group.getValue()) {
                int shade = shade(face);
                for (Vertex v : face) {
                    buffer.pos(v.x, v.y, v.z).tex(v.u, v.v).color(shade, shade, shade, 255).endVertex();
                }
            }
            Tessellator.getInstance().draw();
            if (glass) {
                if (fillFraction > 0.0001) {
                    double clamped = Math.max(0.0, Math.min(1.0, fillFraction));
                    double top = fillMinY + (fillMaxY - fillMinY) * clamped;
                    GlStateManager.disableTexture2D();
                    GlStateManager.enablePolygonOffset();
                    GlStateManager.doPolygonOffset(-1.0F, -1.0F);
                    buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                    for (Vertex[] face : group.getValue()) {
                        drawLevel(buffer, face, top, fillRgb);
                    }
                    Tessellator.getInstance().draw();
                    GlStateManager.disablePolygonOffset();
                    GlStateManager.enableTexture2D();
                }
                GlStateManager.disableBlend();
            }
        }
        GlStateManager.enableLighting();
    }

    private static int shade(Vertex[] face) {
        Vertex a = face[0], b = face[1], c = face[2];
        double x = (b.y - a.y) * (c.z - a.z) - (b.z - a.z) * (c.y - a.y);
        double y = (b.z - a.z) * (c.x - a.x) - (b.x - a.x) * (c.z - a.z);
        return Math.abs(y) > 0.00001 ? 255 : Math.abs(x) > 0.00001 ? 196 : 225;
    }

    private static void drawLevel(BufferBuilder buffer, Vertex[] face, double topY, int rgb) {
        List<Vertex> polygon = clipBelowY(new ArrayList<>(Arrays.asList(face)), topY);
        if (polygon.size() < 3) return;
        int r = (rgb >> 16) & 255;
        int g = (rgb >> 8) & 255;
        int b = rgb & 255;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            Vertex v0 = polygon.get(0), v1 = polygon.get(i), v2 = polygon.get(i + 1);
            buffer.pos(v0.x, v0.y, v0.z).color(r, g, b, 210).endVertex();
            buffer.pos(v1.x, v1.y, v1.z).color(r, g, b, 210).endVertex();
            buffer.pos(v2.x, v2.y, v2.z).color(r, g, b, 210).endVertex();
        }
    }

    private static List<Vertex> clipBelowY(List<Vertex> input, double limitY) {
        List<Vertex> output = new ArrayList<>();
        if (input.isEmpty()) return output;
        Vertex before = input.get(input.size() - 1);
        for (Vertex after : input) {
            double a = before.y - limitY;
            double b = after.y - limitY;
            boolean beforeInside = a <= 0;
            boolean afterInside = b <= 0;
            if (beforeInside != afterInside) {
                double t = a / (a - b);
                output.add(new Vertex(
                        before.x + t * (after.x - before.x),
                        before.y + t * (after.y - before.y),
                        before.z + t * (after.z - before.z), 0, 0));
            }
            if (afterInside) output.add(after);
            before = after;
        }
        return output;
    }

    private IResource resource(String suffix) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(CropManager.ID, modelPath + suffix));
    }

    private void readMaterials() throws IOException {
        try (IResource resource = resource(".mtl");
                BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String material = "";
            for (String line; (line = reader.readLine()) != null;) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] fields = trimmed.split("\\s+");
                if ("newmtl".equals(fields[0]) && fields.length > 1) material = fields[1];
                if ("map_Kd".equals(fields[0]) && fields.length > 1) {
                    ResourceLocation sprite = new ResourceLocation(fields[1]);
                    textures.put(material, new ResourceLocation(sprite.getNamespace(), "textures/" + sprite.getPath() + ".png"));
                }
            }
        }
    }

    private void readMesh() throws IOException {
        List<double[]> positions = new ArrayList<>(), uv = new ArrayList<>();
        List<Vertex[]> faces = null;
        try (IResource resource = resource(".obj");
                BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                String[] fields = trimmed.split("\\s+");
                switch (fields[0]) {
                    case "v":
                        positions.add(new double[] {Double.parseDouble(fields[1]), Double.parseDouble(fields[2]), Double.parseDouble(fields[3])});
                        break;
                    case "vt":
                        uv.add(new double[] {Double.parseDouble(fields[1]), 1.0 - Double.parseDouble(fields[2])});
                        break;
                    case "usemtl":
                        if (!textures.containsKey(fields[1])) throw new IOException("Unknown OBJ material: " + fields[1]);
                        faces = groups.get(fields[1]);
                        if (faces == null) {
                            faces = new ArrayList<>();
                            groups.put(fields[1], faces);
                        }
                        break;
                    case "f":
                        if (faces == null || fields.length != 5) throw new IOException("Expected a textured quadrilateral");
                        Vertex[] face = new Vertex[4];
                        for (int i = 0; i < face.length; i++) {
                            String[] indices = fields[i + 1].split("/");
                            double[] p = positions.get(Integer.parseInt(indices[0]) - 1);
                            double[] t = uv.get(Integer.parseInt(indices[1]) - 1);
                            face[i] = new Vertex(p[0], p[1], p[2], t[0], t[1]);
                        }
                        faces.add(face);
                        break;
                    default:
                        break;
                }
            }
        }
        List<Vertex[]> glass = groups.remove("glass");
        if (glass != null) groups.put("glass", glass);
    }

    private static final class Vertex {
        final double x, y, z, u, v;

        Vertex(double x, double y, double z, double u, double v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }
    }
}
