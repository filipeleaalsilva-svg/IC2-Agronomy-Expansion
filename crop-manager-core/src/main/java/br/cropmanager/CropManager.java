package br.cropmanager;

import ic2.api.item.IC2Items;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;
import org.apache.logging.log4j.Logger;

@Mod(modid = CropManager.ID, name = "Crop Manager Core", version = CropManager.VERSION,
        acceptedMinecraftVersions = "[1.12.2]", acceptableRemoteVersions = "[" + CropManager.VERSION + "]",
        dependencies = "required-after:ic2@[2.8.222-ex112]")
@Mod.EventBusSubscriber(modid = CropManager.ID)
public final class CropManager {
    public static final String ID = "cropmanager";
    public static final String VERSION = "0.5.0-alpha9";
    @Mod.Instance(ID)
    public static CropManager instance;
    public static Logger log;

    public static final ManagerBlock ADVANCED = new ManagerBlock("advanced_crop_manager", 1);
    public static final ManagerBlock MONITOR = new ManagerBlock("agricultural_monitor", 3);
    public static final ManagerBlock COMPOSTER = new ManagerBlock("electric_composter", 4);
    public static final ManagerBlock WEEDER = new ManagerBlock("biogas_weeder", 8);
    public static final ManagerBlock WEED_EX_FABRICATOR = new ManagerBlock("weed_ex_fabricator", 9);
    public static final ManagerBlock[] BLOCKS = {ADVANCED, MONITOR, COMPOSTER, WEEDER, WEED_EX_FABRICATOR};

    public static final br.cropmanager.chest.block.BlockCropDatabaseChest DATABASE_CHEST = new br.cropmanager.chest.block.BlockCropDatabaseChest();
    public static final br.cropmanager.chest.item.ItemPortableCropDatabase PORTABLE_DATABASE = new br.cropmanager.chest.item.ItemPortableCropDatabase();
    public static final br.cropmanager.chest.item.ItemUpgradeLinker UPGRADE_LINKER = new br.cropmanager.chest.item.ItemUpgradeLinker();

    public static final Fluid GRIN_EXTRACT = WeedExFabricatorTile.SPIDER_EXTRACT;
    public static final Fluid LIQUID_REDSTONE = WeedExFabricatorTile.LIQUID_REDSTONE;

    public static final FarmTool PROBE = new FarmTool("agricultural_probe");
    public static final Item WEED_KILLER = infoItem("weed_killer_upgrade", 1, "cropmanager.upgrade_weed_killer");
    public static final Item WIRELESS_RANGE = infoItem("wireless_range_upgrade", 1, "cropmanager.wireless_upgrade_help");
    public static final Item HARVEST_TRANSFER = infoItem("harvest_transfer_upgrade", 1, "cropmanager.harvest_transfer_help");
    public static final Item WIRELESS_EU = new WirelessEuUpgrade();

    @SidedProxy(clientSide = "br.cropmanager.ClientProxy", serverSide = "br.cropmanager.CommonProxy")
    public static CommonProxy proxy;

    private static Item infoItem(String name, int maxStack, String tipKey) {
        return new Item() {
            @Override public void addInformation(ItemStack stack, net.minecraft.world.World world, java.util.List<String> tooltip, net.minecraft.client.util.ITooltipFlag flag) {
                tooltip.add(net.minecraft.client.resources.I18n.format(tipKey));
            }
        }.setRegistryName(ID, name).setTranslationKey("cropmanager." + name).setCreativeTab(net.minecraft.creativetab.CreativeTabs.REDSTONE).setMaxStackSize(maxStack);
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        log = event.getModLog();
        ManagerConfig.load(event.getSuggestedConfigurationFile());
        GameRegistry.registerTileEntity(AdvancedManagerTile.class, new ResourceLocation(ID, "advanced_crop_manager"));
        GameRegistry.registerTileEntity(MonitorTile.class, new ResourceLocation(ID, "agricultural_monitor"));
        GameRegistry.registerTileEntity(ComposterTile.class, new ResourceLocation(ID, "electric_composter"));
        GameRegistry.registerTileEntity(WeederTile.class, new ResourceLocation(ID, "biogas_weeder"));
        GameRegistry.registerTileEntity(WeedExFabricatorTile.class, new ResourceLocation(ID, "weed_ex_fabricator"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest.class, new ResourceLocation(ID, "chest"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest250.class, new ResourceLocation(ID, "chest_250"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.TileCropDatabaseChest125.class, new ResourceLocation(ID, "chest_125"));
        GameRegistry.registerTileEntity(br.cropmanager.chest.inventory.LegacyModularChestTile.class, new ResourceLocation(ID, "modular_chest"));
        FluidRegistry.registerFluid(GRIN_EXTRACT);
        FluidRegistry.registerFluid(LIQUID_REDSTONE);
        ManagerNetwork.init();
        proxy.registerClientResources();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, proxy);
    }

    @SubscribeEvent public static void blocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS);
        event.getRegistry().register(DATABASE_CHEST);
    }

    @SubscribeEvent public static void items(RegistryEvent.Register<Item> event) {
        for (ManagerBlock block : BLOCKS) event.getRegistry().register(new ItemBlock(block).setRegistryName(block.getRegistryName()));
        event.getRegistry().register(new ItemBlock(DATABASE_CHEST).setRegistryName(DATABASE_CHEST.getRegistryName()));
        event.getRegistry().registerAll(PROBE, WEED_KILLER, WIRELESS_RANGE, HARVEST_TRANSFER, WIRELESS_EU, PORTABLE_DATABASE, UPGRADE_LINKER);
    }

    @SubscribeEvent public static void recipes(RegistryEvent.Register<IRecipe> event) {
        ItemStack harvester = required("te", "crop_harvester");
        ItemStack matron = required("te", "cropmatron");
        ItemStack circuit = required("crafting", "advanced_circuit");
        ItemStack alloy = required("crafting", "alloy");
        ItemStack basicCircuit = required("crafting", "circuit");
        recipe(event, "advanced_crop_manager", new ItemStack(ADVANCED), "HIC", "CMC", "III", 'H', harvester, 'I', alloy, 'C', circuit, 'M', matron);
        recipe(event, "agricultural_monitor", new ItemStack(MONITOR), "IGI", "CIC", "IRI", 'I', alloy, 'G', net.minecraft.init.Blocks.GLASS, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "electric_composter", new ItemStack(COMPOSTER), "III", "FCF", "III", 'I', net.minecraft.init.Items.IRON_INGOT, 'F', net.minecraft.init.Blocks.FURNACE, 'C', basicCircuit);
        recipe(event, "biogas_weeder", new ItemStack(WEEDER), "ISI", "FCF", "III", 'I', net.minecraft.init.Items.IRON_INGOT, 'S', net.minecraft.init.Items.SHEARS, 'F', net.minecraft.init.Blocks.FURNACE, 'C', basicCircuit);
        recipe(event, "weed_ex_fabricator", new ItemStack(WEED_EX_FABRICATOR), "IGI", "GCG", "IRI", 'I', net.minecraft.init.Items.IRON_INGOT, 'G', net.minecraft.init.Blocks.GLASS, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "agricultural_probe", new ItemStack(PROBE), " G ", "ICI", " R ", 'G', net.minecraft.init.Blocks.GLASS, 'I', net.minecraft.init.Items.IRON_INGOT, 'C', basicCircuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "weed_killer_upgrade", new ItemStack(WEED_KILLER), "RGR", "CEC", "RGR", 'R', net.minecraft.init.Items.REDSTONE, 'G', net.minecraft.init.Items.GLOWSTONE_DUST, 'C', circuit, 'E', IC2Items.getItem("upgrade", "overclocker"));
        recipe(event, "wireless_range_upgrade", new ItemStack(WIRELESS_RANGE), " E ", "ECE", " R ", 'E', net.minecraft.init.Items.ENDER_PEARL, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        recipe(event, "harvest_transfer_upgrade", new ItemStack(HARVEST_TRANSFER), "IDI", "CEC", "IDI", 'I', alloy, 'D', net.minecraft.init.Items.DIAMOND, 'C', circuit, 'E', net.minecraft.init.Items.ENDER_PEARL);
        recipe(event, "wireless_eu_upgrade", new ItemStack(WIRELESS_EU), "ERE", "ASA", "ETE", 'E', net.minecraft.init.Items.ENDER_PEARL, 'R', net.minecraft.init.Items.REDSTONE, 'A', circuit, 'S', required("upgrade", "energy_storage"), 'T', required("upgrade", "transformer"));
        recipe(event, "upgrade_linker", new ItemStack(UPGRADE_LINKER), " E ", "ICI", " R ", 'E', net.minecraft.init.Items.ENDER_PEARL, 'I', net.minecraft.init.Items.IRON_INGOT, 'C', circuit, 'R', net.minecraft.init.Items.REDSTONE);
        event.getRegistry().register(br.cropmanager.chest.crafting.CropChestRecipe.create().setRegistryName(ID, "crop_database_chest"));
        event.getRegistry().register(br.cropmanager.chest.crafting.PortableCropRecipe.create().setRegistryName(ID, "portable_crop_database"));
        event.getRegistry().register(new br.cropmanager.chest.crafting.PortableLinkRecipe().setRegistryName(ID, "portable_database_link"));
    }

    private static void recipe(RegistryEvent.Register<IRecipe> event, String name, ItemStack output, Object... shape) {
        event.getRegistry().register(new ShapedOreRecipe(new ResourceLocation(ID, "agriculture"), output, shape).setRegistryName(ID, name));
    }

    private static ItemStack required(String name, String variant) {
        ItemStack stack = IC2Items.getItem(name, variant);
        if (stack == null || stack.isEmpty()) throw new IllegalStateException("Missing IC2 item: " + name + ":" + variant);
        return stack;
    }
}
