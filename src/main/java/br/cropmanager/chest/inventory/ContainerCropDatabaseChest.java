package br.cropmanager.chest.inventory;

import br.cropmanager.chest.integration.SeedSearch;
import br.cropmanager.chest.item.ItemUpgradeLinker;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

/**
 * Container paginado e filtrável compartilhado pelo Baú de Dados de Cultivos, Database Portátil
 * e acesso remoto vinculado (GUI 11, 12 e 13).
 *
 * <p>Mantém um mapeamento dinâmico entre índices reais de armazenamento (0..499) e posições visíveis
 * na grade 5×5 usando controle de revisão ({@code revision}/{@code acknowledged}) para evitar cliques
 * dessincronizados quando pipes ou coleta magnética alteram o inventário em segundo plano.
 */
public final class ContainerCropDatabaseChest extends Container {
    public static final int GRID_X = 99, GRID_Y = 15;
    public static final int PLAYER_X = 17, PLAYER_Y = 133, HOTBAR_Y = 191;
    private final TileCropDatabaseChest chest;
    private final int linkedCarrierSlot;
    private final int capacity;
    private final int[] positions;
    private final int[] pendingPositions;
    private final long[] visibleSlotRevisions;
    private int page, pageCount, occupied, revision, acknowledged = -1;
    private boolean dragging, closed;
    private long contentsRevision = -1;
    private long sentContentsRevision = -1;
    private int sentPageCount;
    private boolean sentPickup;
    private boolean favoritesOnly;

    private String query = "";
    private Set<String> translatedMatches = Collections.emptySet();

    public ContainerCropDatabaseChest(InventoryPlayer player, TileCropDatabaseChest chest) {
        this(player, chest, -1);
    }

    public ContainerCropDatabaseChest(InventoryPlayer player, TileCropDatabaseChest chest, int linkedCarrierSlot) {
        this.chest = chest;
        this.linkedCarrierSlot = linkedCarrierSlot;
        capacity = chest.getSizeInventory();
        positions = new int[capacity];
        pendingPositions = new int[capacity];
        visibleSlotRevisions = new long[capacity];
        Arrays.fill(positions, -1);
        Arrays.fill(pendingPositions, -1);
        // Network indices always refer to real storage slots, never filtered positions.
        for (int i = 0; i < capacity; i++) addSlotToContainer(new SeedSlot(chest, i));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            addSlotToContainer(playerSlot(player, 9 + row * 9 + col, PLAYER_X + col * 18, PLAYER_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlotToContainer(playerSlot(player, col, PLAYER_X + col * 18, HOTBAR_Y));
        rebuildView();
        sentContentsRevision = contentsRevision;
        sentPageCount = pageCount;
        chest.openInventory(player.player);
    }

    public static final int TOGGLE_PICKUP = 110;
    public PortableCropInventory getPortableInventory() { return isPortable() ? (PortableCropInventory) chest : null; }
    public boolean isPickupEnabled() { return isPortable() && getPortableInventory().isPickupEnabled(); }
    public int getCapacity() { return chest.getActiveCapacity(); }
    public boolean hasManagement() { return chest.hasManagement(); }
    public boolean favoritesOnly() { return favoritesOnly; }
    public boolean collectingEnabled() { return chest.collectingEnabled(); }
    public int redstoneMode() { return chest.redstoneMode(); }
    public boolean isFavorite(int index) { return isSeedFavorite(index); }
    public boolean isSeedFavorite(int index) { return isPortable() ? getPortableInventory().isFavorite(chest.getStackInSlot(index)) : chest.isFavorite(chest.getStackInSlot(index)); }
    public void toggleFavorite(EntityPlayer player, int index, int expectedRevision) {
        if ((!hasManagement() && !isPortable()) || !canChangeView(player) || index < 0 || index >= capacity) return;
        rebuildView();
        if (expectedRevision != revision || !visible(index) || !SeedInventory.accepts(chest.getStackInSlot(index))) return;
        if (isPortable()) getPortableInventory().toggleFavorite(chest.getStackInSlot(index));
        else chest.toggleFavorite(index);
        rebuildView();
        for (IContainerListener listener : listeners) sendView(listener);
    }
    public void readManagement(net.minecraft.nbt.NBTTagCompound tag) { chest.readManagement(tag); }
    public boolean isPortable() { return chest.isPortable(); }
    public boolean isLinkedRemote() { return linkedCarrierSlot >= 0; }

    public int getPage() { return page; }
    public int getPageCount() { return pageCount; }
    public int getOccupied() { return occupied; }
    public int getRevision() { return revision; }
    public long getContentsRevision() { return chest.getContentsRevision(); }
    public int getPosition(int slot) { return positions[slot]; }
    public boolean usesChest(TileCropDatabaseChest tile) { return chest == tile; }
    private boolean visible(int index) { return positions[index] >= 0; }
    public void acknowledgeView(int value) { acknowledged = value; }
    private boolean isLockedCarrierSlot(int index) { return chest.isCarrierSlot(index) || (linkedCarrierSlot >= 0 && index == linkedCarrierSlot); }
    private Slot playerSlot(InventoryPlayer player, int index, int x, int y) {
        return new Slot(player, index, x, y) {
            @Override public boolean canTakeStack(EntityPlayer owner) { return !isLockedCarrierSlot(index); }
            @Override public boolean isItemValid(ItemStack stack) { return !isLockedCarrierSlot(index) && super.isItemValid(stack); }
        };
    }

    private void positionSlots() {
        for (int i = 0; i < capacity; i++) {
            Slot slot = inventorySlots.get(i);
            int position = positions[i];
            slot.xPos = position >= 0 ? GRID_X + position % SeedInventory.COLUMNS * 18 : -10000;
            slot.yPos = position >= 0 ? GRID_Y + position / SeedInventory.COLUMNS * 18 : -10000;
        }
    }

    private void rebuildView() {
        if (!hasManagement() && !isPortable()) favoritesOnly = false;
        boolean contentsChanged = contentsRevision != chest.getContentsRevision();
        if (contentsChanged) {
            contentsRevision = chest.getContentsRevision();
            revision = (revision + 1) & 32767;
        }
        int[] matches = new int[capacity];
        int count = 0;
        occupied = 0;
        for (int i = 0; i < capacity; i++) {
            ItemStack stack = chest.getStackInSlot(i);
            if (!stack.isEmpty()) occupied++;
            if (i < chest.getActiveCapacity() && (!favoritesOnly || isFavorite(i)) && SeedSearch.matches(stack, query, translatedMatches)) matches[count++] = i;
        }
        pageCount = Math.max(1, (count + SeedInventory.PAGE_SIZE - 1) / SeedInventory.PAGE_SIZE);
        if (page >= pageCount) page = 0;
        int[] updated = new int[capacity];
        Arrays.fill(updated, -1);
        int start = page * SeedInventory.PAGE_SIZE;
        for (int i = start; i < Math.min(count, start + SeedInventory.PAGE_SIZE); i++) updated[matches[i]] = i - start;
        if (!Arrays.equals(positions, updated)) {
            System.arraycopy(updated, 0, positions, 0, positions.length);
            revision = (revision + 1) & 32767;
            positionSlots();
        }
        for (int i = 0; i < capacity; i++) {
            long slotRevision = chest.getSlotRevision(i);
            if (visibleSlotRevisions[i] != slotRevision) {
                visibleSlotRevisions[i] = slotRevision;
                revision = (revision + 1) & 32767;
            }
        }
    }

    private void sendView(IContainerListener listener) {
        if (hasManagement() && listener instanceof EntityPlayerMP) {
            br.cropmanager.ManagerNetwork.CHANNEL.sendTo(
                    new br.cropmanager.chest.network.ChestSettingsMessage(windowId, chest.managementTag()),
                    (EntityPlayerMP) listener);
        }
        if (listener instanceof EntityPlayerMP) {
            br.cropmanager.ManagerNetwork.CHANNEL.sendTo(
                    new br.cropmanager.chest.network.ChestViewMessage(
                            windowId, page, pageCount, occupied, isPickupEnabled(), favoritesOnly, revision, positions),
                    (EntityPlayerMP) listener);
            return;
        }
        listener.sendWindowProperty(this, 0, page);
        listener.sendWindowProperty(this, 1, pageCount);
        listener.sendWindowProperty(this, 2, occupied);
        listener.sendWindowProperty(this, 4, isPickupEnabled() ? 1 : 0);
        listener.sendWindowProperty(this, 5, favoritesOnly ? 1 : 0);

        for (int i = 0; i < positions.length; i++) listener.sendWindowProperty(this, 10 + i, positions[i]);
        // Commit after every position: the client never displays a partially received mapping.
        listener.sendWindowProperty(this, 3, revision);
    }

    public void applyView(int page, int pageCount, int occupied, boolean pickup,
            boolean favoritesOnly, int revision, int[] mappedPositions) {
        this.page = page;
        this.pageCount = Math.max(1, pageCount);
        this.occupied = occupied;
        if (isPortable()) getPortableInventory().setPickupEnabled(pickup);
        this.favoritesOnly = favoritesOnly;
        int len = Math.min(positions.length, mappedPositions.length);
        Arrays.fill(positions, -1);
        System.arraycopy(mappedPositions, 0, positions, 0, len);
        System.arraycopy(positions, 0, pendingPositions, 0, positions.length);
        this.revision = revision;
        positionSlots();
    }

    @Override public void detectAndSendChanges() {
        if (chest == null || (!isPortable() && (chest.getWorld() == null || chest.getWorld().isRemote))) return;
        int oldRevision = revision, oldOccupied = occupied, oldPage = page;
        long oldContentsRevision = contentsRevision;
        rebuildView();
        super.detectAndSendChanges();
        if (revision != oldRevision || occupied != oldOccupied || page != oldPage || sentPageCount != pageCount
                || sentContentsRevision != contentsRevision || contentsRevision != oldContentsRevision || sentPickup != isPickupEnabled()) {
            for (IContainerListener listener : listeners) sendView(listener);
            sentPickup = isPickupEnabled();
            sentContentsRevision = contentsRevision;
            sentPageCount = pageCount;
        }

    }

    @Override public boolean canInteractWith(EntityPlayer player) {
        if (linkedCarrierSlot >= 0) {
            ItemStack carrier = player.inventory.getStackInSlot(linkedCarrierSlot);
            return ItemUpgradeLinker.hasLink(carrier) && chest.isRemotelyUsableByPlayer(player);
        }
        return chest.isUsableByPlayer(player);
    }
    @Override public void addListener(IContainerListener listener) {
        super.addListener(listener);
        sendView(listener);

    }
    @Override public void updateProgressBar(int id, int value) {
        if (id == 0) page = value;
        else if (id == 1) pageCount = Math.max(1, value);
        else if (id == 2) occupied = value;
        else if (id == 4 && isPortable()) getPortableInventory().setPickupEnabled(value != 0);
        else if (id == 5) favoritesOnly = value != 0;

        else if (id >= 10 && id < 10 + positions.length) pendingPositions[id - 10] = value;
        else if (id == 3) {
            System.arraycopy(pendingPositions, 0, positions, 0, positions.length);
            revision = value;
            positionSlots();
        }
    }

    public boolean setSearch(EntityPlayer player, String text, Set<String> names) {
        if (!canChangeView(player) || text.length() > 64 || names.size() > capacity) return false;
        String normalized = text.trim().toLowerCase(Locale.ROOT);
        if (!query.equals(normalized)) page = 0;
        query = normalized;
        translatedMatches = new HashSet<>(names);
        if (isPortable()) contentsRevision = chest.getContentsRevision();
        rebuildView();
        for (IContainerListener listener : listeners) sendView(listener);
        super.detectAndSendChanges();
        return true;
    }
    private boolean canChangeView(EntityPlayer player) {
        return !player.world.isRemote && canInteractWith(player) && !dragging
                && player.inventory.getItemStack().isEmpty();
    }

    @Override public boolean enchantItem(EntityPlayer player, int request) {
        if (!canChangeView(player)) return false;
        if (request >= 111 && request <= 114) {
            if (request == 111) { if (!hasManagement() && !isPortable()) return false; favoritesOnly = !favoritesOnly; page = 0; }
            else {
                if (!hasManagement()) return false;
                if (request == 112) chest.deposit(player);
                else if (request == 113) chest.setCollectingEnabled(!chest.collectingEnabled());
                else if (request == 114) chest.setRedstoneMode((chest.redstoneMode() + 1) % 3);
            }
            rebuildView(); for (IContainerListener listener : listeners) sendView(listener); super.detectAndSendChanges(); return true;
        }
        if (request == TOGGLE_PICKUP) {
            if (!isPortable()) return false;
            getPortableInventory().setPickupEnabled(!isPickupEnabled());
            for (IContainerListener listener : listeners) sendView(listener);
            return true;
        }
        if (request == 115) {
            if (!hasManagement()) return false;
            chest.setCollectingEnabled(!chest.collectingEnabled());
            for (IContainerListener listener : listeners) sendView(listener);
            return true;
        }
        SeedSortMode mode = SeedSortMode.fromRequestId(request);
        if (mode != null) {
            chest.seeds().sort(mode);
            page = 0;
        } else {
            if (request < 0 || request >= pageCount) return false;
            page = request;
        }
        if (isPortable()) contentsRevision = chest.getContentsRevision();
        rebuildView();
        for (IContainerListener listener : listeners) sendView(listener);
        super.detectAndSendChanges();
        return true;
    }

    @Override public ItemStack slotClick(int slotId, int button, ClickType type, EntityPlayer player) {
        if (!canInteractWith(player)) return ItemStack.EMPTY;
        if (type == ClickType.SWAP && isLockedCarrierSlot(button)) return ItemStack.EMPTY;
        if (slotId >= 0 && slotId < inventorySlots.size()) {
            Slot clicked = inventorySlots.get(slotId);
            if (clicked.inventory == player.inventory && isLockedCarrierSlot(clicked.getSlotIndex())) return ItemStack.EMPTY;
        }
        // Rebuild before checking the acknowledged layout; pipes can change it between ticks.
        if (!player.world.isRemote) rebuildView();
        boolean current = !(player instanceof EntityPlayerMP) || acknowledged == revision;
        if (!current || (slotId >= 0 && slotId < capacity && !visible(slotId))) {
            dragging = false;
            resetDrag();
            if (player instanceof EntityPlayerMP) {
                ((EntityPlayerMP) player).sendAllContents(this, getInventory());
                ((EntityPlayerMP) player).updateHeldItem();
            }
            for (IContainerListener listener : listeners) sendView(listener);
            return ItemStack.EMPTY;
        }
        // Prevent right-click split/duplication or creative clone on chest seed slots.
        if (slotId >= 0 && slotId < capacity && (type == ClickType.CLONE || (button == 1 && type == ClickType.PICKUP))) {
            return ItemStack.EMPTY;
        }
        if (type == ClickType.QUICK_CRAFT) {
            int stage = button & 3;
            if (stage == 0) dragging = true;
            else if (stage == 2) dragging = false;
        } else dragging = false;
        long revBefore = chest.getContentsRevision();
        ItemStack result = super.slotClick(slotId, button, type, player);
        if (chest.getContentsRevision() != revBefore && !player.world.isRemote) {
            rebuildView();
            if (player instanceof EntityPlayerMP) {
                if (isPortable()) ((EntityPlayerMP) player).sendAllContents(this, getInventory());
                for (IContainerListener listener : listeners) sendView(listener);
                sentContentsRevision = contentsRevision;
                sentPageCount = pageCount;
                acknowledged = revision;
            }
        }
        return result;
    }
    @Override public boolean canDragIntoSlot(Slot slot) {
        return (slot.inventory == chest ? visible(slot.getSlotIndex()) : !isLockedCarrierSlot(slot.getSlotIndex()));
    }
    @Override public boolean canMergeSlot(ItemStack stack, Slot slot) {
        return (slot.inventory == chest ? visible(slot.getSlotIndex()) : !isLockedCarrierSlot(slot.getSlotIndex())) && super.canMergeSlot(stack, slot);
    }
    @Override public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!canInteractWith(player) || index < 0 || index >= inventorySlots.size()
                || (index < capacity && !visible(index))) return ItemStack.EMPTY;
        Slot source = inventorySlots.get(index);
        if (source.inventory == player.inventory && isLockedCarrierSlot(source.getSlotIndex())) return ItemStack.EMPTY;
        if (!source.getHasStack()) return ItemStack.EMPTY;
        ItemStack stack = source.getStack();
        ItemStack original = stack.copy();
        if (index < capacity) {

            if (!mergeItemStack(stack, capacity, inventorySlots.size(), true)) return ItemStack.EMPTY;
        } else {
            ItemStack remainder = chest.seeds().insertAnywhere(stack, false);
            if (remainder.getCount() == stack.getCount()) return ItemStack.EMPTY;
            stack.setCount(remainder.getCount());
        }
        if (stack.isEmpty()) source.putStack(ItemStack.EMPTY);
        else source.onSlotChanged();
        source.onTake(player, stack);
        return original;
    }
    @Override public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        if (!closed) { closed = true; chest.closeInventory(player); }
    }
    private final class SeedSlot extends Slot {
        SeedSlot(TileCropDatabaseChest inventory, int index) { super(inventory, index, 0, 0); }
        @Override public boolean isItemValid(ItemStack stack) { return SeedInventory.accepts(stack); }
        @Override public int getSlotStackLimit() { return 1; }
        @Override public boolean isEnabled() { return visible(getSlotIndex()); }

    }
}
