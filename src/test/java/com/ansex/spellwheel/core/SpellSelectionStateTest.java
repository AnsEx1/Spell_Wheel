package com.ansex.spellwheel.core;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link SpellSelectionState} 的纯逻辑测试（不依赖 Minecraft 运行时）。
 * <p>覆盖循环切换、空列表、列表顺序变化三类边界。</p>
 */
class SpellSelectionStateTest {

    private static SpellWheelSpell spell(String id) {
        return new SpellWheelSpell(new Identifier("test", id), Text.literal(id));
    }

    private static List<SpellWheelSpell> three() {
        List<SpellWheelSpell> list = new ArrayList<>();
        list.add(spell("a"));
        list.add(spell("b"));
        list.add(spell("c"));
        return list;
    }

    @Test
    void emptyListYieldsNoSelection() {
        SpellSelectionState state = SpellSelectionState.get();
        state.replaceForTesting(List.of());

        assertNull(state.current());
        assertNull(state.cycle());
        assertNull(state.current());
    }

    @Test
    void firstCycleSelectsFirstSpell() {
        SpellSelectionState state = SpellSelectionState.get();
        state.replaceForTesting(three());

        assertSame(state.available().get(0), state.cycle());
        assertEquals("a", state.current().id().getPath());
    }

    @Test
    void cyclingWrapsAround() {
        SpellSelectionState state = SpellSelectionState.get();
        state.replaceForTesting(three());

        state.cycle(); // a
        assertEquals("b", state.cycle().id().getPath());
        assertEquals("c", state.cycle().id().getPath());
        assertEquals("a", state.cycle().id().getPath());
    }

    @Test
    void selectionFollowsSpellWhenListOrderChanges() {
        SpellSelectionState state = SpellSelectionState.get();
        state.replaceForTesting(three());
        state.cycle(); // a
        state.cycle(); // b
        assertEquals("b", state.current().id().getPath());

        // 列表顺序变化：b 挪到末尾
        List<SpellWheelSpell> reordered = new ArrayList<>();
        reordered.add(spell("c"));
        reordered.add(spell("a"));
        reordered.add(spell("b"));
        state.replaceForTesting(reordered);

        assertEquals("b", state.current().id().getPath());
    }

    @Test
    void selectionResetsWhenSpellDisappears() {
        SpellSelectionState state = SpellSelectionState.get();
        state.replaceForTesting(three());
        state.cycle(); // a
        state.cycle(); // b

        List<SpellWheelSpell> withoutB = new ArrayList<>();
        withoutB.add(spell("a"));
        withoutB.add(spell("c"));
        state.replaceForTesting(withoutB);

        // 选中的 b 不在列表里 -> 回退到列表首项
        assertEquals("a", state.current().id().getPath());
    }

    @Test
    void cyclingAfterSpellDisappearsStartsFromHead() {
        SpellSelectionState state = SpellSelectionState.get();
        state.replaceForTesting(three());
        state.cycle(); // a
        state.cycle(); // b

        List<SpellWheelSpell> withoutB = new ArrayList<>();
        withoutB.add(spell("a"));
        withoutB.add(spell("c"));
        state.replaceForTesting(withoutB);

        // syncIndex 找不到 b 时应回退，使下一次切换从头/当前位推进而不越界
        SpellWheelSpell next = state.cycle();
        assertEquals("c", next.id().getPath());
    }
}
