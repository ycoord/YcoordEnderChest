package ru.ycoord.menus;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ru.ycoord.YcoordCore;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.core.gui.GuiPagedData;
import ru.ycoord.core.gui.items.GuiItem;
import ru.ycoord.core.gui.items.GuiMultiItem;
import ru.ycoord.core.gui.items.GuiSlot;
import ru.ycoord.core.messages.ChatMessage;
import ru.ycoord.core.messages.MessageBase;
import ru.ycoord.core.transaction.TransactionManager;
import ru.ycoord.examples.guis.ExampleGuiSlotData;
import ru.ycoord.services.EnderChestService;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class EnderChestMenu extends GuiPagedData {
    public EnderChestMenu(ConfigurationSection section) {
        super(section);
        lockOnAnimation = true;
    }

    protected CompletableFuture<Boolean> setItemAsync(OfflinePlayer player, int slot, int page, Inventory inventory, ItemStack item) {

        EnderChestService service = YcoordEnderChest.getInstance().getService();

        return service.setItemAsync(player, player, page, slot, item, true);

    }

    protected String getKey(OfflinePlayer player) {
        return player.getName();
    }

    protected String getValue(OfflinePlayer player) {
        return "SAVE_SLOT_DATA";
    }

    private void saveAsync(OfflinePlayer player, Inventory inventory) {
        TransactionManager.lock(getKey(player), getValue(player));
        Bukkit.getScheduler().runTaskLaterAsynchronously(YcoordCore.getInstance(), (task) -> {
            for (Integer slot : slots.keySet()) {
                GuiItem item = slots.get(slot);
                if (item instanceof GuiSlot) {
                    ItemStack v = inventory.getItem(slot);
                    try {
                        setItemAsync(player, slot, current, inventory, v).get();
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                } else if (item instanceof GuiMultiItem multiItem) {
                    GuiItem currentItem = multiItem.getCurrentSlot(player);
                    if (currentItem instanceof GuiSlot) {
                        ItemStack v = inventory.getItem(slot);
                        try {
                            setItemAsync(player, slot, current, inventory, v).get();
                        } catch (Exception e) {
                            e.fillInStackTrace();
                        }
                    }
                }
            }

            List<? extends Player> players = Bukkit.getOnlinePlayers().stream().filter(p -> p.getPlayer() != player).toList();

            for (Player p : players) {
                InventoryView view = p.getOpenInventory();
                Inventory top = view.getTopInventory();
                if (top.getHolder() instanceof ExampleGuiSlotData g) {
                    g.rebuild(p, false);
                }
            }

            TransactionManager.unlock(getKey(player), getValue(player));
        }, 1);
    }

    static class SlotItem extends GuiMultiItem {
        private final int currentPage;

        private final Locked locked;
        private final Available available;
        private final Unlocked unlocked;


        public SlotItem(int currentPage, int priority, int slot, int index, @Nullable ConfigurationSection section) {
            super(priority, slot, index, section);
            this.currentPage = currentPage;

            assert section != null;

            locked = new Locked(priority, slot, index, section.getConfigurationSection("locked"));
            available = new Available(priority, slot, index, section.getConfigurationSection("available"));
            unlocked = new Unlocked(currentPage, priority, slot, index, section.getConfigurationSection("unlocked"));
        }

        static class Locked extends GuiItem {
            public Locked(int priority, int slot, int index, @Nullable ConfigurationSection section) {
                super(priority, slot, index, section);
            }
        }

        static class Available extends GuiItem {
            public Available(int priority, int slot, int index, @Nullable ConfigurationSection section) {
                super(priority, slot, index, section);
            }
        }

        static class Unlocked extends GuiSlot {
            public Unlocked(int page, int priority, int slot, int index, @Nullable ConfigurationSection section) {
                super(null, priority, slot, index, section);
                provider = (player) -> getItem(player, slot, page);
            }


            protected ItemStack getItem(OfflinePlayer player, int slot, int page) {

                EnderChestService service = YcoordEnderChest.getInstance().getService();

                try {
                    return service.getItemAsync(player, player, page, slot, true).get();
                } catch (Exception e) {
                    e.fillInStackTrace();
                }

                return null;
            }


        }


        @Override
        public GuiItem getCurrentSlot(OfflinePlayer player) {
            EnderChestService service = YcoordEnderChest.getInstance().getService();
            EnderChestService.SlotStatus slotStatus = service.getSlotStatus(player, currentPage, slot);
            return getCurrentSlot(slotStatus);
        }


        GuiItem getCurrentSlot(EnderChestService.SlotStatus slotStatus) {
            switch (slotStatus) {
                case UNLOCKED -> {
                    return unlocked;
                }
                case LOCKED -> {
                    return locked;
                }
                case AVAILABLE_TO_UNLOCK -> {
                    return available;
                }
            }

            return locked;
        }
    }

    @Override
    protected GuiItem getItem(int dataIndex, int currentMarkerIndex, int priority, OfflinePlayer player, int slotIndex, String type, ConfigurationSection config) {
        return new SlotItem(current, priority, slotIndex, currentMarkerIndex, config);
        //BlockedSlot blockedSlot = new BlockedSlot(priority, slotIndex, currentMarkerIndex, 0, config);
        //MessagePlaceholders placeholders = new MessagePlaceholders(player);
        //getExtraPlaceholders(placeholders);
        //blockedSlot.getExtraPlaceholders(placeholders, slotIndex, 0, this);
        //boolean condition = new EnderChestService().slotUnlocked(player, slotIndex, current);
        //if (!condition)
        //    return blockedSlot;
        //return new GuiSlot(() -> getItem(player, slotIndex, current), priority, slotIndex, currentMarkerIndex, config);

    }

    @Override
    protected int getItemCount(OfflinePlayer player) {
        return YcoordEnderChest.getInstance().getConfig().getInt("max-slots", 52);
    }

    boolean canNotProcess(OfflinePlayer clicker) {
        if (TransactionManager.inProgress(getKey(clicker), getValue(clicker))) {
            ChatMessage message = YcoordCore.getInstance().getChatMessage();
            message.sendMessageIdAsync(MessageBase.Level.INFO, clicker, "messages.data-is-loading");
            return true;
        }
        return false;
    }

    boolean handleEvent(OfflinePlayer clicker, InventoryInteractEvent e) {
        if (canNotProcess(clicker)) {
            e.setCancelled(true);
            return false;
        }
        saveAsync(clicker, e.getInventory());
        return true;
    }

    @Override
    public void handleClickInventory(Player clicker, InventoryClickEvent e) {
        super.handleClickInventory(clicker, e);
        if (e.isCancelled())
            return;
        handleEvent(clicker, e);
    }

    @Override
    public void handleClick(Player clicker, InventoryClickEvent e) {
        super.handleClick(clicker, e);
        if (e.isCancelled())
            return;
        handleEvent(clicker, e);
    }

    @Override
    public void handleDrag(Player clicker, InventoryDragEvent e) {
        super.handleDrag(clicker, e);
        if (e.isCancelled())
            return;
        handleEvent(clicker, e);
    }

    @Override
    public void open(OfflinePlayer player) {
        if (canNotProcess(player)) {
            return;
        }
        super.open(player);
    }
}
