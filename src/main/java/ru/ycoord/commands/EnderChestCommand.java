package ru.ycoord.commands;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.ycoord.YcoordCore;
import ru.ycoord.YcoordEnderChest;
import ru.ycoord.core.commands.AdminCommand;
import ru.ycoord.core.commands.Command;
import ru.ycoord.core.commands.requirements.*;
import ru.ycoord.core.messages.MessageBase;
import ru.ycoord.core.messages.MessagePlaceholders;
import ru.ycoord.menus.EnderChestMenu;
import ru.ycoord.services.EnderChestService;

import java.util.List;

public class EnderChestCommand extends Command {
    @Override
    public String getName() {
        return "ec";
    }

    @Override
    public boolean canExecute(CommandSender sender) {
        return YcoordEnderChest.getInstance().getService().canUse((Player) sender);
    }


    public static class OpenCommand extends Command {
        @Override
        public boolean canExecute(CommandSender sender) {
            return YcoordEnderChest.getInstance().getService().canOpen((Player) sender, (Player) sender);
        }

        @Override
        public List<Requirement> getRequirements(CommandSender sender) {
            return List.of(new PlayerRequirement(this));
        }

        @Override
        public String getName() {
            return "open";
        }

        @Override
        public boolean execute(CommandSender sender, List<String> args, List<Object> params) {
            if (!super.execute(sender, args, params))
                return false;


            if (sender instanceof Player me) {
                OfflinePlayer player = getParam();

                if(!YcoordEnderChest.getInstance().getService().canOpen(me,player))
                    return false;

                EnderChestService service = YcoordEnderChest.getInstance().getService();
                service.openEnderChest(me, player);
            }


            return true;
        }

        @Override
        public String getDescription(CommandSender commandSender) {
            return "открывает эндер-сундук указанного игрока";
        }
    }

    @Override
    public List<Requirement> getRequirements(CommandSender sender) {
        return List.of(new OptionalRequirement(this, List.of(
                new OpenCommand()
        )));
    }

    @Override
    public String getDescription(CommandSender commandSender) {
        return "получить";
    }

    @Override
    public boolean execute(CommandSender sender, List<String> args, List<Object> params) {
        if (!super.execute(sender, args, params))
            return false;
        EnderChestService service = YcoordEnderChest.getInstance().getService();
        if (sender instanceof Player player)
            service.openEnderChest(player, player);
        return true;
    }
}
