package br.cropmanager;

import java.util.Random;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Passive IC2-style hive which accelerates one nearby double-crop crossing site per pulse. */
public final class PollinatorHiveBlock extends Block {
    public static final PropertyBool CHARGED = PropertyBool.create("charged");

    public PollinatorHiveBlock() {
        super(Material.WOOD);
        setRegistryName(CropManager.ID, "pollinator_hive");
        setTranslationKey("cropmanager.pollinator_hive");
        setCreativeTab(CropManager.TAB);
        setHardness(2.0F);
        setResistance(5.0F);
        setSoundType(SoundType.WOOD);
        setDefaultState(blockState.getBaseState().withProperty(CHARGED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, CHARGED);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(CHARGED, (meta & 1) != 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(CHARGED) ? 1 : 0;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new PollinatorHiveTile();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
            EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty() || held.getItem() != CropManager.MUTAGENIC_POWDER) return false;
        if (!player.canPlayerEdit(pos, side, held)) return true;
        if (world.isRemote) return true;

        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof PollinatorHiveTile)) return true;
        if (((PollinatorHiveTile) tile).addPowderCharge()) {
            if (!player.capabilities.isCreativeMode) held.shrink(1);
            world.playSound(null, pos, SoundEvents.BLOCK_WOOD_PLACE, SoundCategory.BLOCKS, 0.6F, 1.4F);
        }
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random random) {
        if (!state.getValue(CHARGED)) return;
        for (int i = 0; i < 3; i++) {
            double x = pos.getX() + 0.2 + random.nextDouble() * 0.6;
            double y = pos.getY() + 0.55 + random.nextDouble() * 0.35;
            double z = pos.getZ() + 0.2 + random.nextDouble() * 0.6;
            world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY, x, y, z, 0.0, 0.01, 0.0);
        }
    }
}
