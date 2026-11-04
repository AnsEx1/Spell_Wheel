package com.ansex.spellwheel.core;

import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * 供 HUD 展示与施法使用的法术视图对象。
 * <p>图标遵循 Spell Engine 约定：{@code <namespace>:textures/spell/<path>.png}。</p>
 */
public record SpellWheelSpell(Identifier id, Text displayName) {

    /** 法术图标贴图（可能不存在，缺失时显示缺失贴图占位）。 */
    public Identifier iconTexture() {
        return new Identifier(id.getNamespace(), "textures/spell/" + id.getPath() + ".png");
    }
}
