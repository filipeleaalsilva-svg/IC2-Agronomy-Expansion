package br.cropmanager.chest.inventory;

/** Separate tile ID keeps capacity fixed when loading worlds and synchronizing chunks. */
public final class TileCropDatabaseChest250 extends TileCropDatabaseChest {
    public TileCropDatabaseChest250() { super(500); }
    @Override public String getName() { return "tile.cropmanager.crop_database_chest.name"; }
}
