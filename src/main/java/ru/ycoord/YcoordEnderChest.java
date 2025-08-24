package ru.ycoord;

import org.bukkit.configuration.ConfigurationSection;
import ru.ycoord.commands.EnderChestCommand;
import ru.ycoord.core.commands.Command;
import ru.ycoord.core.placeholder.IPlaceholderAPI;
import ru.ycoord.events.EnderChestEvent;
import ru.ycoord.placeholders.EnderChestPlaceholder;
import ru.ycoord.services.EnderChestService;
import ru.ycoord.services.ItemStorageService;

import java.util.List;
import java.util.Objects;

public final class YcoordEnderChest extends YcoordPlugin {
    private static YcoordEnderChest instance;
    private EnderChestService service;

    public static YcoordEnderChest getInstance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        super.onEnable();
    }

    @Override
    public void load(ConfigurationSection cfg, boolean reload) {
        super.load(cfg, reload);
        service = new EnderChestService(new ItemStorageService(this),
                Objects.requireNonNull(getConfig().getConfigurationSection("slots")),
                getConfig().getStringList("blacklist"));
        if (doesntRequirePlugin(this, "YcoordCore"))
            return;

        if (!reload)
            this.getServer().getPluginManager().registerEvents(new EnderChestEvent(), this);
    }

    @Override
    public List<Command> getRootCommands() {
        return List.of(new EnderChestCommand());
    }

    @Override
    public IPlaceholderAPI getPlaceholderAPI() {
        return new EnderChestPlaceholder();
    }

    public EnderChestService getService() {
        return service;
    }
}
