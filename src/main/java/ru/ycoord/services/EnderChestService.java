package ru.ycoord.services;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.ycoord.YcoordCore;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.core.balance.IBalance;
import ru.ycoord.core.messages.ChatMessage;
import ru.ycoord.core.messages.MessageBase;
import ru.ycoord.core.messages.MessagePlaceholders;
import ru.ycoord.core.persistance.PlayerDataCache;
import ru.ycoord.menus.EnderChestMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class EnderChestService {
    private final static String permission = "yc.ec.slot.%d.%d";
    private final ItemStorageService itemStorageService;
    private final ConfigurationSection prices;
    private final ConfigurationSection defaultSlots;
    private final List<String> blacklist;

    public EnderChestService(ItemStorageService itemStorageService, ConfigurationSection slots, List<String> blacklist) {
        this.itemStorageService = itemStorageService;
        this.prices = slots.getConfigurationSection("prices");
        this.defaultSlots = slots.getConfigurationSection("default-opened");
        this.blacklist = blacklist;
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

    private @Nullable ConfigurationSection getSlotConfig(int page, int slot) {
        ConfigurationSection pageSection = prices.getConfigurationSection(String.valueOf(page + 1));
        if (pageSection == null) {
            return null;
        }

        return pageSection.getConfigurationSection(String.valueOf(slot));
    }

    public boolean canUse(Player player){
        return player.hasPermission("yc.ec.use");
    }

    public boolean canOpen(Player player, OfflinePlayer owner){
        if(player.getName().equals(owner.getName())){
            return player.hasPermission("yc.ec.open");
        }

        return player.hasPermission("yc.ec.open") && checkPlayerNotInBlacklist(player, owner);
    }

    public boolean canBuySlots(Player clicker, OfflinePlayer owner){
        if(!owner.equals(clicker) && !clicker.hasPermission("yc.ec.buy.other")) {
            ChatMessage message = YcoordCore.getInstance().getChatMessage();
            message.sendMessageIdAsync(MessageBase.Level.ERROR, clicker, "messages.ec-no-permission-use-other");
            return false;
        }
        return true;
    }

    public boolean checkPlayerNotInBlacklist(Player receiver,  OfflinePlayer player){
        if(receiver.hasPermission("yc.ec.blacklist.bypass"))
            return true;
        if(blacklist.contains(player.getName())) {
            ChatMessage message = YcoordCore.getInstance().getChatMessage();
            message.sendMessageIdAsync(MessageBase.Level.ERROR, receiver, "messages.ec-blacklist");
            return false;
        }

        return true;
    }

    public boolean canProcessClick(OfflinePlayer clicker, OfflinePlayer chestOwner){
        if (!clicker.equals(chestOwner)) {
            if (clicker instanceof Player player) {
                if (!player.hasPermission("yc.ec.use.other")) {
                    ChatMessage message = YcoordCore.getInstance().getChatMessage();
                    message.sendMessageIdAsync(MessageBase.Level.ERROR, clicker, "messages.ec-no-permission-use-other");
                    return false;
                }

                return checkPlayerNotInBlacklist(player, chestOwner);
            }
            return false;
        }
        return true;
    }

    public @Nullable Integer getPrice(OfflinePlayer player, int page, int slot) {
        ConfigurationSection pageSection = getSlotConfig(page, slot);
        if (pageSection == null) {
            return null;
        }

        if (!pageSection.contains("price")) {
            return null;
        }

        return pageSection.getInt("price");
    }

    private boolean slotUnlocked(OfflinePlayer player, int page, int slot) {
        ConfigurationSection pageSection = getSlotConfig(page, slot);
        if (pageSection == null) {
            return false;
        }

        if (!pageSection.contains("require-page")) {
            return false;
        }

        if (!pageSection.contains("require-slot")) {
            return false;
        }

        int requirePage = pageSection.getInt("require-page") - 1;
        int requireSlot = pageSection.getInt("require-slot");

        return slotOpened(player, requirePage, requireSlot);
    }

    private boolean slotOpened(OfflinePlayer player, int page, int slot) {

        String perm = String.format(permission, page, slot);
        Player online = player.getPlayer();
        if (online != null) {
            if (online.hasPermission(perm))
                return true;
        }

        List<Integer> c = defaultSlots.getIntegerList(String.valueOf(page + 1));
        if (c.contains(slot)) {
            return true;
        }

        PlayerDataCache pdc = YcoordCore.getInstance().getPlayerDataCache();

        return pdc.has(player, perm);
    }

    public SlotStatus getSlotStatus(OfflinePlayer player, int page, int slot) {
        if (slotOpened(player, page, slot)) {
            return SlotStatus.UNLOCKED;
        }

        if (slotUnlocked(player, page, slot)) {
            return SlotStatus.AVAILABLE_TO_UNLOCK;
        }

        return SlotStatus.LOCKED;
    }


    public boolean unlockSlot(Player payer, OfflinePlayer offlinePlayer, int page, int slot) {
        Integer price = getPrice(offlinePlayer, page, slot);
        if (price == null) {
            return false;
        }
        if(offlinePlayer instanceof Player player) {
            ChatMessage cm = YcoordCore.getInstance().getChatMessage();
            IBalance balance = YcoordEnderChest.getInstance().getCurrenyBalance();
            double currentBalance = balance.get(payer);
            MessagePlaceholders placeholders = new MessagePlaceholders(player);
            placeholders.put("%page%", page);
            placeholders.put("%slot%", slot);
            placeholders.put("%price%", price);
            placeholders.put("%balance%", price);
            placeholders.put("%diff%", Math.abs(price - currentBalance));

            if(currentBalance < price) {
                cm.sendMessageIdAsync(MessageBase.Level.ERROR, payer, "messages.ec-no-money", placeholders);
                return false;
            }

            balance.withdraw(payer, price);

            PlayerDataCache pdc = YcoordCore.getInstance().getPlayerDataCache();
            String perm = String.format(permission, page, slot);
            pdc.add(player, perm, "true");

            cm.sendMessageIdAsync(MessageBase.Level.SUCCESS, payer, "messages.ec-slot-unlocked", placeholders);
            return true;
        }
        return false;
    }

    public boolean openEnderChest(OfflinePlayer initiator, OfflinePlayer player) {
        EnderChestMenu menu = new EnderChestMenu(player, YcoordCore.getInstance().getMenus().get("EnderChest"));
        menu.open(initiator);
        return true;
    }
}
