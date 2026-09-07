package nl.tinyaii.tinyauth.command;

import nl.tinyaii.tinyauth.TinyAuthPlugin;
import nl.tinyaii.tinyauth.auth.AuthManager;
import nl.tinyaii.tinyauth.util.Messages;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * 登录/注册命令（双语：/登录 /login /注册 /register）。
 * 登录成功后传回进服原位置。
 */
public class AuthCommand implements CommandExecutor {

    private final TinyAuthPlugin plugin;

    public AuthCommand(TinyAuthPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player)) { sender.sendMessage("仅玩家可用。"); return true; }
        Player p = (Player) sender;
        AuthManager am = plugin.getAuthManager();
        // 基岩版免登录
        if (plugin.getConfig().getBoolean("auth.bypass-bedrock", true)
                && AuthManager.isBedrockPlayer(p.getUniqueId())) {
            p.sendMessage(Messages.color(plugin.getConfig().getString("messages.bedrock", "&e基岩版玩家免登录，无需注册/登录。")));
            return true;
        }
        String name = cmd.getName();

        if (name.equals("注册") || name.equalsIgnoreCase("register")) {
            if (args.length < 2) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.register-usage", "&c用法: /注册 <密码> <确认密码>"))); return true; }
            if (!args[0].equals(args[1])) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.register-mismatch", "&c两次密码不一致。"))); return true; }
            if (args[0].length() < 4) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.register-short", "&c密码至少 4 位。"))); return true; }
            if (!am.register(p.getUniqueId(), args[0])) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.register-exists", "&c该账号已注册，请直接登录。"))); return true; }
            am.setLoggedIn(p.getUniqueId(), true);
            restoreInventory(p);
            teleportBack(p);
            p.sendMessage(Messages.color(plugin.getConfig().getString("messages.register-ok", "&a注册成功！已自动登录。")));
            return true;
        }

        if (name.equals("登录") || name.equalsIgnoreCase("login")) {
            if (args.length < 1) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.login-usage", "&c用法: /登录 <密码>"))); return true; }
            if (!am.hasAccount(p.getUniqueId())) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.login-not-registered", "&c该账号未注册，请用 /注册 <密码> <确认密码> 注册。"))); return true; }
            if (am.isLoggedIn(p.getUniqueId())) { p.sendMessage(Messages.color(plugin.getConfig().getString("messages.login-already", "&a你已登录。"))); return true; }
            if (am.checkPassword(p.getUniqueId(), args[0])) {
                am.setLoggedIn(p.getUniqueId(), true);
                restoreInventory(p);
                teleportBack(p);
                p.sendMessage(Messages.color(plugin.getConfig().getString("messages.login-ok", "&a登录成功！欢迎回来。")));
            } else {
                p.sendMessage(Messages.color(plugin.getConfig().getString("messages.login-wrong", "&c密码错误！")));
            }
            return true;
        }
        return true;
    }

    /** 登录/注册成功：恢复进服时隐藏的背包 */
    private void restoreInventory(Player p) {
        org.bukkit.inventory.ItemStack[] saved = plugin.getAuthManager().takeSavedInventory(p.getUniqueId());
        if (saved != null) {
            p.getInventory().setContents(saved);
        }
    }

    /** 登录/注册成功：传回进服原位置 */
    private void teleportBack(Player p) {
        Location loc = plugin.getAuthManager().getOriginalLocation(p.getUniqueId());
        if (loc != null && loc.getWorld() != null) {
            p.teleport(loc);
        }
        plugin.getAuthManager().clearOriginalLocation(p.getUniqueId());
    }
}