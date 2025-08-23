package ru.ycoord.events;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.menus.EnderChestMenu;

public class EnderChestEvent implements Listener {
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryOpened(InventoryOpenEvent e) {
        if (e.getInventory().getType() == InventoryType.ENDER_CHEST) {
            e.setCancelled(true);
            YcoordEnderChest chest = YcoordEnderChest.getInstance();

            if (e.getPlayer() instanceof Player player) {
                chest.getService().openEnderChest(player, player);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEvent(EntityDamageByEntityEvent event) {
        if(event.isCancelled()) return;
        if (event.getEntity() instanceof Player player) {
            if (event.getDamager() instanceof Player) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                if (inv.getHolder() instanceof EnderChestMenu) {
                    player.closeInventory();
                }
            }
        }
    }
}
