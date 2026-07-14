# Chain Mining / 连锁挖矿

[![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-brightgreen)](https://www.minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-1.12.2-orange)](https://files.minecraftforge.net/)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

[English](#english) | [中文](#中文)

---

## 中文

一个 Minecraft 1.12.2 Forge 模组，让你只需按住一个键即可一次性挖掘整片相连的同种方块，并提供实时线框预览。

### 功能特性

- **连锁挖掘矿脉** — 挖一个方块，所有相连的匹配方块一同被采集
- **实时线框预览** — 挖掘前即可看到哪些方块会被连锁
- **3 种连锁形态**，按住热键时同时按住shift用鼠标滚轮切换：
  - **无定形** — 向所有方向扩散搜索相连的匹配方块
  - **单向** — 沿你面向的方块延伸线性连锁
  - **3x3 隧道** — 沿朝向进行 3x3 截面隧道挖掘
- **4 种匹配模式**，按住热键时同时按住shift + ctrl用滚轮切换：
  - **Meta 匹配** — 按方块注册名 + metadata 匹配
  - **NBT 匹配** — 按方块注册名 + 白名单 NBT 数据匹配
  - **NBT+Meta** — 按方块注册名 + metadata + NBT 匹配
  - **仅注册名** — 仅按方块注册名匹配
- **可调搜索范围** — 按住热键同时按住shift + Alt用滚轮调整相邻搜索范围（1-5）
- **掉落物聚合** — 所有掉落物和经验球在原始方块位置生成，减少实体卡顿
- **工具与方块黑名单** — 支持通配符，精确控制连锁行为
- **HUD 信息面板** — 显示当前模式、形态、范围及预览统计
- **彩虹预览模式** — 线框颜色循环变化
- **中英双语** — 完整的中文和英文语言支持
- **ConfigAnytime 支持** — 所有设置均可在游戏内实时修改，无需重启

### 使用方法

1. 按住 `~` 键
2. 看向目标方块，线框预览会显示将被连锁的方块
3. 挖掘方块，所有高亮的方块将被一并采集
4. 松开按键恢复普通挖掘

### 前置要求

- **[ConfigAnytime]版本** 越新越好

### 配置

所有设置项位于 `config/chainmining.cfg`，也可在游戏中通过 Mod Options 修改。

#### 客户端设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `chainMiningShape` | `shapeless` | 连锁形态：`shapeless` / `plane` / `tunnel` |
| `chainMiningMatchMode` | `meta` | 匹配模式：`meta` / `nbt` / `registry` / `nbt_meta` |
| `chainMiningHudPosition` | `top_left` | HUD 面板位置：`top_left` / `top_right` |
| `chainMiningPreviewColor` | `FFE65CEB` | 预览线框颜色（十六进制 ARGB）或 `rainbow` |
| `chainMiningPreviewRenderLimit` | `128` | 预览渲染最大方块数（1-1024） |
| `chainMiningNeighborRange` | `1` | 相邻搜索范围（1-5） |
| `chainMiningEnablePreview` | `true` | 是否启用预览 |

#### 服务端设置

| 配置项 | 默认值 | 说明 |
|---|---|---|
| `chainMiningMaxBlocks` | `128` | 单次连锁最大方块数（1-1024） |
| `chainMiningExhaustionPerBlock` | `0.1` | 每个连锁方块的饥饿值消耗（0.0-20.0） |
| `chainMiningMinFoodLevel` | `0.1` | 触发连锁的最低饱食度（0.0-20.0） |
| `chainMiningIgnoreHeldItem` | `false` | 是否无视手持物品限制 |
| `chainMiningRefreshInterval` | `1` | 服务端预览刷新间隔（tick，0=关闭） |
| `chainMiningToolBlackList` | `{}` | 工具黑名单（支持通配符） |
| `chainMiningBlockBlackList` | `{}` | 方块黑名单（支持通配符） |
| `chainMiningNbtMatchKeys` | `[id, tier, type]` | NBT 匹配时比较的键名列表 |

#### 黑名单示例

```
chainMiningToolBlackList <
    minecraft:diamond_pickaxe
    minecraft:stone:*
>
chainMiningBlockBlackList <
    minecraft:obsidian
    minecraft:chest
>
```

### 从源码构建

```bash
./gradlew build
```

编译产物位于 `build/libs/`。

### 鸣谢

- **StarFall063** — 开发者
- 基于 [RetroFuturaGradle](https://github.com/CleanroomMC/RetroFuturaGradle) 构建

### 开源协议

本项目基于 MIT 协议开源。

---

## English

A Minecraft 1.12.2 Forge mod that lets you mine entire veins of matching blocks in a single operation. Hold a key, look at a block, and break it — all connected matching blocks are mined at once, with a live wireframe preview showing exactly what will be affected.

### Features

- **Chain mine entire ore veins** — break one block to harvest all connected matching blocks
- **Live wireframe preview** — see exactly which blocks will be mined before you swing
- **3 chain shapes**, cycle with Shift + mouse wheel while holding the key:
  - **Shapeless** — flood-fill through all connected matching blocks in every direction
  - **Directional** — linear chain extending from the face you're looking at
  - **3x3 Tunnel** — tunnel mining with a 3x3 cross-section
- **4 match modes**, cycle with Shift + Ctrl + mouse wheel while holding the key:
  - **Meta Only** — match by block type + metadata
  - **NBT Only** — match by block type + whitelisted NBT data
  - **NBT + Meta** — match by block type + metadata + NBT
  - **Registry Only** — match by block type only
- **Adjustable range** — Shift + Alt + mouse wheel while holding the key (1-5)
- **Aggregated drops** — all drops and XP spawn at the origin block to reduce entity lag
- **Tool & block blacklists** — wildcard support for fine-grained control
- **HUD overlay** — shows current mode, shape, range, and preview stats
- **Rainbow preview mode** — cycling color wireframe
- **Bilingual** — English and Chinese (zh_cn) localization

### How to Use

1. Hold the grave/tilde key (`~`)
2. Look at a block — the wireframe preview shows what will be mined
3. Break the block — all highlighted blocks are harvested
4. Release the key to return to normal mining

### Requirements

- **[ConfigAnytime](https://www.curseforge.com/minecraft/mc-mods/configanytime)** (the newer the better)

### Configuration

All settings are in `config/chainmining.cfg` and can be changed in-game via Mod Options.

#### Client Settings

| Setting | Default | Description |
|---|---|---|
| `chainMiningShape` | `shapeless` | Chain shape: `shapeless`, `plane`, `tunnel` |
| `chainMiningMatchMode` | `meta` | Match mode: `meta`, `nbt`, `registry`, `nbt_meta` |
| `chainMiningHudPosition` | `top_left` | HUD overlay position: `top_left`, `top_right` |
| `chainMiningPreviewColor` | `FFE65CEB` | Wireframe color (hex ARGB) or `rainbow` |
| `chainMiningPreviewRenderLimit` | `128` | Max blocks rendered in preview (1-1024) |
| `chainMiningNeighborRange` | `1` | Neighbor search range (1-5) |
| `chainMiningEnablePreview` | `true` | Toggle wireframe preview |

#### Server Settings

| Setting | Default | Description |
|---|---|---|
| `chainMiningMaxBlocks` | `128` | Max blocks per chain operation (1-1024) |
| `chainMiningExhaustionPerBlock` | `0.1` | Hunger per block (0.0-20.0) |
| `chainMiningMinFoodLevel` | `0.1` | Min food level to activate (0.0-20.0) |
| `chainMiningIgnoreHeldItem` | `false` | Ignore held item requirement |
| `chainMiningRefreshInterval` | `1` | Server preview refresh in ticks (0=off) |
| `chainMiningToolBlackList` | `{}` | Blacklisted tools (wildcards supported) |
| `chainMiningBlockBlackList` | `{}` | Blacklisted blocks (wildcards supported) |
| `chainMiningNbtMatchKeys` | `[id, tier, type]` | NBT keys to compare for NBT matching |

#### Blacklist Examples

```
chainMiningToolBlackList <
    minecraft:diamond_pickaxe
    minecraft:stone:*
>
chainMiningBlockBlackList <
    minecraft:obsidian
    minecraft:chest
>
```

### Credits

- **StarFall063** — developer
- Built with [RetroFuturaGradle](https://github.com/CleanroomMC/RetroFuturaGradle)

### License

This project is licensed under the MIT License.
