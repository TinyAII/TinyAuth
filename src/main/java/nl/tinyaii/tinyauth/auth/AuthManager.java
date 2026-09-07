package nl.tinyaii.tinyauth.auth;

import nl.tinyaii.tinyauth.TinyAuthPlugin;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 登录管理：账号密码存储（SHA-256+盐哈希）+ 登录状态 + 原位置持久化。
 */
public class AuthManager {

    private final TinyAuthPlugin plugin;
    private final Map<UUID, String> passwords = new HashMap<>();
    private final Map<UUID, Boolean> loggedIn = new HashMap<>();
    private final Map<UUID, Long> joinTime = new HashMap<>();
    private final Map<UUID, Location> originalLocations = new HashMap<>();
    private final Map<UUID, Location> persistedLocations = new HashMap<>();
    /** 未登录隐藏的背包（登录成功后恢复） */
    private final Map<UUID, org.bukkit.inventory.ItemStack[]> savedInventories = new HashMap<>();
    private File file;

    public AuthManager(TinyAuthPlugin plugin) {
        this.plugin = plugin;
    }

    // ===== 背包隐藏 =====
    public void saveInventory(UUID uuid, org.bukkit.inventory.ItemStack[] contents) {
        savedInventories.put(uuid, contents);
    }

    public org.bukkit.inventory.ItemStack[] takeSavedInventory(UUID uuid) {
        return savedInventories.remove(uuid);
    }

    public void clearSavedInventory(UUID uuid) {
        savedInventories.remove(uuid);
    }

    public void load() {
        passwords.clear();
        persistedLocations.clear();
        file = new File(plugin.getDataFolder(), "auth.yml");
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("accounts");
        if (root != null) {
            for (String key : root.getKeys(false)) {
                try {
                    passwords.put(UUID.fromString(key), root.getString(key, ""));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        ConfigurationSection pending = yml.getConfigurationSection("pending-locations");
        if (pending != null) {
            for (String key : pending.getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(key);
                    Location loc = parseLocation(pending.getString(key, ""));
                    if (loc != null) persistedLocations.put(uuid, loc);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, String> e : passwords.entrySet()) {
            yml.set("accounts." + e.getKey(), e.getValue());
        }
        for (Map.Entry<UUID, Location> e : persistedLocations.entrySet()) {
            yml.set("pending-locations." + e.getKey(), serializeLocation(e.getValue()));
        }
        try {
            if (!plugin.getDataFolder().exists()) plugin.getDataFolder().mkdirs();
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("保存 auth.yml 失败: " + ex.getMessage());
        }
    }

    public boolean hasAccount(UUID uuid) { return passwords.containsKey(uuid); }

    public boolean register(UUID uuid, String password) {
        if (hasAccount(uuid)) return false;
        passwords.put(uuid, hash(password));
        save();
        return true;
    }

    public boolean deleteAccount(UUID uuid) {
        boolean ok = passwords.remove(uuid) != null;
        loggedIn.remove(uuid);
        if (ok) save();
        return ok;
    }

    public boolean checkPassword(UUID uuid, String password) {
        String stored = passwords.get(uuid);
        if (stored == null) return false;
        return stored.equals(hash(password));
    }

    public boolean isLoggedIn(UUID uuid) { return loggedIn.getOrDefault(uuid, false); }
    public void setLoggedIn(UUID uuid, boolean v) { loggedIn.put(uuid, v); }

    public void recordJoin(UUID uuid) { joinTime.put(uuid, System.currentTimeMillis()); }
    public void clearJoin(UUID uuid) { joinTime.remove(uuid); }

    public void recordOriginalLocation(UUID uuid, Location loc) { originalLocations.put(uuid, loc); }

    public Location getOriginalLocation(UUID uuid) {
        Location loc = originalLocations.get(uuid);
        return loc != null ? loc : persistedLocations.get(uuid);
    }

    public void persistOriginalLocation(UUID uuid) {
        Location loc = originalLocations.get(uuid);
        if (loc != null) {
            persistedLocations.put(uuid, loc);
            save();
        }
    }

    public boolean hasPersistedLocation(UUID uuid) { return persistedLocations.containsKey(uuid); }

    public void clearOriginalLocation(UUID uuid) {
        originalLocations.remove(uuid);
        persistedLocations.remove(uuid);
        save();
    }

    /** 基岩版识别：Floodgate 固定 UUID 前缀 或 API 反射 */
    public static boolean isBedrockPlayer(UUID uuid) {
        if (uuid.toString().startsWith("00000000-0000-0000-0000-")) return true;
        try {
            Class<?> clazz = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = clazz.getMethod("getInstance").invoke(null);
            return (Boolean) clazz.getMethod("isFloodgatePlayer", UUID.class).invoke(api, uuid);
        } catch (Exception e) {
            return false;
        }
    }

    private String serializeLocation(Location loc) {
        return loc.getWorld().getName() + ":" + loc.getX() + ":" + loc.getY() + ":" + loc.getZ()
                + ":" + loc.getYaw() + ":" + loc.getPitch();
    }

    private Location parseLocation(String s) {
        try {
            String[] parts = s.split(":");
            if (parts.length < 4) return null;
            org.bukkit.World w = plugin.getServer().getWorld(parts[0]);
            if (w == null) return null;
            double x = Double.parseDouble(parts[1]);
            double y = Double.parseDouble(parts[2]);
            double z = Double.parseDouble(parts[3]);
            float yaw = parts.length > 4 ? Float.parseFloat(parts[4]) : 0f;
            float pitch = parts.length > 5 ? Float.parseFloat(parts[5]) : 0f;
            return new Location(w, x, y, z, yaw, pitch);
        } catch (Exception e) {
            return null;
        }
    }

    private String hash(String password) {
        String salt = plugin.getConfig().getString("auth.salt", "TinyAII_2026");
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest((salt + password).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return "hash_error";
        }
    }
}