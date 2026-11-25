package br.cropmanager;

import ic2.core.ref.FluidName;
import java.lang.reflect.Field;
import net.minecraft.init.Bootstrap;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BiogasHeatBurnerTest {
    @Test public void pipeSimulationAndDeliveryShareABudgetAndPersistUnusedHeat() throws Exception {
        Bootstrap.register();
        if (TileEntity.getKey(BiogasHeatBurnerTile.class) == null) {
            TileEntity.register("cropmanager:biogas_heat_burner", BiogasHeatBurnerTile.class);
        }
        Fluid biogas = new Fluid("cropmanager_test_burner_gas",
                new ResourceLocation("minecraft:blocks/water_still"), new ResourceLocation("minecraft:blocks/water_flow"));
        FluidRegistry.registerFluid(biogas);
        Field instance = FluidName.class.getDeclaredField("instance");
        instance.setAccessible(true);
        Object old = instance.get(FluidName.biogas);
        instance.set(FluidName.biogas, biogas);
        try {
            World world = mock(World.class);
            BlockPos burnerPos = new BlockPos(0, 64, 0);
            when(world.getBlockState(burnerPos)).thenReturn(CropManager.BIOGAS_HEAT_BURNER.getDefaultState());
            when(world.isBlockLoaded(burnerPos, false)).thenReturn(true);
            BiogasHeatBurnerTile burner = new BiogasHeatBurnerTile();
            burner.setWorld(world);
            burner.setPos(burnerPos);
            when(world.getTileEntity(burnerPos)).thenReturn(burner);
            assertEquals(0, burner.gas.fill(new FluidStack(FluidRegistry.WATER, 2), true));
            assertEquals(2, burner.gas.fill(new FluidStack(biogas, 2), true));
            assertNull(burner.gas.drain(1, true));
            assertEquals(0, burner.drawHeat(EnumFacing.SOUTH, 32, false));

            ThermalPipeTile pipe = new ThermalPipeTile();
            pipe.setWorld(world);
            pipe.setPos(burnerPos.north());
            assertEquals(15, pipe.drawHeat(EnumFacing.NORTH, 15, true));
            assertEquals(2, burner.gas.getFluidAmount());
            assertEquals(15, pipe.requestHeat(EnumFacing.NORTH, 15));
            assertEquals(1, burner.gas.getFluidAmount());
            assertEquals(17, pipe.requestHeat(EnumFacing.NORTH, 32));
            assertEquals(0, pipe.requestHeat(EnumFacing.NORTH, 1));

            when(world.getTotalWorldTime()).thenReturn(1L);
            assertEquals(5, pipe.requestHeat(EnumFacing.NORTH, 5));
            BiogasHeatBurnerTile loaded = new BiogasHeatBurnerTile();
            loaded.readFromNBT(burner.writeToNBT(new NBTTagCompound()));
            loaded.setWorld(world);
            assertEquals(0, loaded.gas.getFluidAmount());
            assertEquals(27, loaded.requestHeat(EnumFacing.NORTH, 32));
            assertEquals(0, loaded.requestHeat(EnumFacing.NORTH, 1));
        } finally {
            instance.set(FluidName.biogas, old);
        }
    }

    @Test public void returnsZeroHeatSafelyWhenBlockReplacedOrUnloaded() {
        Bootstrap.register();
        World world = mock(World.class);
        BlockPos burnerPos = new BlockPos(0, 64, 0);
        BiogasHeatBurnerTile burner = new BiogasHeatBurnerTile();
        burner.setWorld(world);
        burner.setPos(burnerPos);
        when(world.isBlockLoaded(burnerPos, false)).thenReturn(true);
        when(world.getBlockState(burnerPos)).thenReturn(net.minecraft.init.Blocks.AIR.getDefaultState());
        assertEquals(0, burner.maxrequestHeatTick(EnumFacing.NORTH));
        assertEquals(0, burner.drawHeat(EnumFacing.NORTH, 32, false));
        when(world.isBlockLoaded(burnerPos, false)).thenReturn(false);
        assertEquals(0, burner.maxrequestHeatTick(EnumFacing.NORTH));
        net.minecraft.block.state.IBlockState self = CropManager.BIOGAS_HEAT_BURNER.getDefaultState();
        assertFalse(burner.shouldRefresh(world, burnerPos, self, self.withProperty(AgriculturalMachineBlock.FACING, EnumFacing.SOUTH)));
        assertTrue(burner.shouldRefresh(world, burnerPos, self, net.minecraft.init.Blocks.AIR.getDefaultState()));
    }
}
