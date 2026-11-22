package br.cropmanager.chest.crafting;

import br.cropmanager.CropManager;
import br.cropmanager.chest.item.ItemUpgradeLinker;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.NonNullList;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Shapeless recipe that applies a bound Upgrade Linker onto a Portable Crop Database,
 * preserving all stored seeds and settings inside the portable item.
 */
public final class PortableLinkRecipe extends IForgeRegistryEntry.Impl<IRecipe> implements IRecipe {
    @Override
    public boolean matches(InventoryCrafting inv, World world) {
        return !findResult(inv).isEmpty();
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting inv) {
        return findResult(inv);
    }

    @Override
    public boolean canFit(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public ItemStack getRecipeOutput() {
        return new ItemStack(CropManager.PORTABLE_DATABASE);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(InventoryCrafting inv) {
        return NonNullList.withSize(inv.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    private static ItemStack findResult(InventoryCrafting inv) {
        ItemStack portable = ItemStack.EMPTY;
        ItemStack linker = ItemStack.EMPTY;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.isEmpty()) continue;
            if (stack.getItem() == CropManager.PORTABLE_DATABASE && portable.isEmpty()) {
                portable = stack;
            } else if (stack.getItem() == CropManager.UPGRADE_LINKER && linker.isEmpty() && ItemUpgradeLinker.hasLink(stack)) {
                linker = stack;
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (portable.isEmpty() || linker.isEmpty()) return ItemStack.EMPTY;
        ItemStack result = portable.copy();
        result.setCount(1);
        ItemUpgradeLinker.copyLink(linker, result);
        return result;
    }
}
