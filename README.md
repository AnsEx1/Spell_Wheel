# Spell Wheel (法术轮盘)

为 **Spell Engine** 添加一个**法术轮盘**功能：循环切换当前法术、一键施法、以及一个位置可配置的 HUD。

- **Minecraft 1.20.1 · Fabric**
- 作者：**AnsEX**、**deepseek**
- 仓库：<https://github.com/AnsEx1/Spell_Wheel>
- 开发历程：[DEVELOPMENT_HISTORY.md](DEVELOPMENT_HISTORY.md)
- 许可证：[LICENSE.txt](LICENSE.txt)

## 功能

- **循环切换**当前选中法术（可配置按键，默认 `R`）
- **一键施法**（可配置按键，默认 `C`；无需打开任何 GUI）
- **HUD**：显示当前选中法术的**图标 + 名称**，位置/缩放/颜色全部由配置决定（不硬编码）
- **冷却遮罩**：法术冷却时图标由下往上变暗
- **施法进度条**：前摇/引导期间在 HUD 底部显示进度
- **失败提示**：法术不在热栏、或没有可用键位时，通过 actionbar 直接告知原因

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
  "enabled": true,           // HUD 总开关
  "showBackground": true,    // 背景面板
  "showIcon": true,          // 法术图标
  "showName": true,          // 法术名称
  "showCooldown": true,      // 冷却遮罩
  "showCastProgress": true,  // 施法进度条
  "anchor": "BOTTOM_RIGHT",  // BOTTOM_LEFT / BOTTOM_RIGHT / TOP_LEFT / TOP_RIGHT
  "offsetX": 12,             // 相对锚点偏移（像素）
  "offsetY": 12,
  "scale": 1.0,              // 缩放（0.25 ~ 4.0）
  "color": "#80FFFFFF",      // 背景 ARGB（#RRGGBB 会自动补全为不透明）
  "debug": false             // 调试日志（默认关闭）
}
```

> 修改后需重新进入世界或重启客户端生效。

## 按键（控制 → 按键绑定 → 法术轮盘）

| 名称 | 默认 |
|---|---|
| 循环切换当前法术 | `R` |
| 施放当前选中法术 | `C` |

## 施法机制

本模组**驱动 Spell Engine 自己的法术热栏键位**：找到选中法术在 Spell Engine 热栏上的槽位，
取其键位并模拟按下/松开，由 Spell Engine 完整执行开始、前摇、引导与释放（含动画与音效）。

- 有前摇/引导的法术：**按住**施法键直到释放。
- 瞬发法术：按一下即可。

> 早先版本曾提供 `castVia: "network"`（客户端直接发包），因绕过前摇、且与冷却/引导不同步，
> 已在 1.1.0 移除。

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
- 法术列表**事件驱动刷新**：仅在手持物品种类或护甲变化时重算，HUD 每帧只读缓存。
- 本模组为**纯客户端**模组（`environment: client`），服务器无需安装。

## 测试

```bash
./gradlew test -PwithTests    # 需要能访问 Maven Central 解析 JUnit
```

默认构建不编译测试源码，便于离线构建。
