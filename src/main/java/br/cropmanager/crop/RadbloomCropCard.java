package br.cropmanager.crop;

import br.cropmanager.CropManager;
import br.cropmanager.ManagerConfig;
import ic2.api.crops.CropProperties;
import ic2.api.crops.ICropTile;
import ic2.core.IC2Potion;
import ic2.core.item.armor.ItemArmorHazmat;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** Catalyzed radioactive crop: requires Radioactive Soil beneath farmland and emits IC2 radiation when mature. */
public final class RadbloomCropCard extends BaseCropCard {
    public RadbloomCropCard(Supplier<ItemStack> gainSupplier) {
        super(
                "radbloom",
                new CropProperties(7, 4, 0, 4, 2, 1),
                new String[] {"Radioactive", "Uranium", "Catalyzed"},
                gainSupplier,
                true
        );
    }

    @Override
    public int getRootsLength(ICropTile crop) {
        // Scans up to 3 blocks below the crop stick (covers farmland at y-1 and subsoil at y-2/y-3).
        return 4;
    }

    @Override
    public boolean canGrow(ICropTile crop) {
        if (!crop.isBlockBelow(CropManager.RADIOACTIVE_SOIL)) return false;
        return super.canGrow(crop);
    }

    @Override
    public int getEmittedLight(ICropTile crop) {
        int size = crop.getCurrentSize();
        return size >= getMaxSize() ? 10 : size == 3 ? 5 : 0;
    }

    @Override
    public void tick(ICropTile crop) {
        if (crop.getCurrentSize() < getMaxSize()) return;
        World world = crop.getWorld();
        if (world == null || world.isRemote) return;
        BlockPos pos = crop.getLocation();
        if (pos == null) return;
        double r = ManagerConfig.radbloomRadiationRadius;
        if (r <= 0 || ManagerConfig.radbloomRadiationDuration <= 0) return;
        AxisAlignedBB box = new AxisAlignedBB(pos).grow(r);
        List<EntityLivingBase> entities = world.getEntitiesWithinAABB(EntityLivingBase.class, box);
        for (EntityLivingBase entity : entities) {
            irradiate(entity);
        }
    }

    @Override
    public boolean onEntityCollision(ICropTile crop, Entity entity) {
        if (crop.getCurrentSize() >= getMaxSize() && entity instanceof EntityLivingBase && !entity.world.isRemote) {
            irradiate((EntityLivingBase) entity);
        }
        return super.onEntityCollision(crop, entity);
    }

    public static boolean shouldIrradiate(EntityLivingBase entity) {
        if (entity == null || !entity.isEntityAlive()) return false;
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            if (player.capabilities.isCreativeMode || player.isSpectator()) return false;
        }
        return !ItemArmorHazmat.hasCompleteHazmat(entity);
    }

    public static boolean irradiate(EntityLivingBase entity) {
        if (!shouldIrradiate(entity) || IC2Potion.radiation == null) return false;
        IC2Potion.radiation.applyTo(entity, ManagerConfig.radbloomRadiationDuration, 0);
        return true;
    }
}
