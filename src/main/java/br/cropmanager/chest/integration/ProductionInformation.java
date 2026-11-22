package br.cropmanager.chest.integration;

import ic2.api.crops.CropCard;
import ic2.api.crops.ICropTile;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.ItemStack;

public final class ProductionInformation {
    public enum Status { HIDDEN, DECLARED, NONE, CONTEXT_REQUIRED, UNAVAILABLE }
    public final Status status;
    public final List<ItemStack> products;

    private ProductionInformation(Status status, List<ItemStack> products) {
        this.status = status;
        this.products = products;
    }
    public static ProductionInformation hidden() { return empty(Status.HIDDEN); }
    private static ProductionInformation empty(Status status) {
        return new ProductionInformation(status, Collections.emptyList());
    }

    public static ProductionInformation query(CropCard crop) {
        List<ItemStack> declared = PackCropProducts.query(crop);
        if (declared != null) return new ProductionInformation(Status.DECLARED, declared);
        // Exact implementation verified in IC2 2.8.222-ex112: getGain only copies cropDrop.
        // Do not extend to subclasses: their overrides may use randomness, world or mutations.
        String implementation = crop.getClass().getName();
        if (!implementation.equals("ic2.core.crop.cropcard.CropBaseMetalCommon")) {
            // Verified variable implementations. Uninspected classes remain unavailable.
            boolean variable = implementation.equals("ic2.core.crop.cropcard.GenericCropCard")
                    || implementation.equals("ic2.core.crop.cropcard.CropPotato")
                    || implementation.equals("ic2.core.crop.cropcard.CropRedWheat");
            return empty(variable ? Status.CONTEXT_REQUIRED : Status.UNAVAILABLE);
        }
        ICropTile context = (ICropTile) Proxy.newProxyInstance(ICropTile.class.getClassLoader(),
                new Class<?>[] {ICropTile.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getCrop")) return crop;
                    throw new UnsupportedOperationException("No live crop context: " + method.getName());
                });
        try {
            ItemStack product = crop.getGain(context);
            return product.isEmpty() ? empty(Status.NONE)
                    : new ProductionInformation(Status.DECLARED, Collections.singletonList(product.copy()));
        } catch (RuntimeException error) {
            // Patched implementations can demand more context. Never fabricate that context.
            return empty(Status.UNAVAILABLE);
        }
    }
}
