package com.ansex.spellwheel.core;

import com.ansex.spellwheel.SpellWheelMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.SpellContainer;
import net.spell_engine.internals.SpellCooldownManager;
import net.spell_engine.internals.SpellContainerHelper;
import net.spell_engine.internals.SpellRegistry;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCasterClient;
import net.spell_engine.internals.casting.SpellCasterEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Spell Engine 集成层（Fabric 原生类型化调用，无反射）。
 * <p>
 * 法术来源：主手/副手（经 {@code getEquipped} 解析，含 proxy 聚合的副手与饰品）+ 护甲槽。
 * 图标与名称遵循 Spell Engine 约定（{@code textures/spell/<path>.png}、{@code spell.<ns>.<path>.name}）。
 * </p>
 */
public final class SpellEngineBridge {

    private SpellEngineBridge() {
    }

    /** 读取玩家当前可施放的法术列表（含来源物品，用于施法）。 */
    public static List<SpellWheelSpell> getSpells(PlayerEntity player) {
        if (player == null) {
            return List.of();
        }
        Map<Identifier, ItemStack> sources = new LinkedHashMap<>();
        addFromGetEquipped(player.getMainHandStack(), player, sources);
        addFromGetEquipped(player.getOffHandStack(), player, sources);
        for (ItemStack stack : player.getArmorItems()) {
            addFromStack(stack, sources);
        }
        List<SpellWheelSpell> result = new ArrayList<>(sources.size());
        for (Identifier id : sources.keySet()) {
            result.add(new SpellWheelSpell(id, resolveName(id)));
        }
        return result;
    }

    /** 原生：getEquipped 会解析 proxy（聚合副手/饰品法术）。 */
    private static void addFromGetEquipped(ItemStack stack, PlayerEntity player, Map<Identifier, ItemStack> out) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        collect(SpellContainerHelper.getEquipped(stack, player), stack, out);
    }

    private static void addFromStack(ItemStack stack, Map<Identifier, ItemStack> out) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        collect(SpellContainerHelper.containerFromItemStack(stack), stack, out);
    }

    private static void collect(SpellContainer container, ItemStack source, Map<Identifier, ItemStack> out) {
        if (container == null || container.spell_ids == null) {
            return;
        }
        for (String idString : container.spell_ids) {
            Identifier id = Identifier.tryParse(idString);
            if (id != null) {
                out.putIfAbsent(id, source);
            }
        }
    }

    private static Text resolveName(Identifier id) {
        if (SpellRegistry.getSpell(id) != null) {
            return Text.translatable("spell." + id.getNamespace() + "." + id.getPath() + ".name");
        }
        return Text.literal(id.getPath().replace('_', ' '));
    }

    /**
     * 当前施法进度（0..1，含端点）。
     * <p>返回 {@link Optional#empty()} 表示未在施法；这与“进度恰为 0”可区分。</p>
     */
    public static Optional<Float> castProgress(PlayerEntity player) {
        if (!(player instanceof SpellCasterClient caster)) {
            return Optional.<Float>empty();
        }
        SpellCast.Progress progress = caster.getSpellCastProgress();
        if (progress == null) {
            return Optional.<Float>empty();
        }
        return Optional.of(Math.max(0F, Math.min(1F, progress.ratio())));
    }

    /** 玩家是否正在施法。 */
    public static boolean isCasting(PlayerEntity player) {
        return player instanceof SpellCasterClient caster && caster.isCastingSpell();
    }

    /**
     * 法术剩余冷却比例（0..1）。
     * <p>返回 {@link Optional#empty()} 表示法术不在冷却中；有值表示仍在冷却、值越大剩余越多。</p>
     */
    public static Optional<Float> cooldownRemaining(PlayerEntity player, Identifier spellId) {
        if (player == null || spellId == null) {
            return Optional.<Float>empty();
        }
        SpellCooldownManager manager = cooldownManager(player);
        if (manager == null) {
            return Optional.<Float>empty();
        }
        try {
            float progress = manager.getCooldownProgress(spellId, 0F);
            if (progress <= 0F) {
                return Optional.<Float>empty();
            }
            return Optional.of(Math.min(1F, progress));
        } catch (Throwable t) {
            return Optional.<Float>empty();
        }
    }

    private static SpellCooldownManager cooldownManager(PlayerEntity player) {
        return player instanceof SpellCasterEntity entity ? entity.getCooldownManager() : null;
    }

    /** 调试：描述法术 mode / 施法时间。 */
    public static String describeSpell(Identifier id) {
        Spell spell = SpellRegistry.getSpell(id);
        if (spell == null) {
            return "spell=null";
        }
        return "mode=" + spell.mode
                + ", duration=" + spell.cast.duration
                + ", channel_ticks=" + spell.cast.channel_ticks;
    }
}