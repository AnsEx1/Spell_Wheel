package com.ansex.spellwheel.client;

import com.ansex.spellwheel.SpellWheelMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import net.spell_engine.client.input.SpellHotbar;

/**
 * 无感驱动 Spell Engine 热栏键位。
 * <p>
 * 找到选中法术在 Spell Engine 热栏上的槽位，取其键位并 {@code setPressed(true/false)}，
 * 让 Spell Engine 自己的 {@code SpellHotbar.handle} 完成开始/推进/释放（含前摇、引导、动画）。
 * 玩家可见操作仍是本模组的施法键。
 * </p>
 */
public final class SpellEngineHotbar {

    /** 按下结果，用于向玩家反馈失败原因。 */
    public enum Result {
        OK,
        /** 当前没有选中法术。 */
        NO_SPELL,
        /** Spell Engine 热栏里找不到该法术的槽位（未装备对应施法物品）。 */
        NOT_ON_HOTBAR,
        /** 槽位存在但该法术没有可用键位。 */
        NO_KEYBINDING
    }

    /** 当前被按下的热栏键位（松开时恢复）。 */
    private static KeyBinding heldKey;

    private SpellEngineHotbar() {
    }

    /** 按下选中法术对应的热栏键位。 */
    public static Result press(Identifier spellId) {
        release();
        if (spellId == null) {
            return Result.NO_SPELL;
        }
        SpellHotbar hotbar = SpellHotbar.INSTANCE;
        if (hotbar == null || hotbar.slots == null) {
            SpellWheelMod.LOGGER.debug("[{}] Spell Engine 热栏尚未初始化", SpellWheelMod.MOD_ID);
            return Result.NOT_ON_HOTBAR;
        }
        var options = MinecraftClient.getInstance().options;
        for (SpellHotbar.Slot slot : hotbar.slots) {
            if (!slot.spell().id().equals(spellId)) {
                continue;
            }
            KeyBinding key = slot.getKeyBinding(options);
            if (key == null) {
                SpellWheelMod.LOGGER.warn("[{}] 该法术槽位没有可用键位: {}", SpellWheelMod.MOD_ID, spellId);
                return Result.NO_KEYBINDING;
            }
            key.setPressed(true);
            heldKey = key;
            SpellWheelMod.LOGGER.debug("[{}] 按下热栏键位 {} -> {}", SpellWheelMod.MOD_ID, key.getTranslationKey(), spellId);
            return Result.OK;
        }
        SpellWheelMod.LOGGER.debug("[{}] 热栏中没有该法术的槽位（是否手持施法物品？）: {}", SpellWheelMod.MOD_ID, spellId);
        return Result.NOT_ON_HOTBAR;
    }

    /** 松开之前按下的热栏键位。 */
    public static void release() {
        if (heldKey != null) {
            heldKey.setPressed(false);
            SpellWheelMod.LOGGER.debug("[{}] 松开热栏键位 {}", SpellWheelMod.MOD_ID, heldKey.getTranslationKey());
            heldKey = null;
        }
    }
}
