package br.cropmanager.crop;

import br.cropmanager.CropManager;
import br.cropmanager.ManagerConfig;
import ic2.api.crops.CropCard;
import ic2.api.crops.CropProperties;
import ic2.api.crops.Crops;
import ic2.api.item.IC2Items;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

/** Registers Crop Manager's custom IC2 crops (Agricultural Support + Catalyzed species). */
public final class CropRegistry {
    public static final BaseCropCard GRINBLOOM = new BaseCropCard(
            "grinbloom",
            new CropProperties(4, 2, 0, 3, 1, 2),
            new String[] {"Toxic", "Green", "Weed"},
            () -> safeIc2("crop_res", "grin_powder", new ItemStack(Items.SPIDER_EYE)),
            false
    );

    public static final BaseCropCard BIOREED = new BaseCropCard(
            "bioreed",
            new CropProperties(3, 1, 2, 0, 1, 1),
            new String[] {"Reed", "Green", "Leaves"},
            () -> new ItemStack(CropManager.BIO_FIBER, 2),
            false
    );

    public static final BaseCropCard NITROBULB = new BaseCropCard(
            "nitrobulb",
            new CropProperties(4, 3, 2, 0, 1, 0),
            new String[] {"Root", "Food", "Nutrient"},
            () -> new ItemStack(CropManager.NITRO_ESSENCE, 1),
            false
    );

    public static final BaseCropCard LAZULI_BLOOM = new BaseCropCard(
            "lazuli_bloom",
            new CropProperties(5, 3, 0, 2, 3, 0),
            new String[] {"Blue", "Crystal", "Catalyzed"},
            () -> safeIc2("dust", "small_lapis", new ItemStack(Items.DYE, 1, 4)),
            true
    );

    public static final BaseCropCard IRID_MOSS = new BaseCropCard(
            "irid_moss",
            new CropProperties(8, 3, 0, 3, 2, 0),
            new String[] {"Metal", "Rare", "Catalyzed"},
            () -> safeIc2("misc_resource", "iridium_shard", new ItemStack(Items.DIAMOND)),
            true
    );

    public static final RadbloomCropCard RADBLOOM = new RadbloomCropCard(
            () -> safeIc2("nuclear", "small_uranium_238", new ItemStack(Items.SLIME_BALL))
    );

    public static final BaseCropCard GLOWBLOOM = new BaseCropCard(
            "glowbloom",
            new CropProperties(4, 2, 0, 1, 4, 0),
            new String[] {"Light", "Yellow", "Glowing"},
            () -> new ItemStack(Items.GLOWSTONE_DUST, 1),
            false,
            12
    );

    public static final BaseCropCard QUARTZREED = new BaseCropCard(
            "quartzreed",
            new CropProperties(5, 2, 0, 2, 2, 0),
            new String[] {"Crystal", "White", "Catalyzed"},
            () -> new ItemStack(Items.QUARTZ, 1),
            true
    );

    public static final BaseCropCard LITHIVINE = new BaseCropCard(
            "lithivine",
            new CropProperties(6, 3, 0, 2, 2, 0),
            new String[] {"Metal", "Lithium", "Catalyzed"},
            () -> safeIc2("dust", "small_lithium", new ItemStack(Items.REDSTONE)),
            true
    );

    public static final BaseCropCard SULFURBUD = new BaseCropCard(
            "sulfurbud",
            new CropProperties(4, 3, 0, 2, 2, 1),
            new String[] {"Sulfur", "Yellow", "Chemical"},
            () -> safeIc2("dust", "small_sulfur", new ItemStack(Items.GUNPOWDER)),
            false
    );

    public static final List<BaseCropCard> ALL = Collections.unmodifiableList(
            Arrays.asList(GRINBLOOM, BIOREED, NITROBULB, LAZULI_BLOOM, IRID_MOSS, RADBLOOM,
                    GLOWBLOOM, QUARTZREED, LITHIVINE, SULFURBUD)
    );

    private CropRegistry() {}

    public static void registerAll() {
        if (!ManagerConfig.enableCustomCrops || Crops.instance == null) return;
        for (CropCard card : ALL) {
            Crops.instance.registerCrop(card);
        }
        // Allow planting Bioreed directly from Bio-Fiber as an entry point into the custom crop line.
        Crops.instance.registerBaseSeed(new ItemStack(CropManager.BIO_FIBER), BIOREED, 1, 1, 1, 1);
    }

    private static ItemStack safeIc2(String name, String variant, ItemStack fallback) {
        ItemStack stack = IC2Items.getItem(name, variant);
        return stack == null || stack.isEmpty() ? fallback : stack;
    }
}
