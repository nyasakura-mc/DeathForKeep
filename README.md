# DeathForKeep (死亡物品保护)

DeathForKeep是一个Minecraft Bukkit/Spigot服务器插件，提供死亡物品保护功能，让玩家可以通过购买保护来防止死亡时物品和经验的丢失。

## 主要功能

- **死亡物品保护**: 玩家死亡时保留物品和经验
- **经济系统集成**: 通过Vault支持各种经济插件，玩家可以付费购买保护时间
- **时间管理**: 保护时间可按小时、天或自定义时间购买
- **可视化界面**: 提供GUI界面进行各种操作
- **保护共享**: 玩家可以将自己的保护共享给其他玩家
- **粒子效果**: 可自定义的粒子效果显示保护状态
- **BossBar提醒**: 在保护即将到期时通过BossBar提醒玩家
- **数据库支持**: 支持SQLite和MySQL数据库
- **多语言支持**: 内置中文和英文，并可自定义语言文件
- **更新检查**: 自动检查插件更新

## 命令

- `/deathkeep buy [天数]` - 购买死亡保护
- `/deathkeep check` - 查看自己的保护状态
- `/deathkeep check <玩家>` - 查看其他玩家的保护状态
- `/deathkeep gui` - 打开图形界面
- `/deathkeep particles` - 切换粒子效果
- `/deathkeep share <玩家>` - 与其他玩家共享保护
- `/deathkeep add <玩家> <天数>` - (管理员) 添加保护时间
- `/deathkeep remove <玩家>` - (管理员) 移除玩家的保护
- `/deathkeep reload` - (管理员) 重新加载插件配置
- `/deathkeep find <玩家>` - (管理员) 查找玩家的保护详情
- `/deathkeep resetall` - (管理员) 重置所有玩家数据

## 权限

- `deathkeep.buy` - 允许购买保护 (默认: 所有玩家)
- `deathkeep.check` - 允许查看自己的保护状态 (默认: 所有玩家)
- `deathkeep.check.others` - 允许查看其他玩家的保护状态 (默认: OP)
- `deathkeep.particles` - 允许切换粒子效果 (默认: 所有玩家)
- `deathkeep.share` - 允许共享保护 (默认: 所有玩家)
- `deathkeep.admin` - 管理员权限 (默认: OP)

## PlaceholderAPI 变量

本插件支持PlaceholderAPI，提供以下变量：

- `%deathkeep_time%` - 显示剩余保护时间，格式化为易读形式
- `%deathkeep_status%` - 显示保护状态（激活/过期）
- `%deathkeep_particles%` - 显示粒子效果状态（启用/禁用）
- `%deathkeep_share_status%` - 显示保护共享状态

使用示例：
- 显示在计分板: `剩余保护: %deathkeep_time%`
- 在聊天格式中: `[%deathkeep_status%] 玩家名`
- 在Hologram显示: `保护状态: %deathkeep_status%`

## 依赖

- **必需**: Vault
- **可选**: PlaceholderAPI

## 配置

详细配置选项请参考插件生成的`config.yml`文件，支持自定义：

- 保护价格
- 粒子效果
- BossBar设置
- 共享功能
- 数据库连接
- 语言设置
- 以及更多...

## 支持

如有问题或建议，请提交GitHub Issue或加入我们的Discord社区。 