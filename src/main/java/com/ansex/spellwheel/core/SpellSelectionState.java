package com.ansex.spellwheel.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;

/**
 * 客户端维护的法术选择状态：缓存法术列表 + 选中下标。
 * <p>列表改为<b>事件驱动</b>刷新：仅当玩家实例、手持物品种类或护甲内容变化时才重算，
 * 不再每 N tick 无差别轮询。HUD 每帧只读缓存。</p>
 */
public final class SpellSelectionState {

    private static final SpellSelectionState INSTANCE = new SpellSelectionState();

    /** 兜底刷新间隔（tick）：防止漏掉非物品栏变化带来的列表变动。 */
    private static final int SAFETY_REFRESH_TICKS = 40;

    private int selectedIndex = -1;
    private Identifier selectedSpellId;

    private List<SpellWheelSpell> cached = List.of();
    private long lastRefreshTick = -1000L;
    private int lastSignature = 0;

    private SpellSelectionState() {
    }

    public static SpellSelectionState get() {
        return INSTANCE;
    }

    /** 每客户端 tick 调用：仅在物品栏签名变化时刷新缓存。 */
    public void tick(PlayerEntity player) {
        if (player == null) {
            reset();
            return;
        }
        int signature = signatureOf(player);
        long now = player.age;
        boolean stale = now < lastRefreshTick || now - lastRefreshTick >= SAFETY_REFRESH_TICKS;
        if (signature != lastSignature || stale) {
            refresh(player);
        }
    }

    /** 立即刷新缓存。 */
    public void refresh(PlayerEntity player) {
        if (player == null) {
            reset();
            return;
        }
        cached = SpellEngineBridge.getSpells(player);
        lastRefreshTick = player.age;
        lastSignature = signatureOf(player);
    }

    private void reset() {
        cached = List.of();
        selectedIndex = -1;
        selectedSpellId = null;
        lastRefreshTick = -1000L;
        lastSignature = 0;
    }

    /**
     * 影响法术列表的最小状态指纹：手持物品种类 + 护甲内容。
     * <p>只取 {@code item} 而非整份 NBT，避免每次 tick 深比较。</p>
     */
    private static int signatureOf(PlayerEntity player) {
        int h = 1;
        var main = player.getMainHandStack();
        var off = player.getOffHandStack();
        h = 31 * h + (main.isEmpty() ? 0 : main.getItem().hashCode());
        h = 31 * h + (off.isEmpty() ? 0 : off.getItem().hashCode());
        for (var stack : player.getArmorItems()) {
            h = 31 * h + (stack.isEmpty() ? 0 : stack.getItem().hashCode());
        }
        return h;
    }

    public List<SpellWheelSpell> available() {
        return cached;
    }

    /** 当前选中法术（来自缓存）。 */
    public SpellWheelSpell current() {
        if (cached.isEmpty()) {
            return null;
        }
        // 快路径：下标仍指向选中的法术
        if (selectedIndex >= 0 && selectedIndex < cached.size()) {
            SpellWheelSpell candidate = cached.get(selectedIndex);
            if (selectedSpellId == null || candidate.id().equals(selectedSpellId)) {
                return candidate;
            }
        }
        // 慢路径：按 id 重新定位（列表顺序变化时）
        if (selectedSpellId != null) {
            for (SpellWheelSpell spell : cached) {
                if (spell.id().equals(selectedSpellId)) {
                    return spell;
                }
            }
        }
        return cached.get(0);
    }

    /** 循环切换到下一个法术。 */
    public SpellWheelSpell cycle() {
        if (cached.isEmpty()) {
            selectedIndex = -1;
            selectedSpellId = null;
            return null;
        }
        syncIndex();
        int next = (selectedIndex + 1) % cached.size();
        selectedIndex = next;
        SpellWheelSpell spell = cached.get(next);
        selectedSpellId = spell.id();
        return spell;
    }

    /** 依据 selectedSpellId 同步下标（首次切换保持 -1，使 next = 0）。 */
    private void syncIndex() {
        if (selectedSpellId == null) {
            selectedIndex = -1;
            return;
        }
        for (int i = 0; i < cached.size(); i++) {
            if (cached.get(i).id().equals(selectedSpellId)) {
                selectedIndex = i;
                return;
            }
        }
        if (selectedIndex < 0 || selectedIndex >= cached.size()) {
            selectedIndex = 0;
        }
    }

    /**
     * 测试与热重载入口：直接用给定列表替换缓存（不依赖 Minecraft 运行时）。
     */
    public void replaceForTesting(List<SpellWheelSpell> spells) {
        cached = Objects.requireNonNullElse(spells, List.of());
        lastRefreshTick = -1000L;
        lastSignature = 0;
    }
}
