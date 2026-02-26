package ua.vdev.clanhomeaddon;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class HomeConfig {

    private FileConfiguration config;

    public HomeConfig(FileConfiguration config) {
        this.config = config;
    }

    public void reload(FileConfiguration newConfig) {
        this.config = newConfig;
    }

    public int getTeleportDelay() {
        return Math.max(0, config.getInt("teleport-delay", 3));
    }

    public boolean isMoveCancelsTP() {
        return config.getBoolean("move-cancels-teleport", true);
    }

    public boolean isOwnerOnly() {
        return config.getBoolean("owner-only", false);
    }

    public int getHomeLimit(int level) {
        ConfigurationSection sec = config.getConfigurationSection("home-limits");
        if (sec == null) return config.getInt("default-home-limit", 1);

        TreeMap<Integer, Integer> limits = new TreeMap<>(Collections.reverseOrder());
        for (String key : sec.getKeys(false)) {
            try {
                limits.put(Integer.parseInt(key), sec.getInt(key));
            } catch (NumberFormatException ignored) {}
        }

        for (Map.Entry<Integer, Integer> entry : limits.entrySet()) {
            if (level >= entry.getKey()) return entry.getValue();
        }

        return config.getInt("default-home-limit", 1);
    }

    public String getMessage(String key) {
        String val = config.getString("messages." + key);
        return val != null ? val : "<red>[ClanHome] Сообщение не найдено: <gold>" + key;
    }

    public List<String> getMessageList(String key) {
        List<String> list = config.getStringList("messages." + key);
        if (list.isEmpty()) {
            return List.of("<red>[ClanHome] Список сообщений не найден: <gold>" + key);
        }
        return list;
    }
}