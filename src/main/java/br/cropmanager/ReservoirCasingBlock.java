package br.cropmanager;

import javax.annotation.Nullable;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Structural shell block; part appearance is derived from its cached controller offset. */
public final class ReservoirCasingBlock extends Block {
    public enum Part implements IStringSerializable {
        CASING, GLASS, LADDER, RAILING, TOWER;
        @Override public String getName() { return name().toLowerCase(java.util.Locale.ROOT); }
    }
    public static final PropertyEnum<Part> PART = PropertyEnum.create("part", Part.class);

    public ReservoirCasingBlock() {
        super(Material.IRON);
        setRegistryName(CropManager.ID, "reservoir_casing");
        setTranslationKey("cropmanager.reservoir_casing");
        setCreativeTab(CropManager.TAB);
        setHardness(3.0F);
        setResistance(10.0F);
        setHarvestLevel("pickaxe", 1);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(PART, Part.CASING));
    }

    @Override protected BlockStateContainer createBlockState() { return new BlockStateContainer(this, PART); }
    @Override public IBlockState getStateFromMeta(int meta) { return getDefaultState(); }
    @Override public int getMetaFromState(IBlockState state) { return 0; }
    @Override public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        if (tile instanceof ReservoirCasingTile) return state.withProperty(PART, ((ReservoirCasingTile) tile).renderPart());
        return state.withProperty(PART, Part.CASING);
    }
    @Override public boolean hasTileEntity(IBlockState state) { return true; }
    @Override public TileEntity createTileEntity(World world, IBlockState state) { return new ReservoirCasingTile(); }
    @Override public boolean isOpaqueCube(IBlockState state) { return false; }
    @Override public boolean isFullCube(IBlockState state) { return true; }
    @SideOnly(Side.CLIENT) @Override public BlockRenderLayer getRenderLayer() { return BlockRenderLayer.CUTOUT_MIPPED; }

    @Override public boolean hasComparatorInputOverride(IBlockState state) { return true; }

    @Override public int getComparatorInputOverride(IBlockState state, World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof ReservoirCasingTile ? ((ReservoirCasingTile) tile).comparatorSignal() : 0;
    }

    @Override public boolean onBlockActivated(World world, BlockPos pos, IBlockState state,
            EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof ReservoirCasingTile)) return false;
        AgriculturalReservoirTile controller = ((ReservoirCasingTile) tile).controller();
        if (controller == null || !controller.formed) return false;
        if (!world.isRemote) {
            controller.tryForm();
            if (!controller.formed) return false;
            BlockPos master = controller.getPos();
            player.openGui(CropManager.instance, 15, world, master.getX(), master.getY(), master.getZ());
        }
        return true;
    }

    @Override public void breakBlock(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof ReservoirCasingTile) ((ReservoirCasingTile) tile).notifyControllerBroken();
        }
        super.breakBlock(world, pos, state);
    }
}
