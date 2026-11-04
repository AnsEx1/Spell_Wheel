package com.ansex.spellwheel.client;

import com.ansex.spellwheel.SpellWheelMod;
import com.ansex.spellwheel.config.SpellWheelConfig;
import com.ansex.spellwheel.core.SpellEngineBridge;
import com.ansex.spellwheel.core.SpellSelectionState;
import com.ansex.spellwheel.core.SpellWheelSpell;
import net.minecraft.client.MinecraftClient;

/**
 * 按键处理（每客户端 tick 调用一次）。
 * <ul>
 *   <li>刷新法术列表缓存。</li>
 *   <li>切换键：按下推进选中下标。</li>
 *   <li>施法键：按下开始施法、松开结束（方式由配置 {@code castVia} 决定）。</li>
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
            return;
        }
        SpellSelectionState selection = SpellSelectionState.get();
        selection.tick(client.player);
        boolean debug = SpellWheelConfig.get().debug;

        if (KeyBindings.CYCLE.wasPressed()) {
            selection.refresh(client.player);
            SpellWheelSpell next = selection.cycle();
            SpellWheelMod.LOGGER.info("[{}] 切换按键: 可用法术={} 选中={}",
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
                SpellWheelMod.LOGGER.info("[{}][debug] 施法进度: {}",
                        SpellWheelMod.MOD_ID, SpellEngineBridge.castProgress(client.player));
            }
        }
    }

    private static void castStart(MinecraftClient client, SpellSelectionState selection, boolean debug) {
        SpellWheelSpell current = selection.current();
        if (current == null) {
            SpellWheelMod.LOGGER.info("[{}] 施法按键: 当前没有选中法术（未持有施法物品？）", SpellWheelMod.MOD_ID);
            return;
        }
        SpellWheelConfig config = SpellWheelConfig.get();
        SpellWheelMod.LOGGER.info("[{}] 施法按键按下: {} (方式={})",
                SpellWheelMod.MOD_ID, current.id(), config.castVia);
        if (debug) {
            SpellWheelMod.LOGGER.info("[{}][debug] 法术信息: {}",
                    SpellWheelMod.MOD_ID, SpellEngineBridge.describeSpell(current.id()));
        }

        if (config.isNetworkCast()) {
            SpellEngineBridge.castViaNetwork(client.player, current.id());
        } else {
            SpellEngineHotbar.press(current.id());
        }

        if (debug) {
            SpellWheelMod.LOGGER.info("[{}][debug] 施法后: isCasting={}, progress={}",
                    SpellWheelMod.MOD_ID,
                    SpellEngineBridge.isCasting(client.player),
                    SpellEngineBridge.castProgress(client.player));
        }
    }

    private static void castEnd() {
        if (!SpellWheelConfig.get().isNetworkCast()) {
            SpellEngineHotbar.release();
        }
    }
}
