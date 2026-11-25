package br.cropmanager;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Decorative agricultural and greenhouse building blocks. */
public final class DecorativeBlock extends Block {
    public enum Kind {
        GREENHOUSE_FRAME("greenhouse_frame", Material.IRON, SoundType.METAL, 2.0F, 0.0F, false),
        GARDEN_TRELLIS("garden_trellis", Material.WOOD, SoundType.WOOD, 0.6F, 0.0F, true);

        final String registryName;
        final Material material;
        final SoundType sound;
        final float hardness;
        final float lightLevel;
        final boolean cutout;

        Kind(String registryName, Material material, SoundType sound, float hardness, float lightLevel, boolean cutout) {
            this.registryName = registryName;
            this.material = material;
            this.sound = sound;
            this.hardness = hardness;
            this.lightLevel = lightLevel;
            this.cutout = cutout;
        }
    }

    private final Kind kind;

    public DecorativeBlock(Kind kind) {
        super(kind.material);
        this.kind = kind;
        setRegistryName(CropManager.ID, kind.registryName);
        setTranslationKey("cropmanager." + kind.registryName);
        setCreativeTab(CropManager.TAB);
        setHardness(kind.hardness);
        setResistance(5.0F);
        setSoundType(kind.sound);
        if (kind.lightLevel > 0.0F) setLightLevel(kind.lightLevel);
        this.fullBlock = !kind.cutout;
        this.lightOpacity = kind.cutout ? 0 : 255;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return kind == null || !kind.cutout;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return kind == null || !kind.cutout;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return kind.cutout ? BlockRenderLayer.CUTOUT : BlockRenderLayer.SOLID;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(I18n.format("cropmanager." + kind.registryName + "_help"));
    }
}
