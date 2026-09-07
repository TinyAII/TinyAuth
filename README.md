# 登录插件 TinyAuth

独立登录插件，支持注册/登录/未登录锁定/超时踢出/基岩免登录/背包隐藏。零依赖，MIT 开源。

## 功能特性

- **注册/登录**：`/注册 <密码> <确认密码>`、`/登录 <密码>`，双语别名 `/register`、`/login`
- **未登录锁定**：未登录期间不能移动/破坏/交互/攻击/拾取/丢弃/打开背包/使用命令
- **超时踢出**：未注册玩家 30 秒未登录自动踢出（已注册玩家不踢）
- **背包隐藏**：未登录时整个背包隐藏（非清空），登录后原样恢复，退出自动保存
- **基岩免登录**：Floodgate/Geyser 基岩版玩家自动放行
- **出生点等待**：进服暂存位置 → 瞬移出生点 → 登录成功后传回原位置
- **密码安全**：SHA-256 + 盐哈希存储，不存明文

## 使用

1. 首次进服：`/注册 <密码> <确认密码>`
2. 以后进服：`/登录 <密码>`
3. 未登录时所有操作被锁定，登录后恢复正常

## 配置

```yaml
auth:
  enabled: true              # 登录模块开关
  kick-timeout-seconds: 30   # 未登录超时踢出（仅对未注册玩家生效）
  kick-message: "&c你已超时未登录，请重新进服。"
  kick-message-unregistered: "&c你尚未注册账号，请重新进服注册。（60秒内未注册）"
  bypass-bedrock: true       # 基岩版玩家免登录自动放行
  salt: "TinyAII_2026"       # 密码哈希盐（生产环境建议改）
```

## 权限

- `tinyauth.bypass` - 管理员绕过登录（默认 OP）

## 兼容性

- Java 17+
- Spigot / Paper / Purpur / Leaves
- 支持 1.16 ~ 26.2

## 协议

MIT 开源 · TinyAII

---

# TinyAuth - Login Plugin

Standalone login plugin with register/login/lock/kick/bedrock bypass/inventory hide. Zero dependencies, MIT licensed.

## Features

- **Register/Login**: `/register <password> <confirm>`, `/login <password>`, aliases `/register`, `/login`
- **Lock when not logged in**: Block move/break/interact/attack/pickup/drop/inventory/commands
- **Kick timeout**: Unregistered players kicked after 30s (registered players not kicked)
- **Inventory hide**: Full inventory hidden (not cleared) when not logged in, restored after login
- **Bedrock bypass**: Floodgate/Geyser players auto-login
- **Spawn waiting**: Save location → teleport to spawn → restore after login
- **Password security**: SHA-256 + salt hash, no plaintext storage

## Config

```yaml
auth:
  enabled: true
  kick-timeout-seconds: 30
  kick-message: "&c你已超时未登录，请重新进服。"
  kick-message-unregistered: "&c你尚未注册账号，请重新进服注册。（60秒内未注册）"
  bypass-bedrock: true
  salt: "TinyAII_2026"
```

## Permissions

- `tinyauth.bypass` - Admin bypass login (default OP)

## Compatibility

- Java 17+
- Spigot / Paper / Purpur / Leaves
- 1.16 ~ 26.2

## License

MIT · TinyAII
