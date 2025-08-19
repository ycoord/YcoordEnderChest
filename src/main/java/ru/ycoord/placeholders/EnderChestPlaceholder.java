package ru.ycoord.placeholders;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.core.placeholder.IPlaceholderAPI;
import ru.ycoord.services.EnderChestService;

import java.util.List;

public class EnderChestPlaceholder extends IPlaceholderAPI {
    @Override
    public String getId() {
        return "ec";
    }

    @Override
    public String process(Player player, List<String> list) {
        if (!list.isEmpty()) {
            String mode = list.get(0);
            if (mode.equalsIgnoreCase("status")) {
                if (list.size() == 3) {
                    int page = Integer.parseInt(list.get(1));
                    int slot = Integer.parseInt(list.get(2));

                    EnderChestService.SlotStatus slotStatus = YcoordEnderChest.getInstance().getService().getSlotStatus(player, page, slot);
                    return slotStatus.toString().toLowerCase();
                }
            }
        }
        return null;
    }
}
