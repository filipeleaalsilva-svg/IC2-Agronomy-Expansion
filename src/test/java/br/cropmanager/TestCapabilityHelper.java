package br.cropmanager;

import java.lang.reflect.Field;
import java.util.Map;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public final class TestCapabilityHelper {
    private TestCapabilityHelper() {}

    @SuppressWarnings("unchecked")
    public static synchronized void ensureFluidCapabilities() {
        try {
            Field providersField = CapabilityManager.class.getDeclaredField("providers");
            providersField.setAccessible(true);
            Map<String, Capability<?>> providers = (Map<String, Capability<?>>) providersField.get(CapabilityManager.INSTANCE);
            if (!providers.containsKey(IFluidHandler.class.getName().intern())) {
                CapabilityFluidHandler.register();
            }
            if (CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY == null) {
                Field f = CapabilityFluidHandler.class.getDeclaredField("FLUID_HANDLER_CAPABILITY");
                f.setAccessible(true);
                f.set(null, providers.get(IFluidHandler.class.getName().intern()));
            }
            if (CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY == null) {
                Field f = CapabilityFluidHandler.class.getDeclaredField("FLUID_HANDLER_ITEM_CAPABILITY");
                f.setAccessible(true);
                f.set(null, providers.get(IFluidHandlerItem.class.getName().intern()));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
