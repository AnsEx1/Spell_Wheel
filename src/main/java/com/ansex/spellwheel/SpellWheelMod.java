package com.ansex.spellwheel;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Spell Wheel — 为 Spell Engine 提供法术轮盘（切换 / 一键施法 / 可配置 HUD）。
 * <p>Minecraft 1.20.1 · Fabric · 作者：AnsEX</p>
 */
public class SpellWheelMod implements ModInitializer {

    public static final String MOD_ID = "spell_wheel";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 本模组为纯客户端功能（environment = client），配置由客户端初始化时载入。
        LOGGER.info("[{}] initialized", MOD_ID);
    }
}
