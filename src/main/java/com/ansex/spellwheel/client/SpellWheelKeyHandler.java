package com.ansex.spellwheel.client;

import com.ansex.spellwheel.SpellWheelMod;
import com.ansex.spellwheel.config.SpellWheelConfig;
import com.ansex.spellwheel.core.SpellEngineBridge;
import com.ansex.spellwheel.core.SpellSelectionState;
import com.ansex.spellwheel.core.SpellWheelSpell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * 按键处理（每客户端 tick 调用一次）。
 * <ul>
 *   <li>刷新法术列表缓存（事件驱动）。</li>
 *   <li>切换键：按下推进选中下标。</li>
 *   <li>施法键：按下开始施法、松开结束。</li>
 * </ul>
 */
public final class SpellWheelKeyHandler {

    private static final int PROGRESS_LOG_INTERVAL = 5;

    private static long lastProgressLogTick = Long.MIN_VALUE;
    private static boolean castKeyDown = false;

    private SpellWheelKeyHandler() {
    }

    /** 由 {@code ClientTickEvents.END_CLIENT_TICK} 调用。 */
    public static void onClientTick(MinecraftClient client) {
        if (client.player == null) {
            // 离开世界时复位，避免状态泄漏到下一次进入
            castKeyDown = false;
            SpellSelectionState.get().tick(null);
            return;
        }
        SpellSelectionState selection = SpellSelectionState.get();
        selection.tick(client.player);
        boolean debug = SpellWheelConfig.get().debug;

        if (KeyBindings.CYCLE.wasPressed()) {
            selection.refresh(client.player);
            SpellWheelSpell next = selection.cycle();
            if (next == null) {
                notifyPlayer(client, Text.translatable("spell_wheel.hud.empty"));
            } else {
                notifyPlayer(client, next.displayName());
            }
            SpellWheelMod.LOGGER.debug("[{}] 切换按键: 可用法术={} 选中={}",
                    SpellWheelMod.MOD_ID, selection.available().size(), next == null ? "<none>" : next.id());
        }

        boolean down = KeyBindings.CAST.isPressed();
        if (down && !castKeyDown) {
            castStart(client, selection, debug);
        } else if (!down && castKeyDown) {
            castEnd();
        }
        castKeyDown = down;

        if (debug && SpellEngineBridge.isCasting(client.player)) {
            long now = client.player.age;
            if (now < lastProgressLogTick || now - lastProgressLogTick >= PROGRESS_LOG_INTERVAL) {
                lastProgressLogTick = now;
                SpellWheelMod.LOGGER.debug("[{}] 施法进度: {}",
                        SpellWheelMod.MOD_ID, SpellEngineBridge.castProgress(client.player));
            }
        }
    }

    private static void castStart(MinecraftClient client, SpellSelectionState selection, boolean debug) {
        SpellWheelSpell current = selection.current();
        if (current == null) {
            notifyPlayer(client, Text.translatable("spell_wheel.hud.empty"));
            return;
        }
        if (debug) {
            SpellWheelMod.LOGGER.debug("[{}] 施法按键按下: {}", SpellWheelMod.MOD_ID, current.id());
            SpellWheelMod.LOGGER.debug("[{}] 法术信息: {}",
                    SpellWheelMod.MOD_ID, SpellEngineBridge.describeSpell(current.id()));
        }

        SpellEngineHotbar.Result result = SpellEngineHotbar.press(current.id());
        if (result != SpellEngineHotbar.Result.OK) {
            notifyPlayer(client, Text.translatable(failureKey(result), current.displayName()));
            return;
        }

        if (debug) {
            SpellWheelMod.LOGGER.debug("[{}] 施法后: isCasting={}, progress={}",
                    SpellWheelMod.MOD_ID,
                    SpellEngineBridge.isCasting(client.player),
                    SpellEngineBridge.castProgress(client.player));
        }
    }

    private static String failureKey(SpellEngineHotbar.Result result) {
        return switch (result) {
            case NO_SPELL -> "spell_wheel.cast.no_spell";
            case NOT_ON_HOTBAR -> "spell_wheel.cast.not_on_hotbar";
            case NO_KEYBINDING -> "spell_wheel.cast.no_keybinding";
            case OK -> "spell_wheel.cast.ok";
        };
    }

    private static void castEnd() {
        SpellEngineHotbar.release();
    }

    /** 在 actionbar 提示玩家（不刷聊天栏）。 */
    private static void notifyPlayer(MinecraftClient client, Text message) {
        if (client.player != null && message != null) {
            client.player.sendMessage(message, true);
        }
    }
}
