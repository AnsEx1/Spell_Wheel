package com.ansex.spellwheel.client;

import com.ansex.spellwheel.SpellWheelMod;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;

/** 客户端初始化：注册按键、HUD 渲染回调、客户端 tick。 */
public class SpellWheelClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(KeyBindings.CYCLE);
        KeyBindingHelper.registerKeyBinding(KeyBindings.CAST);

        HudRenderCallback.EVENT.register((context, tickDelta) -> SpellWheelHud.render(context));
        ClientTickEvents.END_CLIENT_TICK.register(SpellWheelKeyHandler::onClientTick);

        SpellWheelMod.LOGGER.info("[{}] client initialized", SpellWheelMod.MOD_ID);
    }
}
