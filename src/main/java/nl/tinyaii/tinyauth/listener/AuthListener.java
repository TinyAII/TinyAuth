package nl.tinyaii.tinyauth.listener;

import nl.tinyaii.tinyauth.TinyAuthPlugin;
import nl.tinyaii.tinyauth.auth.AuthManager;
import nl.tinyaii.tinyauth.util.Messages;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

/**
 * 登录监听：未登录玩家锁定（移动/交互/破坏/攻击/受伤/拾取/丢弃/命令），超时踢出。
 * 出生点等待：进服记原位置 → 瞬移出生点 → 登录后传回。
 */
public class AuthListener implements Listener {

    private final TinyAuthPlugin plugin;
    private final AuthManager am;

    public AuthListener(TinyAuthPlugin plugin) {
        this.plugin = plugin;
        this.am = plugin.getAuthManager();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (!plugin.getConfig().getBoolean("auth.enabled", true)) return;
        // 基岩版免登录
        if (plugin.getConfig().getBoolean("auth.bypass-bedrock", true)
                && AuthManager.isBedrockPlayer(p.getUniqueId())) {
            am.setLoggedIn(p.getUniqueId(), true);
            p.sendMessage(Messages.color(plugin.getConfig().getString("messages.bedrock", "&e基岩版玩家免登录，欢迎回来！")));
            return;
        }
        am.recordJoin(p.getUniqueId());
        // 延迟 1 tick 记录真实退出位置，随后传送登录等待区
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            if (!am.hasPersistedLocation(p.getUniqueId())) {
                am.recordOriginalLocation(p.getUniqueId(), p.getLocation());
            }
            p.teleport(plugin.getSpawnLocation());
        }, 1L);
        if (am.hasAccount(p.getUniqueId())) {
            p.sendMessage(Messages.color(plugin.getConfig().getString("messages.login-prompt", "&e请登录：&a/登录 <密码>")));
        } else {
            p.sendMessage(Messages.color(plugin.getConfig().getString("messages.register-prompt", "&e首次进服请注册：&a/注册 <密码> <确认密码>")));
        }
        // 未登录背包隐藏：保存当前背包内容，清空显示（登录后恢复）
        if (!am.isLoggedIn(p.getUniqueId())) {
            org.bukkit.inventory.ItemStack[] contents = p.getInventory().getContents();
            am.saveInventory(p.getUniqueId(), contents);
            p.getInventory().clear();
        }
        // 超时踢出：仅对未注册玩家生效，已注册玩家不踢
        int timeout = plugin.getConfig().getInt("auth.kick-timeout-seconds", 30);
        if (timeout > 0 && !am.hasAccount(p.getUniqueId())) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && !am.isLoggedIn(p.getUniqueId())) {
                    p.kickPlayer(plugin.getConfig().getString("auth.kick-message-unregistered", "你尚未注册账号，请重新进服注册"));
                }
            }, timeout * 20L);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        UUID u = e.getPlayer().getUniqueId();
        Player p = e.getPlayer();
        // 未登录退服：恢复背包内容再保存（防止丢物品）
        if (!am.isLoggedIn(u)) {
            org.bukkit.inventory.ItemStack[] saved = am.takeSavedInventory(u);
            if (saved != null) {
                p.getInventory().setContents(saved);
            }
            am.persistOriginalLocation(u);
        } else {
            am.clearOriginalLocation(u);
        }
        am.clearJoin(u);
        am.setLoggedIn(u, false);
    }

    private boolean locked(Player p) {
        if (!plugin.getConfig().getBoolean("auth.enabled", true)) return false;
        if (plugin.getConfig().getBoolean("auth.bypass-bedrock", true)
                && AuthManager.isBedrockPlayer(p.getUniqueId())) return false;
        return !am.isLoggedIn(p.getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        if (!locked(e.getPlayer())) return;
        if (e.getFrom().getBlockX() != e.getTo().getBlockX()
                || e.getFrom().getBlockY() != e.getTo().getBlockY()
                || e.getFrom().getBlockZ() != e.getTo().getBlockZ()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player && locked((Player) e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player && locked((Player) e.getDamager())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player && locked((Player) e.getEntity())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (locked(e.getPlayer())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player && locked((Player) e.getWhoClicked())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        if (!locked(e.getPlayer())) return;
        String cmd = e.getMessage().toLowerCase();
        if (cmd.startsWith("/登录") || cmd.startsWith("/login")
                || cmd.startsWith("/注册") || cmd.startsWith("/register")) return;
        e.setCancelled(true);
        e.getPlayer().sendMessage(Messages.color(plugin.getConfig().getString("messages.must-login", "&c请先登录：/登录 <密码>")));
    }
}