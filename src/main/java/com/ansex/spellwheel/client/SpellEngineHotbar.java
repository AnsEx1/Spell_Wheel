package com.ansex.spellwheel.client;

import com.ansex.spellwheel.SpellWheelMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import net.spell_engine.client.input.SpellHotbar;

/**
 * 无感驱动 Spell Engine 热栏键位（方案 B）。
 * <p>
 * 找到选中法术在 Spell Engine 热栏上的槽位，取其键位并 {@code setPressed(true/false)}，
 * 让 Spell Engine 自己的 {@code SpellHotbar.handle} 完成开始/推进/释放（含前摇、引导、动画）。
 * 玩家可见操作仍是本模组的施法键。
 * </p>
 */
public final class SpellEngineHotbar {

    /** 当前被按下的热栏键位（松开时恢复）。 */
    private static KeyBinding heldKey;

    private SpellEngineHotbar() {
    }

    /** 按下选中法术对应的热栏键位。 */
    public static boolean press(Identifier spellId) {
        release();
        if (spellId == null) {
            return false;
        }
        var options = MinecraftClient.getInstance().options;
        for (SpellHotbar.Slot slot : SpellHotbar.INSTANCE.slots) {
            if (!slot.spell().id().equals(spellId)) {
                continue;
            }
            KeyBinding key = slot.getKeyBinding(options);
            if (key == null) {
                SpellWheelMod.LOGGER.warn("[{}] 该法术槽位没有可用键位: {}", SpellWheelMod.MOD_ID, spellId);
                return false;
            }
            key.setPressed(true);
            heldKey = key;
            SpellWheelMod.LOGGER.info("[{}] 按下热栏键位 {} -> {}", SpellWheelMod.MOD_ID, key.getTranslationKey(), spellId);
            return true;
        }
        SpellWheelMod.LOGGER.warn("[{}] 热栏中没有该法术的槽位（是否手持施法物品？）: {}", SpellWheelMod.MOD_ID, spellId);
        return false;
    }

    /** 松开之前按下的热栏键位。 */
    public static void release() {
        if (heldKey != null) {
            heldKey.setPressed(false);
            SpellWheelMod.LOGGER.info("[{}] 松开热栏键位 {}", SpellWheelMod.MOD_ID, heldKey.getTranslationKey());
            heldKey = null;
        }
    }
}
