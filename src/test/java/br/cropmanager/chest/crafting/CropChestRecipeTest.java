package br.cropmanager.chest.crafting;

import br.cropmanager.chest.SeedFixtures;
import java.lang.reflect.Field;
import java.util.Map;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CropChestRecipeTest {
    private static Item can, otherCan;
    private static Fluid weedEx;
    private CropChestRecipe recipe;
    private InventoryCrafting grid;

    @SuppressWarnings("unchecked")
    @BeforeClass public static void bootstrap() throws ReflectiveOperationException {
        SeedFixtures.bootstrap();
        br.cropmanager.TestCapabilityHelper.ensureFluidCapabilities();
        can = new TestCan().setRegistryName("cropdatabasechest_test", "fluid_can");
        otherCan = new TestCan().setRegistryName("cropdatabasechest_test", "other_can");
        ForgeRegistries.ITEMS.register(can);
        ForgeRegistries.ITEMS.register(otherCan);
        weedEx = new Fluid("cropdatabasechest_test_weed_ex", new ResourceLocation("test:still"), new ResourceLocation("test:flow"));
        FluidRegistry.registerFluid(weedEx);
    }

    @Before public void setup() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        Item[] items = {Items.STICK, Items.IRON_INGOT, Items.STICK,
                Items.REDSTONE, Items.DIAMOND, Items.REDSTONE,
                can, Items.COMPASS, can};
        for (int i = 0; i < items.length; i++) ingredients.set(i, Ingredient.fromItem(items[i]));
        recipe = new CropChestRecipe(new ItemStack(Items.CHEST_MINECART), ingredients,
                new ItemStack(can), FluidRegistry.WATER, weedEx);
        grid = new InventoryCrafting(mock(Container.class), 3, 3);
        for (int i = 0; i < items.length; i++) grid.setInventorySlotContents(i, new ItemStack(items[i]));
        grid.setInventorySlotContents(6, filled(can, FluidRegistry.WATER, 1000));
        grid.setInventorySlotContents(8, filled(can, weedEx, 1000));
    }

    @Test public void exactBucketsCraftAndReturnBothEmptyCans() {
        assertTrue(recipe.matches(grid, null));
        assertEquals(Items.CHEST_MINECART, recipe.getCraftingResult(grid).getItem());
        NonNullList<ItemStack> remainder = recipe.getRemainingItems(grid);
        for (int i = 0; i < 9; i++) {
            if (i == 6 || i == 8) {
                assertEquals(can, remainder.get(i).getItem());
                assertEquals(1, remainder.get(i).getCount());
                assertNull(FluidUtil.getFluidContained(remainder.get(i)));
            } else assertTrue(remainder.get(i).isEmpty());
        }
    }

    @Test public void matchingAndRemaindersDoNotMutateInputsOrLoseExtraFluid() {
        grid.setInventorySlotContents(6, filled(can, FluidRegistry.WATER, 3500));
        grid.setInventorySlotContents(8, filled(can, weedEx, 2000));
        NBTTagCompound beforeWater = grid.getStackInSlot(6).serializeNBT();
        NBTTagCompound beforeWeed = grid.getStackInSlot(8).serializeNBT();
        assertTrue(recipe.matches(grid, null));
        NonNullList<ItemStack> remainder = recipe.getRemainingItems(grid);
        assertEquals(2500, FluidUtil.getFluidContained(remainder.get(6)).amount);
        assertEquals(1000, FluidUtil.getFluidContained(remainder.get(8)).amount);
        assertEquals(FluidRegistry.WATER, FluidUtil.getFluidContained(remainder.get(6)).getFluid());
        assertEquals(weedEx, FluidUtil.getFluidContained(remainder.get(8)).getFluid());
        assertEquals(beforeWater, grid.getStackInSlot(6).serializeNBT());
        assertEquals(beforeWeed, grid.getStackInSlot(8).serializeNBT());
    }

    @Test public void rejectsEmptyOrInsufficientWater() {
        grid.setInventorySlotContents(6, new ItemStack(can));
        assertFalse(recipe.matches(grid, null));
        grid.setInventorySlotContents(6, filled(can, FluidRegistry.WATER, 999));
        assertFalse(recipe.matches(grid, null));
    }

    @Test public void rejectsEmptyOrInsufficientWeedEx() {
        grid.setInventorySlotContents(8, new ItemStack(can));
        assertFalse(recipe.matches(grid, null));
        grid.setInventorySlotContents(8, filled(can, weedEx, 999));
        assertFalse(recipe.matches(grid, null));
    }

    @Test public void rejectsWrongFluidsAndSwappedCans() {
        grid.setInventorySlotContents(6, filled(can, FluidRegistry.LAVA, 1000));
        assertFalse(recipe.matches(grid, null));
        grid.setInventorySlotContents(6, filled(can, weedEx, 1000));
        grid.setInventorySlotContents(8, filled(can, FluidRegistry.WATER, 1000));
        assertFalse(recipe.matches(grid, null));
    }

    @Test public void rejectsOtherFluidContainerAndWrongGridPositions() {
        grid.setInventorySlotContents(6, filled(otherCan, FluidRegistry.WATER, 1000));
        assertFalse(recipe.matches(grid, null));
        grid.setInventorySlotContents(6, filled(can, FluidRegistry.WATER, 1000));
        ItemStack center = grid.getStackInSlot(4);
        grid.setInventorySlotContents(4, grid.getStackInSlot(7));
        grid.setInventorySlotContents(7, center);
        assertFalse(recipe.matches(grid, null));
    }

    @Test public void cannotCraftInPlayerTwoByTwoGrid() {
        assertFalse(recipe.canFit(2, 2));
        assertFalse(recipe.matches(new InventoryCrafting(mock(Container.class), 2, 2), null));
    }

    @Test public void crystalIngredientRequiresFullChargeAndDoesNotChangeInput() {
        ic2.api.item.IElectricItemManager previous = ic2.api.item.ElectricItem.manager;
        ic2.api.item.IElectricItemManager manager = mock(ic2.api.item.IElectricItemManager.class);
        try {
            ic2.api.item.ElectricItem.manager = manager;
            when(manager.getMaxCharge(any(ItemStack.class))).thenReturn(100.0);
            when(manager.getCharge(any(ItemStack.class))).thenAnswer(call -> {
                ItemStack stack = call.getArgument(0);
                return stack.hasTagCompound() ? stack.getTagCompound().getDouble("charge") : 0.0;
            });
            when(manager.charge(any(ItemStack.class), anyDouble(), anyInt(), anyBoolean(), anyBoolean())).thenAnswer(call -> {
                ItemStack stack = call.getArgument(0);
                NBTTagCompound tag = new NBTTagCompound();
                tag.setDouble("charge", 100);
                stack.setTagCompound(tag);
                return 100.0;
            });
            Ingredient crystal = new CropChestRecipe.ChargedCrystalIngredient(new ItemStack(Items.DIAMOND));
            assertFalse(crystal.isSimple());
            assertEquals(100, crystal.getMatchingStacks()[0].getTagCompound().getDouble("charge"), 0);
            ItemStack input = new ItemStack(Items.DIAMOND);
            assertFalse(crystal.apply(input));
            for (double charge : new double[] {0, 50, 99.999, 100}) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setDouble("charge", charge);
                input.setTagCompound(tag);
                NBTTagCompound before = input.serializeNBT();
                assertEquals(charge == 100, crystal.apply(input));
                assertEquals(before, input.serializeNBT());
            }
            assertFalse(crystal.apply(new ItemStack(Items.EMERALD)));
        } finally { ic2.api.item.ElectricItem.manager = previous; }
    }

    private static ItemStack filled(Item item, Fluid fluid, int amount) {
        ItemStack stack = new ItemStack(item);
        assertEquals(amount, FluidUtil.getFluidHandler(stack).fill(new FluidStack(fluid, amount), true));
        return stack;
    }

    private static final class TestCan extends Item {
        TestCan() { setMaxStackSize(1); }
        @Override public ICapabilityProvider initCapabilities(ItemStack stack, NBTTagCompound nbt) {
            return new FluidHandlerItemStack(stack, 10000);
        }
    }
}
