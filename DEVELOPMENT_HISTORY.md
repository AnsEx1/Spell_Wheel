# 开发历程（Development History）

本项目在首次公开发布前，将 44 条开发提交整理为单条初始提交（2026-11-05）。
原始逐条历史完整保留在本地分支 `backup/pre-squash-20261105`，未推送到远端。

本文是那 44 条提交的**完整归纳**——记录真实走过的路径，包括死胡同，便于后来者（也包括未来的自己）理解"为什么代码是现在这样"。

---

## 总览

| 项目 | 数值 |
|---|---|
| 原始提交数 | 44 条（全部经过整理，当前远端为 1 条） |
| 开发时间跨度 | 2026-09-08 → 2026-11-05 |
| 累计代码变动 | +3,589 / −2,254 行，157 次文件改动 |
| 最终代码规模 | 10 个 Java 文件，689 行 |
| 产物 | `spell_wheel-1.0.0.jar`（约 31 KB，含 sources jar） |

时间分布极不均匀：42 条提交集中在 **2026-09-08 单日**完成（一次性高强度开发），其余 2 条为后续的仓库整理。

---

## 阶段一：Forge 原型（21 次提交）

**目标**：先把"能跑"做出来，平台选 NeoForge/Forge 1.20.1。

**核心架构**（`0fb73d5`，+991 行）：一次性搭起 21 个文件的骨头——ForgeGradle 6.0 构建、`mods.toml`、主类、HUD、按键、配置、Spell Engine 桥接。

**UI 框架选型 — 第一次弯路**：选用 **LDLib2** 的 `ModularHudLayer + ModularUI`（`07d1fae`）。随之而来的是一连串平台适配问题：

| 问题 | 提交 | 解决方式 |
|---|---|---|
| `ResourceLocation` 废弃 API | `0aa61ee` `2c0543b` | 回退到构造器（1.20.1 无 `fromNamespaceAndPath`） |
| `settings.gradle` 块顺序 | `4ad8496` `37971db` | `pluginManagement` 必须置首 |
| `mods.toml` 写成 NeoForge 格式 | `502bac3` | 改为 Forge 的 `mandatory=true/false` |
| GUI overlay 注册签名 | `00d5ce1` `87af449` | 1.20.1 收 `String`；`registerAboveAll` 会自动加命名空间前缀 |
| 依赖解析（离线/本地） | `bcc2888` `168d750` `779e94c` `cee1b98` `9de6ac4` | 改用 `libs/` 本地 jar + `mavenLocal()`，配套 CI |
| LDLib2 版本坐标错误 | `e745f22` | 不锁版本，要求 2+ |

**Spell Engine 集成 — 靠"猜"到"实锤"**：`067a347` 与 `7670126` 两次修正，才把桥接对准真实的 1.20.1 API（`internals.SpellContainerHelper` / `SpellRegistry` / `SpellCasterClient`），并加 `isInstance` 保护（`4a6e781`）。

**HUD 反复三次**：

1. LDLib2 实现 → `d66f657` 发现 **overlay 根本不绘制**，且 GL 报 `Invalid key -1`，改用原版 `GuiGraphics` 手绘。
2. 参考 Iron's Spells 重构成 `IGuiOverlay` 单例（`35dd578`）。
3. 又因反射 bug 回退到 LDLib2（`b0ee3ba`）—— **随后在同一天被彻底推翻**（见阶段二）。

**法术来源**：从"只读主手"逐步扩展到 `a364ab3` 的**全装备源**（主手/副手/护甲 + Curios/饰品法术书），最终 `c41cb44` 改用 Spell Engine 原生 `getEquipped(ItemStack, Player)`，让引擎自己处理 proxy 聚合。

**施法机制 — 四次反复**：

| 提交 | 做法 | 结果 |
|---|---|---|
| `dc21c3e` | 用"法术的来源物品"施法 + 显示法术原生图标 | 可行，成为基础 |
| `819be7e` | 改用主手物品施法，修复蓄力法术被取消 | 修复 |
| `fe66a11` | 按住施法 + 松键释放引导法术 + 修正首次循环下标 | 修复 |
| `e5736e7` | **不再**在松键时取消施法 | 推翻上一版的取消逻辑 |

---

## 阶段二：彻底剥离 LDLib2（6 次提交）

`c5119fa` 是**转折点**：删掉 4.9 MB 的 `ldlib2-forge-...jar`，**完全改成原版渲染**。

**这一决定的价值**：
- 依赖从「Spell Engine + LDLib2」降到只剩 **Spell Engine**
- 构建不再需要本地 jar，CI 从"易碎"变"稳定"
- 体积、启动开销、兼容风险同步下降

伴随清理：`show_background` 与图标/名称独立开关（`c0f412b`）、法术列表缓存与 HUD 缩放（`2f5fd3b`）。

---

## 阶段三：施法方案定型（5 次提交）

这是全项目最曲折的部分——**如何触发 Spell Engine 的施法**。

**方案 A：network**（`372e0d2`）
客户端直接发 `SpellRequest(RELEASE)` 给服务端，由 `SpellHelper.performSpell` 执行。
→ 优点：直接。缺点：**一次性释放，没有前摇/引导动画**，蓄力类法术体验很差。

**方案 B：hotbar**（`c154ea2`，最终默认）
不碰网络包，而是**无感地按下/松开 Spell Engine 自己的热栏键位**，把开始/前摇/引导/释放全部交还给引擎逻辑。
→ 动画、音效、引导行为全部原生正确。

**取舍结论**：保留两者，用 `castVia` 配置切换，默认 `hotbar`；`2e75035` 删除了走不通的 `castViaClient` 路径。

调试能力也在这一阶段沉淀：`debug` 开关（`76a30ac`）、施法状态即时日志（`c73220e`、`d7ef84b`）、诊断前置。

---

## 阶段四：移植到 Fabric（1 次提交，但最重）

`214a24c`：**+1,056 / −1,195 行，31 个文件**。

- 构建：ForgeGradle → **fabric-loom 1.7**
- 包名：`net.spellwheel` → `com.ansex.spellwheel`
- 移除 Forge 专属件：`mods.toml`、`SpellWheelForgeEvents`、Forge 事件总线
- 入口：`ModInitializer` + `ClientModInitializer`
- 渲染：`IGuiOverlay` → Fabric `HudRenderCallback`
- 按键：Forge `KeyMapping` → Fabric `KeyBinding`

**为何换 Fabric**：更轻的构建链、对 Spell Engine 生态更友好、CI 依赖面更小。

随后 `bd45eb4` 优化 HUD/配置热路径，并确认无 Forge/LDLib 残留；`f24c515` 清除 IDE/agent 等非模组产物。

---

## 阶段五：发布与仓库整理（2 次提交）

- `3d6dd25`：仓库链接统一为 `AnsEx1/Spell_Wheel`（`README.md`、`fabric.mod.json`）
- `6afe4cc`：`gradlew`/`gradlew.bat` 补 `+x` 位 —— 修复 CI 的 `Permission denied`（exit 126）

---

## 经验沉淀

**1. 依赖越少，路越宽。**
LDLib2 占了 4.9 MB，且带来一串注册/绘制兼容问题；换成原版渲染后问题成片消失。能用平台原生能力就不要引第三方 UI 框架。

**2. "驱动引擎自己的入口"优于"绕过引擎"。**
方案 B（驱动热栏键位）让 Spell Engine 完整接管施法生命周期，动画/引导/音效自动正确；方案 A 只是"看起来能放技能"。集成第三方模组时，优先复用它的输入适配层，而不是复刻它的内部调用。

**3. 反射集成必须先读源码。**
`SpellEngineBridge` 前后修了三次（`067a347`、`7670126`、`b0ee3ba`），本质是"按记忆猜 API"。对照真实源码后一次就对。

**4. UI 层要独立于渲染框架。**
HUD 在 5 种实现间反复（LDLib2 → 原版 → IGuiOverlay → 又回 LDLib2 → 最终原版），根因是把业务逻辑和 UI 框架耦合了。现在 `SpellWheelHud` 只依赖一个 `SpellWheelSpell` 视图对象。

**5. CI 从第一天就接上。**
虽然在 `9de6ac4` 才加，但正是 CI 第一时间暴露了 `gradlew` 可执行位问题——本地 Windows 看不出来，Linux runner 直接 exit 126。

---

## 保留原始历史的办法

压缩前的 44 条提交仍在本地：

```bash
git log backup/pre-squash-20261105          # 查看完整历史
git diff backup/pre-squash-20261105 HEAD    # 与当前状态对比（应为空）
git checkout backup/pre-squash-20261105     # 如需检视某个中间版本
```

该分支**未推送**到远端，需要时再 `git push origin backup/pre-squash-20261105`。
