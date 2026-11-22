package br.cropmanager.chest.integration;

import ic2.api.crops.CropCard;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/** Declared alternatives from E2E-E 1.87.3, not a simulated harvest. */
public final class PackCropProducts {
    private static final Map<String, String[]> PRODUCTS = load();
    private PackCropProducts() { }

    private static Map<String, String[]> load() {
        Map<String, String[]> products = new HashMap<>();
        InputStream stream = PackCropProducts.class.getResourceAsStream("/assets/cropmanager/crop_products_e2ee_1_87_3.tsv");
        if (stream == null) throw new IllegalStateException("Missing crop catalog");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("#") || line.isEmpty()) continue;
                String[] entry = line.split("\t");
                products.put(entry[0], entry[1].split(","));
            }
        } catch (IOException error) { throw new IllegalStateException("Cannot read crop catalog", error); }
        return products;
    }

    public static List<ItemStack> query(CropCard crop) {
        String[] definitions = PRODUCTS.get(SeedSearch.identity(crop));
        if (definitions == null) return null;
        List<ItemStack> result = new ArrayList<>();
        for (String definition : definitions) {
            int colon = definition.lastIndexOf(':');
            Item item = Item.getByNameOrId(definition.substring(0, colon));
            // Never present a partial list as if it covered all declared alternatives.
            if (item == null) return null;
            result.add(new ItemStack(item, 1, Integer.parseInt(definition.substring(colon + 1))));
        }
        return result;
    }
}
