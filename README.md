# Spell Wheel (法术轮盘)

为 **Spell Engine** 添加一个**法术轮盘**功能：循环切换当前法术、一键施法、以及一个位置可配置的 HUD。

- **Minecraft 1.20.1 · Fabric**
- 作者：**AnsEX**
- 仓库：<https://github.com/AnsEx1/Spell_Wheel>
- 开发历程：[DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md)
- 许可证：[LICENSE.txt](LICENSE.txt)

## 功能

- **循环切换**当前选中法术（可配置按键，默认 `R`）
- **一键施法**（可配置按键，默认 `C`；无需打开任何 GUI）
- **HUD**：显示当前选中法术的**图标 + 名称**，位置/缩放/颜色全部由配置决定（不硬编码）

## 依赖

| 依赖 | 说明 |
|---|---|
| **Fabric Loader / Fabric API** | 基础运行环境 |
| **Spell Engine**（1.20.1） | 法术系统核心 |

## 构建

```
./gradlew build
```

产物在 `build/libs/spell_wheel-<version>.jar`。

- 依赖从 Modrinth maven 解析；离线时可把依赖 jar 放进 `libs/`（`modImplementation fileTree('libs')` 会自动加载）。

## 安装

把 `spell_wheel-<version>.jar` 与 **Spell Engine**、**Fabric API** 一起放进 Fabric 的 `mods/` 目录。
想有法术可选，还需要一个 Spell Engine **内容模组**（如 Wizards / Paladins / Archers）。

## 配置（`config/spell_wheel.json`）

```json
{
  "enabled": true,          // HUD 总开关
  "showBackground": true,   // 背景面板
  "showIcon": true,         // 法术图标
  "showName": true,         // 法术名称
  "anchor": "BOTTOM_RIGHT", // BOTTOM_LEFT / BOTTOM_RIGHT / TOP_LEFT / TOP_RIGHT
  "offsetX": 12,            // 相对锚点偏移（像素）
  "offsetY": 12,
  "scale": 1.0,             // 缩放（0.25 ~ 4.0）
  "color": "#80FFFFFF",     // 背景 ARGB
  "debug": false,           // 调试日志
  "castVia": "hotbar"       // 施法方式：hotbar / network
}
```

## 按键（控制 → 按键绑定 → 法术轮盘）

| 名称 | 默认 |
|---|---|
| 循环切换当前法术 | `R` |
| 施放当前选中法术 | `C` |

## 施法方式（`castVia`）

| 值 | 说明 |
|---|---|
| **`hotbar`（默认）** | **无感驱动 Spell Engine 热栏键位**：本模组把选中法术在 Spell Engine 热栏上的键位按下/松开，由 Spell Engine 自己的逻辑完成开始/前摇/引导/释放（含动画）。按住施法键期间请**持续按住**。 |
| `network` | 直接向服务端发送 `SpellRequest(RELEASE)` 包，由服务端执行 `SpellHelper.performSpell`。一次性释放，**无前摇/引导动画**，适合投射物/自身类法术。 |

## 架构

```
com.ansex.spellwheel
├── SpellWheelMod            # ModInitializer：载入配置
├── config/SpellWheelConfig  # JSON 配置（HUD 位置/外观/施法方式）
├── client
│   ├── SpellWheelClient     # ClientModInitializer：按键、HUD 回调、客户端 tick
│   ├── KeyBindings          # Fabric KeyBinding（CYCLE / CAST）
│   ├── SpellWheelHud        # HudRenderCallback：绘制图标 + 名称
│   ├── SpellWheelKeyHandler # 按键逻辑（切换 / 施法）
│   └── SpellEngineHotbar    # 方案 B：驱动 Spell Engine 热栏键位
└── core
    ├── SpellSelectionState  # 法术列表缓存 + 选中下标
    ├── SpellEngineBridge    # Spell Engine 类型化集成（读取法术 / 施法）
    └── SpellWheelSpell      # 法术视图对象（id / 名称 / 图标贴图）
```

## 说明

- 法术来源：主手 / 副手（经 `SpellContainerHelper.getEquipped` 解析，含 proxy 聚合）+ 护甲槽。
- 图标：`<namespace>:textures/spell/<path>.png`；名称：语言键 `spell.<namespace>.<path>.name`。
- 有施法时间（前摇/引导）的法术请使用 `castVia = "hotbar"` 并**按住**施法键。
