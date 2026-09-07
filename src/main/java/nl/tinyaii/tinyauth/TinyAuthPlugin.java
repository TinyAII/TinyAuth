package nl.tinyaii.tinyauth;

import nl.tinyaii.tinyauth.auth.AuthManager;
import nl.tinyaii.tinyauth.command.AuthCommand;
import nl.tinyaii.tinyauth.listener.AuthListener;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

public class TinyAuthPlugin extends JavaPlugin {

    private AuthManager authManager;

    @Override
    public void onEnable() {
        // TinyAII 品牌横幅 —— 必须在所有初始化逻辑之前输出
        getLogger().info(" _____ _                _    ___ ___");
        getLogger().info("|_   _(_)_ __  _   _   / \\  |_ _|_ _|");
        getLogger().info("  | | | | '_ \\| | | | / _ \\  | | | |");
        getLogger().info("  | | | | | | | |_| |/ ___ \\ | | | |");
        getLogger().info("  |_| |_|_| |_|\\__, /_/   \\_\\___|___|");
        getLogger().info("               |___/");
        getLogger().info("TinyAuth 登录插件 v" + getDescription().getVersion() + " - TinyAII 出品");

        saveDefaultConfig();
        authManager = new AuthManager(this);
        authManager.load();

        getServer().getPluginManager().registerEvents(new AuthListener(this), this);
        getCommand("注册").setExecutor(new AuthCommand(this));
        getCommand("登录").setExecutor(new AuthCommand(this));

        getLogger().info("登录插件已启用。超时=" + getConfig().getInt("auth.kick-timeout-seconds", 30)
                + " 基岩免登录=" + getConfig().getBoolean("auth.bypass-bedrock", true));
    }

    @Override
    public void onDisable() {
        if (authManager != null) authManager.save();
    }

    public AuthManager getAuthManager() { return authManager; }

    /** 登录等待区坐标（世界出生点） */
    public Location getSpawnLocation() {
        World w = Bukkit.getWorlds().get(0);
        return w.getSpawnLocation();
    }
}