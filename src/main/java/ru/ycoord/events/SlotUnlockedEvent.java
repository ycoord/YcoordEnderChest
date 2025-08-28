package ru.ycoord.events;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class SlotUnlockedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final OfflinePlayer owner;
    private final int page;
    private final int slot;

    public OfflinePlayer getOwner() {
        return owner;
    }

    public int getPage() {
        return page;
    }

    public int getSlot() {
        return slot;
    }

    @Override public @NotNull HandlerList getHandlers() { return HANDLERS; }
    public static HandlerList getHandlerList() { return HANDLERS; }
    public SlotUnlockedEvent(OfflinePlayer owner, int page, int slot) {
        super(true);
        this.owner = owner;
        this.page = page;
        this.slot = slot;
    }
}
