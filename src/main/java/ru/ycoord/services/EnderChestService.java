package ru.ycoord.services;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import ru.ycoord.YcoordCore;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.core.messages.ChatMessage;
import ru.ycoord.core.messages.MessageBase;
import ru.ycoord.core.messages.MessagePlaceholders;
import ru.ycoord.core.persistance.PlayerDataCache;
import ru.ycoord.menus.EnderChestMenu;

import java.util.concurrent.CompletableFuture;

public class EnderChestService {
    private final static String slotKey = "ec_slot_info_%d_%d";
    private final ItemStorageService itemStorageService;

    public EnderChestService(ItemStorageService itemStorageService) {
        this.itemStorageService = itemStorageService;
    }

    public ItemStorageService getItemStorageService() {
        return itemStorageService;
    }

    public CompletableFuture<Boolean> setItemAsync(OfflinePlayer initiator, OfflinePlayer player, int page, int slot, @NotNull ItemStack itemInMainHand, boolean silent) {
        return CompletableFuture.supplyAsync(() -> {
            itemStorageService.setItem(player, page, slot, itemInMainHand);


            if (!silent) {
                ChatMessage chat = YcoordCore.getInstance().getChatMessage();
                MessagePlaceholders placeholders = new MessagePlaceholders(initiator);
                placeholders.put("%player%", player.getName());
                placeholders.put("%slot%", slot);
                placeholders.put("%page%", page);
                chat.sendMessageIdAsync(MessageBase.Level.SUCCESS, initiator, "messages.item-set", placeholders);
            }
            return true;
        });
    }

    public CompletableFuture<ItemStack> getItemAsync(OfflinePlayer initiator, OfflinePlayer player, int page, int slot, boolean silent) {
        return CompletableFuture.supplyAsync(() -> {
            ChatMessage chat = YcoordCore.getInstance().getChatMessage();
            MessagePlaceholders placeholders = new MessagePlaceholders(initiator);
            placeholders.put("%player%", player.getName());
            placeholders.put("%slot%", slot);
            placeholders.put("%page%", page);

            ItemStack stack = itemStorageService.getItem(player, page, slot);
            if (stack == null) {
                if (!silent) {
                    chat.sendMessageIdAsync(MessageBase.Level.ERROR, initiator, "messages.ec-no-item", placeholders);
                }
                return null;
            }

            if (!silent)
                chat.sendMessageIdAsync(MessageBase.Level.SUCCESS, initiator, "messages.ec-item-got", placeholders);

            return stack;
        });
    }

    public enum SlotStatus {
        UNLOCKED,
        LOCKED,
        AVAILABLE_TO_UNLOCK
    }

    public SlotStatus getSlotStatus(OfflinePlayer player, int page, int slot) {
        if (true)
            return SlotStatus.UNLOCKED;
        if (slot == 10) {
            return SlotStatus.UNLOCKED;
        } else if (slot == 11) {
            return SlotStatus.AVAILABLE_TO_UNLOCK;
        }
        return SlotStatus.LOCKED;
    }

    public void unlockSlot(OfflinePlayer player, int page, int slot) {
        YcoordCore core = YcoordCore.getInstance();
        PlayerDataCache dc = core.getPlayerDataCache();
        String key = String.format(slotKey, page, slot);
        dc.add(player, key, "true");
    }

    public boolean openEnderChest(OfflinePlayer initiator, OfflinePlayer player) {
        EnderChestMenu menu = new EnderChestMenu(player, YcoordCore.getInstance().getMenus().get("EnderChest"));
        menu.open(initiator);
        return true;
    }
}
