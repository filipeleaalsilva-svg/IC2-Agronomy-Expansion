package br.cropmanager;

import ic2.api.crops.Crops;
import ic2.core.crop.TileEntityCrop;
import ic2.core.ref.FluidName;
import javax.annotation.Nullable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.*;

/**
 * Entidade de bloco do Capinador a Biogás.
 *
 * <p>Consome biogás IC2 para varrer uma área configurável e remover apenas ervas daninhas reais
 * ({@link Crops#weed}), preservando estacas vazias e cultivos de alto crescimento sem usar {@code CropCard.isWeed}.
 */
public final class WeederTile extends FarmTile implements ITickable {
    public int serverMaxRadius=ManagerConfig.maxRadius;
    public int radius=ManagerConfig.defaultRadius,layers=ManagerConfig.defaultLayers,redstoneMode,removed,cursor;
    public boolean enabled=true,paused;
    private int activeTicks;
    public final FluidTank gas=new FluidTank(ManagerConfig.weederGasCapacity) {
        @Override public boolean canFillFluidType(FluidStack f){return f!=null && f.getFluid()==FluidName.biogas.getInstance();}
        @Override protected void onContentsChanged(){markDirty();}
    };
    public final ItemStackHandler inventory=new ItemStackHandler(2) {
        @Override public boolean isItemValid(int slot,ItemStack stack){FluidStack f=FluidUtil.getFluidContained(stack);return slot==0 && f!=null && f.getFluid()==FluidName.biogas.getInstance();}
        @Override protected void onContentsChanged(int slot){markDirty();}
    };
    @Override public boolean shouldRefresh(net.minecraft.world.World w,BlockPos p,net.minecraft.block.state.IBlockState a,net.minecraft.block.state.IBlockState b){return a.getBlock()!=b.getBlock();}
    @Override public void update(){
        if(world.isRemote)return;emptyFluidContainer(inventory,0,1,gas);if(activeTicks>0)activeTicks--;
        paused=!enabled || !ManagerTile.redstoneAllows(redstoneMode,redstoneMode!=0 && world.isAreaLoaded(pos.add(-2,-2,-2),pos.add(2,2,2),false) && world.isBlockPowered(pos));
        if(!paused)for(int i=0;i<ManagerConfig.weederScansPerTick && (ManagerConfig.weederScanGasCostMB==0 || gas.getFluidAmount()>=ManagerConfig.weederScanGasCostMB);i++)scanOne();
        setActive(!paused && activeTicks>0);
    }
    boolean scanOne(){
        int[] offset=ScanLogic.offset(cursor,radius,layers);cursor=(cursor+1)%ScanLogic.volume(radius,layers);markDirty();BlockPos target=pos.add(offset[0],offset[1],offset[2]);
        if(target.getY()<0 || target.getY()>=world.getHeight() || !world.isBlockLoaded(target,false) || gas.getFluidAmount()<ManagerConfig.weederScanGasCostMB)return false;
        TileEntity tile=world.getTileEntity(target);
        if(!(tile instanceof TileEntityCrop))return false;
        if(ManagerConfig.weederScanGasCostMB>0)gas.drain(ManagerConfig.weederScanGasCostMB,true);
        TileEntityCrop crop=(TileEntityCrop)tile;
        // CropCard.isWeed also flags valuable high-growth crops; never use it for deletion.
        if(crop.getCrop()!=Crops.weed || gas.getFluidAmount()<ManagerConfig.weederRemoveGasCostMB)return false;
        crop.reset();crop.updateState();world.markChunkDirty(target,crop);if(ManagerConfig.weederRemoveGasCostMB>0)gas.drain(ManagerConfig.weederRemoveGasCostMB,true);if(removed<Integer.MAX_VALUE)removed++;activeTicks=8;markDirty();return true;
    }
    @Override public boolean control(int action,EntityPlayer player){
        if(action==200)enabled=!enabled;
        else if(action==13)redstoneMode=(redstoneMode+1)%3;
        else if(action>=210 && action<=214){int[] next=ManagerSettings.applyControl(action<212?action-210+4:action-212+6,radius,layers,0,ManagerConfig.maxRadius);if(next==null)return false;radius=next[0];layers=next[1];cursor=0;}
        else if(action==215){radius=ManagerConfig.defaultRadius;layers=ManagerConfig.defaultLayers;cursor=0;}
        else return false;markDirty();return true;
    }
    private final IItemHandler items=new IItemHandler(){
        public int getSlots(){return 2;} public ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slot);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return inventory.isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==1?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 64;}
    };
    @Override public boolean hasCapability(Capability<?> c,@Nullable EnumFacing f){return c==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY || c==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY || super.hasCapability(c,f);}
    @Override public <T>T getCapability(Capability<T> c,@Nullable EnumFacing f){return c==CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY?CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(gas):c==CapabilityItemHandler.ITEM_HANDLER_CAPABILITY?CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(items):super.getCapability(c,f);}
    @Override public NBTTagCompound guiState(){NBTTagCompound t=new NBTTagCompound();t.setInteger("MaxRadius",ManagerConfig.maxRadius);t.setInteger("Gas",gas.getFluidAmount());t.setInteger("GasCapacity",gas.getCapacity());t.setInteger("Removed",removed);t.setInteger("Radius",radius);t.setInteger("Layers",layers);t.setInteger("Redstone",redstoneMode);t.setBoolean("Enabled",enabled);t.setBoolean("Paused",paused);return t;}
    @Override public void acceptGuiState(NBTTagCompound t){serverMaxRadius=t.getInteger("MaxRadius");if(t.getInteger("GasCapacity")>0)gas.setCapacity(t.getInteger("GasCapacity"));gas.setFluid(t.getInteger("Gas")>0?new FluidStack(FluidName.biogas.getInstance(),t.getInteger("Gas")):null);removed=t.getInteger("Removed");radius=t.getInteger("Radius");layers=t.getInteger("Layers");redstoneMode=t.getInteger("Redstone");enabled=t.getBoolean("Enabled");paused=t.getBoolean("Paused");}
    @Override public NBTTagCompound writeToNBT(NBTTagCompound t){super.writeToNBT(t);t.setTag("Gas",gas.writeToNBT(new NBTTagCompound()));t.setTag("Inventory",inventory.serializeNBT());t.setInteger("Removed",removed);t.setInteger("Radius",radius);t.setInteger("Layers",layers);t.setInteger("Redstone",redstoneMode);t.setBoolean("Enabled",enabled);t.setInteger("Cursor",cursor);return t;}
    @Override public void readFromNBT(NBTTagCompound t){super.readFromNBT(t);gas.readFromNBT(t.getCompoundTag("Gas"));resizeTank(gas,ManagerConfig.weederGasCapacity);NBTTagCompound items=t.getCompoundTag("Inventory").copy();items.setInteger("Size",2);inventory.deserializeNBT(items);removed=Math.max(0,t.getInteger("Removed"));radius=Math.max(1,Math.min(ManagerConfig.maxRadius,t.hasKey("Radius")?t.getInteger("Radius"):ManagerConfig.defaultRadius));layers=t.getInteger("Layers")&7;if(layers==0)layers=ManagerConfig.defaultLayers;redstoneMode=Math.max(0,Math.min(2,t.getInteger("Redstone")));enabled=!t.hasKey("Enabled") || t.getBoolean("Enabled");cursor=Math.floorMod(t.getInteger("Cursor"),ScanLogic.volume(radius,layers));}
    @Override public void dropContents(){dropHandler(inventory);}
}
