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

public class EnderChestCommand extends AdminCommand {
    @Override
    public String getName() {
        return "ec";
    }


    static class PageRequirement extends RangedIntegerRequirement {

        public PageRequirement(Command command) {
            super(command, 0, 10);
        }

        @Override
        public void sendDescription(CommandSender sender) {
            if (sender instanceof Player player) {
                YcoordCore.getInstance().getChatMessage().sendMessageIdAsync(MessageBase.Level.NONE, player, "messages.page-description");
            }

        }
    }

    static class SlotRequirement extends RangedIntegerRequirement {

        public SlotRequirement(Command command) {
            super(command, 0, 53);
        }

        @Override
        public void sendDescription(CommandSender sender) {
            if (sender instanceof Player player) {
                YcoordCore.getInstance().getChatMessage().sendMessageIdAsync(MessageBase.Level.NONE, player, "messages.slot-description");
            }

        }
    }

    public static class GetCommand extends AdminCommand {


        @Override
        public List<Requirement> getRequirements(CommandSender sender) {
            return List.of(
                    new OnlinePlayerRequirement(this),
                    new PageRequirement(this),
                    new SlotRequirement(this)
            );
        }

        @Override
        public String getName() {
            return "get";
        }

        @Override
        public String getDescription(CommandSender commandSender) {
            return YcoordCore.getInstance().getChatMessage().makeMessageId(MessageBase.Level.NONE, "messages.get-command-description", new MessagePlaceholders(null));
        }

        @Override
        public boolean execute(CommandSender sender, List<String> args, List<Object> params) {
            if (!super.execute(sender, args, params))
                return false;

            if (sender instanceof Player player) {
                EnderChestService service = YcoordEnderChest.getInstance().getService();
                service.getItemAsync(player, getParam(), getParam(), getParam(), false).thenAccept((itemInSlot) -> {
                    if (itemInSlot == null) {
                        return;
                    }
                    player.getInventory().addItem(itemInSlot);
                });
            }

            return true;
        }
    }

    public static class SetCommand extends AdminCommand {

        @Override
        public List<Requirement> getRequirements(CommandSender sender) {
            return List.of(
                    new OnlinePlayerRequirement(this),
                    new PageRequirement(this),
                    new SlotRequirement(this)
            );
        }

        @Override
        public boolean execute(CommandSender sender, List<String> args, List<Object> params) {
            if (!super.execute(sender, args, params))
                return false;

            if (sender instanceof Player player) {
                EnderChestService service = YcoordEnderChest.getInstance().getService();

                service.setItemAsync(player, getParam(), getParam(), getParam(), player.getInventory().getItemInMainHand(), false).thenAccept((ok) -> {

                });
            }


            return true;
        }

        @Override
        public String getName() {
            return "set";
        }

        @Override
        public String getDescription(CommandSender commandSender) {
            return YcoordCore.getInstance().getChatMessage().makeMessageId(MessageBase.Level.NONE, "messages.set-command-description", new MessagePlaceholders(null));
        }
    }

    public static class OpenCommand extends AdminCommand {

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
                new GetCommand(),
                new SetCommand(),
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
