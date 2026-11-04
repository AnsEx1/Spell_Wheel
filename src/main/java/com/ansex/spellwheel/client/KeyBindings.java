package com.ansex.spellwheel.client;

import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** 按键定义：循环切换法术（R）与一键施法（C）。均可在「控制 → 按键绑定」中修改。 */
public final class KeyBindings {

    public static final String CATEGORY = "key.categories.spell_wheel";

    /** 循环切换当前法术。 */
    public static final KeyBinding CYCLE = new KeyBinding(
            "key.spell_wheel.cycle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORY);

    /** 施放当前选中法术。 */
    public static final KeyBinding CAST = new KeyBinding(
            "key.spell_wheel.cast", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY);

    private KeyBindings() {
    }
}
