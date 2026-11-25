package br.cropmanager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import org.lwjgl.opengl.GL11;

/** Draws the supplied multiblock OBJ in controller-relative coordinates. */
final class ReservoirTowerModel {
    private static final String MODEL = "models/block/agricultural_reservoir";
    private final Map<String, ResourceLocation> textures = new LinkedHashMap<>();
    private final Map<String, List<Vertex[]>> groups = new LinkedHashMap<>();
    private boolean loaded;

    void render(FluidTank first, FluidTank second) {
        if (!loaded) {
            loaded = true;
            try {
                readMaterials();
                readMesh();
            } catch (IOException | RuntimeException error) {
                CropManager.log.error("Could not load the reservoir tower OBJ", error);
                groups.clear();
            }
        }
        GlStateManager.disableLighting();
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        for (Map.Entry<String, List<Vertex[]>> group : groups.entrySet()) {
            boolean glass = group.getKey().equals("glass");
            Minecraft.getMinecraft().getTextureManager().bindTexture(textures.get(group.getKey()));
            if (glass) {
                GlStateManager.enableBlend();
                GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            }
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            for (Vertex[] face : group.getValue()) {
                int shade = shade(face);
                for (Vertex v : face) buffer.pos(v.x, v.y, v.z).tex(v.u, v.v).color(shade, shade, shade, 255).endVertex();
            }
            Tessellator.getInstance().draw();
            if (glass) {
                GlStateManager.disableTexture2D();
                GlStateManager.enablePolygonOffset();
                GlStateManager.doPolygonOffset(-1, -1);
                buffer.begin(GL11.GL_TRIANGLES, DefaultVertexFormats.POSITION_COLOR);
                for (Vertex[] face : group.getValue()) {
                    drawLevel(buffer, face, first, true);
                    drawLevel(buffer, face, second, false);
                }
                Tessellator.getInstance().draw();
                GlStateManager.disablePolygonOffset();
                GlStateManager.enableTexture2D();
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

    private static void drawLevel(BufferBuilder buffer, Vertex[] face, FluidTank tank, boolean left) {
        FluidStack fluid = tank.getFluid();
        if (fluid == null || fluid.amount <= 0 || tank.getCapacity() <= 0) return;
        double top = 2.5 + (3.5625 - 2.5) * Math.min(1, (double) fluid.amount / tank.getCapacity());
        List<Vertex> polygon = new ArrayList<>(java.util.Arrays.asList(face));
        polygon = clip(polygon, top, false, true);
        polygon = clip(polygon, .5, true, left);
        if (polygon.size() < 3) return;
        int rgb = fluid.getFluid() == FluidRegistry.WATER ? 0x438cbd
                : fluid.getFluid() == ic2.core.ref.FluidName.weed_ex.getInstance() ? 0xb9a247 : fluid.getFluid().getColor(fluid);
        if ((rgb & 0xffffff) == 0xffffff) rgb = 0x83aa55;
        for (int i = 1; i + 1 < polygon.size(); i++) {
            colorVertex(buffer, polygon.get(0), rgb);
            colorVertex(buffer, polygon.get(i), rgb);
            colorVertex(buffer, polygon.get(i + 1), rgb);
        }
    }

    private static void colorVertex(BufferBuilder buffer, Vertex vertex, int color) {
        buffer.pos(vertex.x, vertex.y, vertex.z).color(color >> 16 & 255, color >> 8 & 255, color & 255, 205).endVertex();
    }

    private static List<Vertex> clip(List<Vertex> input, double limit, boolean alongX, boolean below) {
        List<Vertex> output = new ArrayList<>();
        if (input.isEmpty()) return output;
        Vertex before = input.get(input.size() - 1);
        for (Vertex after : input) {
            double a = (alongX ? before.x : before.y) - limit;
            double b = (alongX ? after.x : after.y) - limit;
            boolean beforeInside = below ? a <= 0 : a >= 0;
            boolean afterInside = below ? b <= 0 : b >= 0;
            if (beforeInside != afterInside) {
                double t = a / (a - b);
                output.add(new Vertex(before.x + t * (after.x - before.x),
                        before.y + t * (after.y - before.y), before.z + t * (after.z - before.z), 0, 0));
            }
            if (afterInside) output.add(after);
            before = after;
        }
        return output;
    }

    private IResource resource(String suffix) throws IOException {
        return Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(CropManager.ID, MODEL + suffix));
    }

    private void readMaterials() throws IOException {
        try (IResource resource = resource(".mtl");
                BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String material = "";
            for (String line; (line = reader.readLine()) != null;) {
                String[] fields = line.trim().split("\\s+");
                if (fields[0].equals("newmtl")) material = fields[1];
                if (fields[0].equals("map_Kd")) {
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
                String[] fields = line.trim().split("\\s+");
                switch (fields[0]) {
                    case "v":
                        positions.add(new double[] {number(fields[1]), number(fields[2]), number(fields[3])});
                        break;
                    case "vt":
                        uv.add(new double[] {number(fields[1]), 1 - number(fields[2])});
                        break;
                    case "usemtl":
                        if (!textures.containsKey(fields[1])) throw new IOException("Unknown OBJ material: " + fields[1]);
                        faces = groups.computeIfAbsent(fields[1], key -> new ArrayList<>());
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
                    default: break;
                }
            }
        }
        // Transparent material is drawn after the solid tower.
        List<Vertex[]> glass = groups.remove("glass");
        if (glass != null) groups.put("glass", glass);
    }

    private static double number(String value) { return Double.parseDouble(value); }

    private static final class Vertex {
        final double x, y, z, u, v;
        Vertex(double x, double y, double z, double u, double v) {
            this.x = x; this.y = y; this.z = z; this.u = u; this.v = v;
        }
    }
}
