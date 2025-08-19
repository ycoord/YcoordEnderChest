package ru.ycoord.services;// ItemStorageService.java
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class ItemStorageService {

    private final Plugin plugin;
    private final File playersDir;

    // Корневой путь в YAML (можно поменять на свой)
    private static final String ROOT = "items";

    public ItemStorageService(Plugin plugin) {
        this.plugin = plugin;
        this.playersDir = new File(plugin.getDataFolder(), "players");
        if (!playersDir.exists() && !playersDir.mkdirs()) {
            plugin.getLogger().warning("Не удалось создать папку: " + playersDir.getAbsolutePath());
        }
    }

    /**
     * Сохраняет предмет по (page, slot) в файл игрока.
     */
    public void setItem(OfflinePlayer player, int page, int slot, ItemStack stack) {
        validateIndices(page, slot);
        File file = getPlayerFile(player);
        YamlConfiguration cfg = load(file);

        String path = path(page, slot);
        cfg.set(path, stack); // ItemStack сериализуется автоматически

        save(file, cfg);
    }

    /**
     * Возвращает предмет по (page, slot) из файла игрока. Может вернуть null, если ничего не сохранено.
     */
    public ItemStack getItem(OfflinePlayer player, int page, int slot) {
        validateIndices(page, slot);
        File file = getPlayerFile(player);
        if (!file.exists()) return null;

        YamlConfiguration cfg = load(file);
        String path = path(page, slot);
        return cfg.getItemStack(path);
    }

    /* ================== ВСПОМОГАТЕЛЬНОЕ ================== */

    private void validateIndices(int page, int slot) {
        if (page < 0) throw new IllegalArgumentException("page must be >= 0");
        if (slot < 0) throw new IllegalArgumentException("slot must be >= 0");
    }

    private String path(int page, int slot) {
        // items.<page>.<slot>
        return ROOT + "." + page + "." + slot;
    }

    private File getPlayerFile(OfflinePlayer player) {
        // требуется имя файла = имя игрока
        String rawName = Objects.toString(player.getName(), player.getUniqueId().toString());
        String safe = sanitizeFileName(rawName);
        return new File(playersDir, safe + ".yml");
    }

    private String sanitizeFileName(String name) {
        // убрать недопустимые символы для файловой системы
        return name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
    }

    private YamlConfiguration load(File file) {
        YamlConfiguration cfg = new YamlConfiguration();
        if (file.exists()) {
            try {
                cfg.load(file);
            } catch (IOException | InvalidConfigurationException e) {
                plugin.getLogger().warning("Ошибка загрузки " + file.getName() + ": " + e.getMessage());
            }
        }
        return cfg;
    }

    private void save(File file, YamlConfiguration cfg) {
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("Ошибка сохранения " + file.getName() + ": " + e.getMessage());
        }
    }
}
