package br.cropmanager.chest.crafting;

import br.cropmanager.CropManager;
import ic2.api.item.IC2Items;
import ic2.api.item.ElectricItem;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/** Fixed layout: each universal cell supplies one bucket and is returned. */
public final class CropChestRecipe extends ShapedRecipes {
    public CropChestRecipe(ItemStack output, NonNullList<Ingredient> ingredients,
                           ItemStack universalCan, Fluid water, Fluid weedEx) {
        super("", 3, 3, ingredients, output);
        ingredients.set(6, new FluidCanIngredient(universalCan, water));
        ingredients.set(8, new FluidCanIngredient(universalCan, weedEx));
    }

    public static CropChestRecipe create() {
        NonNullList<Ingredient> ingredients = NonNullList.withSize(9, Ingredient.EMPTY);
        ingredients.set(0, Ingredient.fromStacks(IC2Items.getItem("plate", "steel")));
        ingredients.set(1, Ingredient.fromStacks(IC2Items.getItem("te", "mfsu")));
        ingredients.set(2, Ingredient.fromStacks(IC2Items.getItem("plate", "steel")));
        ingredients.set(3, Ingredient.fromStacks(IC2Items.getItem("crafting", "advanced_circuit")));
        ingredients.set(4, Ingredient.fromStacks(IC2Items.getItem("resource", "advanced_machine")));
        ingredients.set(5, Ingredient.fromStacks(IC2Items.getItem("crafting", "advanced_circuit")));
        ingredients.set(7, new ChargedCrystalIngredient(IC2Items.getItem("lapotron_crystal")));
        return new CropChestRecipe(new ItemStack(CropManager.DATABASE_CHEST), ingredients,
                IC2Items.getItem("fluid_cell"), FluidRegistry.WATER, FluidRegistry.getFluid("ic2weed_ex"));
    }

    @Override public boolean matches(InventoryCrafting inventory, World world) {
        if (inventory.getWidth() != 3 || inventory.getHeight() != 3) return false;
        for (int slot = 0; slot < 9; slot++) {
            if (!recipeItems.get(slot).apply(inventory.getStackInSlot(slot))) return false;
        }
        return true;
    }

    @Override public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inventory) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inventory.getSizeInventory(), ItemStack.EMPTY);
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack input = inventory.getStackInSlot(slot);
            if ((slot == 6 || slot == 8) && recipeItems.get(slot).apply(input)) {
                ItemStack copy = input.copy();
                copy.setCount(1);
                IFluidHandlerItem handler = FluidUtil.getFluidHandler(copy);
                handler.drain(1000, true);
                remaining.set(slot, handler.getContainer());
            } else {
                remaining.set(slot, ForgeHooks.getContainerItem(input));
            }
        }
        return remaining;
    }

    static final class ChargedCrystalIngredient extends Ingredient {
        private final ItemStack crystal;

        ChargedCrystalIngredient(ItemStack crystal) {
            super(fullyCharged(crystal));
            this.crystal = crystal.copy();
        }
        private static ItemStack fullyCharged(ItemStack crystal) {
            ItemStack copy = crystal.copy();
            ElectricItem.manager.charge(copy, Double.POSITIVE_INFINITY, Integer.MAX_VALUE, true, false);
            return copy;
        }
        @Override public boolean apply(ItemStack input) {
            if (input == null || input.isEmpty() || input.getItem() != crystal.getItem()) return false;
            ItemStack copy = input.copy();
            double maximum = ElectricItem.manager.getMaxCharge(copy);
            return maximum > 0 && ElectricItem.manager.getCharge(copy) >= maximum;
        }
        @Override public boolean isSimple() { return false; }
    }

    private static final class FluidCanIngredient extends Ingredient {
        private final ItemStack can;
        private final Fluid fluid;

        FluidCanIngredient(ItemStack can, Fluid fluid) {
            super(filledCan(can, fluid));
            this.can = can.copy();
            this.fluid = fluid;
        }

        private static ItemStack filledCan(ItemStack can, Fluid fluid) {
            if (can.isEmpty() || fluid == null) throw new IllegalStateException("Missing IC2 recipe item or fluid");
            ItemStack filled = can.copy();
            filled.setCount(1);
            IFluidHandlerItem handler = FluidUtil.getFluidHandler(filled);
            if (handler == null || handler.fill(new FluidStack(fluid, 1000), true) != 1000) {
                throw new IllegalStateException("IC2 universal cell cannot hold recipe fluid");
            }
            return handler.getContainer();
        }

        @Override public boolean apply(ItemStack input) {
            if (input == null || input.isEmpty() || !ItemStack.areItemsEqual(can, input)) return false;
            ItemStack copy = input.copy();
            copy.setCount(1);
            IFluidHandlerItem handler = FluidUtil.getFluidHandler(copy);
            if (handler == null) return false;
            FluidStack drained = handler.drain(1000, false);
            return drained != null && drained.amount == 1000 && drained.getFluid() == fluid;
        }

        @Override public boolean isSimple() { return false; }
    }
}
