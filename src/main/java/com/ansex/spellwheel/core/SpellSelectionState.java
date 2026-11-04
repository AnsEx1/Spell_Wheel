package com.ansex.spellwheel.core;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * 客户端维护的法术选择状态：缓存法术列表 + 选中下标。
 * <p>列表由 {@link #tick(PlayerEntity)} 按间隔刷新，避免 HUD 每帧都读取 Spell Engine。</p>
 */
public final class SpellSelectionState {

    private static final SpellSelectionState INSTANCE = new SpellSelectionState();

    /** 列表刷新间隔（tick）。 */
    private static final int REFRESH_INTERVAL_TICKS = 5;

    private int selectedIndex = -1;
    private Identifier selectedSpellId;

    private List<SpellWheelSpell> cached = List.of();
    private long lastRefreshTick = -1000L;

    private SpellSelectionState() {
    }

    public static SpellSelectionState get() {
        return INSTANCE;
    }

    /** 每客户端 tick 调用：按间隔刷新缓存。 */
    public void tick(PlayerEntity player) {
        if (player == null) {
            cached = List.of();
            lastRefreshTick = -1000L;
            return;
        }
        long now = player.age;
        if (now < lastRefreshTick || now - lastRefreshTick >= REFRESH_INTERVAL_TICKS) {
            refresh(player);
        }
    }

    /** 立即刷新缓存。 */
    public void refresh(PlayerEntity player) {
        cached = (player == null) ? List.of() : SpellEngineBridge.getSpells(player);
        lastRefreshTick = (player == null) ? -1000L : player.age;
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
}
