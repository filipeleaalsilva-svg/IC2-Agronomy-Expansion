package br.cropmanager;

import ic2.api.crops.CropCard;
import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Cutout greenhouse pane that gives one directly adjacent IC2 crop a small growth boost. */
public final class GreenhouseGlassBlock extends Block {
    public GreenhouseGlassBlock() {
        super(Material.GLASS);
        setRegistryName(CropManager.ID, "greenhouse_glass");
        setTranslationKey("cropmanager.greenhouse_glass");
        setCreativeTab(CropManager.TAB);
        setHardness(0.5F);
        setResistance(2.0F);
        setSoundType(SoundType.GLASS);
        setLightOpacity(0);
        setTickRandomly(true);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean shouldSideBeRendered(IBlockState state, IBlockAccess access, BlockPos pos, EnumFacing side) {
        if (access.getBlockState(pos.offset(side)).getBlock() == this) return false;
        return super.shouldSideBeRendered(state, access, pos, side);
    }

    @Override
    public int quantityDropped(Random random) {
        return 1;
    }

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (world.isRemote || pos.getY() + 1 >= world.getHeight()) return;
        BlockPos cropPos = pos.down();
        if (cropPos.getY() < 0 || !world.isBlockLoaded(cropPos, false)
                || world.getLightFromNeighbors(pos.up()) < 9) return;

        TileEntity tile = world.getTileEntity(cropPos);
        if (!(tile instanceof TileEntityCrop)) return;
        TileEntityCrop crop = (TileEntityCrop) tile;
        CropCard card = crop.getCrop();
        if (card == null || card == Crops.weed || crop.getCurrentSize() >= card.getMaxSize()) return;

        int bonus = Math.max(0, ManagerConfig.greenhouseGrowthBonus);
        if (bonus == 0) return;
        long next = (long) Math.max(0, crop.getGrowthPoints()) + bonus;
        crop.setGrowthPoints((int) Math.min(Integer.MAX_VALUE, next));
        crop.dirty = true;
        world.markChunkDirty(cropPos, crop);
    }
}
