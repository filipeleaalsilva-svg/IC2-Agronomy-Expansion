package br.cropmanager;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Typed item for Crop Manager / Agricultural Monitor upgrades with localized tooltip. */
public final class MonitorUpgradeItem extends Item {
    public enum Role {
        WEED_KILLER("weed_killer_upgrade", "cropmanager.upgrade_weed_killer"),
        WIRELESS_RANGE("wireless_range_upgrade", "cropmanager.wireless_upgrade_help"),
        HARVEST_TRANSFER("harvest_transfer_upgrade", "cropmanager.harvest_transfer_help");

        final String registryName;
        final String tipKey;

        Role(String registryName, String tipKey) {
            this.registryName = registryName;
            this.tipKey = tipKey;
        }
    }

    private final Role role;

    public MonitorUpgradeItem(Role role) {
        this.role = role;
        setRegistryName(CropManager.ID, role.registryName);
        setTranslationKey("cropmanager." + role.registryName);
        setCreativeTab(CropManager.TAB);
        setMaxStackSize(1);
    }

    public Role getRole() {
        return role;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format(role.tipKey));
    }
}
