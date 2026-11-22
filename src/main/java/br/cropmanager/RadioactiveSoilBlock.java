package br.cropmanager;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Required subsoil block beneath farmland to cultivate radioactive IC2 crops (Radbloom). */
public final class RadioactiveSoilBlock extends Block {
    public RadioactiveSoilBlock() {
        super(Material.GROUND);
        setRegistryName(CropManager.ID, "radioactive_soil");
        setTranslationKey("cropmanager.radioactive_soil");
        setCreativeTab(CreativeTabs.BUILDING_BLOCKS);
        setHardness(0.8F);
        setResistance(4.0F);
        setSoundType(SoundType.GROUND);
        setHarvestLevel("shovel", 0);
        setLightLevel(0.45F);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager.radioactive_soil_help"));
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        if (rand.nextInt(4) == 0) {
            double x = pos.getX() + 0.15 + rand.nextDouble() * 0.7;
            double y = pos.getY() + 1.05;
            double z = pos.getZ() + 0.15 + rand.nextDouble() * 0.7;
            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 0.0, 0.015, 0.0);
        }
    }
}
