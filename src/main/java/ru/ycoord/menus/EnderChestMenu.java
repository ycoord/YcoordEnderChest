package ru.ycoord.menus;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import ru.ycoord.YcoordCore;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.core.balance.Balance;
import ru.ycoord.core.gui.GuiBase;
import ru.ycoord.core.gui.GuiPagedData;
import ru.ycoord.core.gui.items.GuiHeadItem;
import ru.ycoord.core.gui.items.GuiItem;
import ru.ycoord.core.gui.items.GuiMultiItem;
import ru.ycoord.core.gui.items.GuiSlot;
import ru.ycoord.core.messages.ChatMessage;
import ru.ycoord.core.messages.MessageBase;
import ru.ycoord.core.messages.MessagePlaceholders;
import ru.ycoord.core.transaction.TransactionManager;
import ru.ycoord.examples.guis.ExampleGuiSlotData;
import ru.ycoord.services.EnderChestService;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class EnderChestMenu extends GuiPagedData {
    private final OfflinePlayer target;

    public EnderChestMenu(OfflinePlayer target, ConfigurationSection section) {
        super(section);
        this.target = target;
        lockOnAnimation = true;
    }

    protected CompletableFuture<Boolean> setItemAsync(OfflinePlayer player, int slot, int page, ItemStack item) {

        EnderChestService service = YcoordEnderChest.getInstance().getService();

        return service.setItemAsync(player, player, page, slot, item, true);

    }

    @Override
    public void getExtraPlaceholders(MessagePlaceholders placeholders) {
        super.getExtraPlaceholders(placeholders);
        placeholders.put("%owner%", target.getName());
    }

    @Override
    public GuiItem makeItem(int currentIndex, int slot, int priority, OfflinePlayer player, String type, ConfigurationSection section) {
        if (type.equalsIgnoreCase("OWNER_HEAD")) {
            return new GuiHeadItem(target.getName() == null ? target.getUniqueId().toString() : target.getName(), priority, slot, currentIndex, section);
        }
        return super.makeItem(currentIndex, slot, priority, player, type, section);
    }

    protected String getKey(OfflinePlayer player) {
        return player.getName();
    }

    protected String getValue(OfflinePlayer player) {
        return "SAVE_SLOT_DATA";
    }

    private void saveAsync(OfflinePlayer clicker, OfflinePlayer player, Inventory inventory, boolean updateForClicker) {
        TransactionManager.lock(getKey(player), getValue(player));
        Bukkit.getScheduler().runTaskLaterAsynchronously(YcoordCore.getInstance(), (task) -> {
            for (Integer slot : slots.keySet()) {
                GuiItem item = slots.get(slot);
                if (item instanceof GuiSlot) {
                    ItemStack v = inventory.getItem(slot);
                    try {
                        setItemAsync(player, slot, current, v).get();
                    } catch (Exception e) {
                        e.fillInStackTrace();
                    }
                } else if (item instanceof GuiMultiItem multiItem) {
                    GuiItem currentItem = multiItem.getCurrentSlot(player);
                    if (currentItem instanceof GuiSlot) {
                        ItemStack v = inventory.getItem(slot);
                        try {
                            setItemAsync(player, slot, current, v).get();
                        } catch (Exception e) {
                            e.fillInStackTrace();
                        }
                    }
                }
            }

            List<? extends Player> players = Bukkit.getOnlinePlayers().stream().filter(p -> updateForClicker || p.getPlayer() != clicker).toList();

            for (Player p : players) {
                InventoryView view = p.getOpenInventory();
                Inventory top = view.getTopInventory();
                if (top.getHolder() instanceof EnderChestMenu g) {
                    if (g.target == target) {
                        g.rebuild(p, false);
                    }
                }
            }

            TransactionManager.unlock(getKey(player), getValue(player));
        }, 1);
    }

    class SlotItem extends GuiMultiItem {
        private final OfflinePlayer target;
        private final int currentPage;

        private final Locked locked;
        private final Available available;
        private final Unlocked unlocked;


        public SlotItem(OfflinePlayer target, int currentPage, int priority, int slot, int index, @Nullable ConfigurationSection section) {
            super(priority, slot, index, section);
            this.target = target;
            this.currentPage = currentPage;

            assert section != null;

            locked = new Locked(priority, slot, index, section.getConfigurationSection("locked"));
            available = new Available(priority, slot, index, section.getConfigurationSection("available"));
            unlocked = new Unlocked(currentPage, priority, slot, index, section.getConfigurationSection("unlocked"));
        }

        class Locked extends GuiItem {
            public Locked(int priority, int slot, int index, @Nullable ConfigurationSection section) {
                super(priority, slot, index, section);
            }

            @Override
            public boolean handleClick(GuiBase gui, InventoryClickEvent event, MessagePlaceholders placeholders) {
                if (!super.handleClick(gui, event, placeholders))
                    return false;

                if (event.getWhoClicked() instanceof Player clicker) {
                    if (!target.equals(clicker)) {
                        ChatMessage message = YcoordCore.getInstance().getChatMessage();
                        message.sendMessageIdAsync(MessageBase.Level.ERROR, clicker, "messages.ec-no-permission-use-other");
                        return false;
                    }
                }
                return true;
            }

            @Override
            public List<String> getLoreAfter(OfflinePlayer ignored) {
                List<String> lore = super.getLoreAfter(ignored);
                EnderChestService service = YcoordEnderChest.getInstance().getService();
                String descId = service.getDescId(currentPage, slot);
                if (descId == null)
                    return lore;
                lore.add(YcoordCore.getInstance().getChatMessage().makeMessageId(MessageBase.Level.NONE, descId, new MessagePlaceholders(ignored)));

                return lore;
            }
        }

        class Available extends GuiItem {
            public Available(int priority, int slot, int index, @Nullable ConfigurationSection section) {
                super(priority, slot, index, section);
            }

            @Override
            public void getExtraPlaceholders(OfflinePlayer player, MessagePlaceholders placeholders, int slot, int index, GuiBase base) {
                super.getExtraPlaceholders(player, placeholders, slot, index, base);
                EnderChestService service = YcoordEnderChest.getInstance().getService();
                Integer price = service.getPrice(target, currentPage, slot);
                String currency = service.getCurrency(currentPage, slot);

                Balance balance = YcoordCore.getInstance().getBalance(currency);
                if (price != null) {
                    assert balance != null;
                    placeholders.put("%price%", balance.format(price));
                }
            }

            @Override
            public List<String> getLoreAfter(OfflinePlayer ignored) {
                List<String> lore = super.getLoreAfter(ignored);
                EnderChestService service = YcoordEnderChest.getInstance().getService();
                String descId = service.getDescId(currentPage, slot);
                if (descId == null)
                    return lore;

                lore.add(YcoordCore.getInstance().getChatMessage().makeMessageId(MessageBase.Level.NONE, descId, new MessagePlaceholders(ignored)));

                if (service.canUnlockSlot(Objects.requireNonNull(ignored.getPlayer()))) {
                    lore.add("");
                    lore.add(YcoordCore.getInstance().getChatMessage().makeMessageId(MessageBase.Level.NONE, "messages.ec-can-unlock", new MessagePlaceholders(ignored)));
                }


                return lore;
            }

            @Override
            public boolean handleClick(GuiBase gui, InventoryClickEvent event, MessagePlaceholders placeholders) {
                if (!super.handleClick(gui, event, placeholders))
                    return false;

                EnderChestService service = YcoordEnderChest.getInstance().getService();

                if (event.getWhoClicked() instanceof Player clicker) {
                    if (service.unlockSlotForce(clicker, target, currentPage, slot)) {
                        saveAsync(clicker, target, event.getInventory(), true);
                        return true;
                    }

                    if (!service.canBuySlots(clicker, target))
                        return false;


                    if (service.unlockSlot(clicker, target, currentPage, slot))
                        saveAsync(clicker, target, event.getInventory(), true);
                }

                return true;
            }
        }

        class Unlocked extends GuiSlot {
            public Unlocked(int page, int priority, int slot, int index, @Nullable ConfigurationSection section) {
                super(null, priority, slot, index, section);
                provider = (player) -> getItem(player, slot, page);
            }


            protected ItemStack getItem(OfflinePlayer player, int slot, int page) {

                EnderChestService service = YcoordEnderChest.getInstance().getService();

                try {
                    return service.getItemAsync(player, target, page, slot, true).get();
                } catch (Exception e) {
                    e.fillInStackTrace();
                }

                return null;
            }
        }


        @Override
        public GuiItem getCurrentSlot(OfflinePlayer player) {
            EnderChestService service = YcoordEnderChest.getInstance().getService();
            EnderChestService.SlotStatus slotStatus = service.getSlotStatus(target, currentPage, slot);
            return getCurrentSlot(slotStatus);
        }


        private GuiItem getCurrentSlot(EnderChestService.SlotStatus slotStatus) {
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
        return new SlotItem(target, current, priority, slotIndex, currentMarkerIndex, config);
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

    private void handleEvent(OfflinePlayer clicker, InventoryInteractEvent e) {
        if (!YcoordEnderChest.getInstance().getService().canProcessClick(clicker, target)) {
            e.setCancelled(true);
            return;
        }
        if (canNotProcess(target)) {
            e.setCancelled(true);
            return;
        }
        saveAsync(clicker, target, e.getInventory(), false);
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
