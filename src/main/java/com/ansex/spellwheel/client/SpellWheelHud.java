package com.ansex.spellwheel.client;

import com.ansex.spellwheel.config.SpellWheelConfig;
import com.ansex.spellwheel.core.SpellEngineBridge;
import com.ansex.spellwheel.core.SpellSelectionState;
import com.ansex.spellwheel.core.SpellWheelSpell;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Objects;
import java.util.Optional;

/**
 * 法术轮盘 HUD：显示当前选中法术的图标 + 名称（可选冷却遮罩与施法进度条）。
 * <p>位置/缩放/颜色全部来自 {@link SpellWheelConfig}，不硬编码。</p>
 * <p>性能：每帧渲染，故按“当前选中法术 + 语言”缓存名称字符串/宽度/图标贴图，
 * 避免每帧重复分配与计算。</p>
 */
public final class SpellWheelHud {

    private static final int ICON_SIZE = 16;
    private static final int GAP = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int COOLDOWN_OVERLAY = 0xA0000000;
    private static final int PROGRESS_COLOR = 0xFF4FA3FF;
    private static final int PROGRESS_HEIGHT = 2;

    /** 空法术占位文案（本地化键）。 */
    private static final Text EMPTY_NAME = Text.translatable("spell_wheel.hud.empty");

    // --- 每帧渲染缓存（仅在选中法术 / 语言变化时更新）---
    private static Identifier cachedSpellId;
    private static Text cachedNameSource;
    private static Identifier cachedIcon;
    private static String cachedName;
    private static int cachedNameWidth;

    private SpellWheelHud() {
    }

    /** 由 {@code HudRenderCallback} 每帧调用。 */
    public static void render(DrawContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.player == null || client.player.isSpectator()) {
            return;
        }
        SpellWheelConfig config = SpellWheelConfig.get();
        if (!config.enabled) {
            return;
        }

        SpellWheelSpell spell = SpellSelectionState.get().current();
        updateCache(spell, client);

        boolean showIcon = config.showIcon && cachedIcon != null;
        boolean showName = config.showName;
        int iconBox = showIcon ? ICON_SIZE : 0;
        int width = GAP * 2 + iconBox + (showName ? (showIcon ? GAP : 0) + cachedNameWidth : 0);
        int height = Math.max(ICON_SIZE, client.textRenderer.fontHeight + 2) + GAP * 2;

        int scaledWidth = (int) Math.round(width * config.scale);
        int scaledHeight = (int) Math.round(height * config.scale);
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        int x;
        int y;
        switch (config.anchor) {
            case TOP_LEFT -> {
                x = config.offsetX;
                y = config.offsetY;
            }
            case TOP_RIGHT -> {
                x = screenWidth - config.offsetX - scaledWidth;
                y = config.offsetY;
            }
            case BOTTOM_LEFT -> {
                x = config.offsetX;
                y = screenHeight - config.offsetY - scaledHeight;
            }
            default -> {
                x = screenWidth - config.offsetX - scaledWidth;
                y = screenHeight - config.offsetY - scaledHeight;
            }
        }

        // 冷却 / 施法进度（每个法术每帧最多各查一次）
        Optional<Float> cooldown = spell == null
                ? Optional.empty()
                : SpellEngineBridge.cooldownRemaining(client.player, spell.id());
        float castRatio = SpellEngineBridge.castProgress(client.player).orElse(-1F);

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0);
        matrices.scale((float) config.scale, (float) config.scale, 1.0F);

        if (config.showBackground) {
            context.fill(0, 0, width, height, config.colorArgb());
        }
        if (showIcon) {
            context.drawTexture(cachedIcon, GAP, GAP, 0, 0, ICON_SIZE, ICON_SIZE, ICON_SIZE, ICON_SIZE);
            // 冷却遮罩：按剩余比例从下往上覆盖
            if (config.showCooldown && cooldown.isPresent()) {
                int overlayHeight = Math.round(ICON_SIZE * cooldown.get());
                if (overlayHeight > 0) {
                    context.fill(GAP, GAP + ICON_SIZE - overlayHeight,
                            GAP + ICON_SIZE, GAP + ICON_SIZE, COOLDOWN_OVERLAY);
                }
            }
        }
        if (showName) {
            int nameX = GAP + iconBox + (showIcon ? GAP : 0);
            int nameY = height / 2 - client.textRenderer.fontHeight / 2;
            context.drawTextWithShadow(client.textRenderer, cachedName, nameX, nameY, TEXT_COLOR);
        }
        // 施法进度条：贴底边绘制
        if (config.showCastProgress && castRatio >= 0F) {
            int barWidth = Math.round(width * Math.min(1F, castRatio));
            if (barWidth > 0) {
                context.fill(0, height - PROGRESS_HEIGHT, barWidth, height, PROGRESS_COLOR);
            }
        }

        matrices.pop();
    }

    /** 仅在选中法术（或语言）变化时重算名称/宽度/图标贴图。 */
    private static void updateCache(SpellWheelSpell spell, MinecraftClient client) {
        Identifier id = (spell == null) ? null : spell.id();
        Text nameSource = (spell == null) ? EMPTY_NAME : spell.displayName();
        if (Objects.equals(id, cachedSpellId) && Objects.equals(nameSource, cachedNameSource)) {
            return;
        }
        cachedSpellId = id;
        cachedNameSource = nameSource;
        cachedIcon = (spell == null) ? null : spell.iconTexture();
        // 语言切换后同一法术的显示名会变，因此每次都从 Text 重新解析
        cachedName = nameSource.getString();
        cachedNameWidth = client.textRenderer.getWidth(cachedName);
    }
}