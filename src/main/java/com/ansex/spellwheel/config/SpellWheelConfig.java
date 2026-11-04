package com.ansex.spellwheel.config;

import com.ansex.spellwheel.SpellWheelMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端配置（JSON：{@code config/spell_wheel.json}）。
 * <p>HUD 位置/外观完全可配置，不硬编码坐标。</p>
 */
public final class SpellWheelConfig {

    /** HUD 锚点。 */
    public enum Anchor {
        BOTTOM_LEFT, BOTTOM_RIGHT, TOP_LEFT, TOP_RIGHT
    }

    // --- HUD ---
    public boolean enabled = true;
    public boolean showBackground = true;
    public boolean showIcon = true;
    public boolean showName = true;
    /** 冷却中在图标上绘制暗色遮罩。 */
    public boolean showCooldown = true;
    /** 施法时在 HUD 底部绘制进度条（前摇/引导）。 */
    public boolean showCastProgress = true;
    public Anchor anchor = Anchor.BOTTOM_RIGHT;
    public int offsetX = 12;
    public int offsetY = 12;
    public double scale = 1.0;
    public String color = "#80FFFFFF";

    // --- 调试 ---
    /** 是否输出调试日志（默认关闭；开启后 info 级日志才会出现）。 */
    public boolean debug = false;

    // --- 运行时 ---
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static SpellWheelConfig instance = new SpellWheelConfig();
    private static Path path;
    /** 解析后的背景色缓存（避免每帧解析字符串）。 */
    private transient int cachedColor = Integer.MIN_VALUE;

    private SpellWheelConfig() {
    }

    public static SpellWheelConfig get() {
        return instance;
    }

    /** 载入（不存在则写出默认值）。 */
    public static void load() {
        path = configPath();
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                SpellWheelConfig loaded = GSON.fromJson(reader, SpellWheelConfig.class);
                if (loaded != null) {
                    instance = loaded;
                }
            } catch (Exception e) {
                SpellWheelMod.LOGGER.warn("[{}] 读取配置失败，使用默认值: {}", SpellWheelMod.MOD_ID, e.toString());
                instance = new SpellWheelConfig();
            }
        } else {
            save();
        }
    }

    /** 写出当前配置。 */
    public static void save() {
        if (path == null) {
            path = configPath();
        }
        try {
            Files.createDirectories(path.getParent());
            try (var writer = Files.newBufferedWriter(path)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            SpellWheelMod.LOGGER.warn("[{}] 写出配置失败: {}", SpellWheelMod.MOD_ID, e.toString());
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(SpellWheelMod.MOD_ID + ".json");
    }

    /** 背景颜色解析为 ARGB（结果缓存；失败回退半透明白）。 */
    public int colorArgb() {
        if (cachedColor == Integer.MIN_VALUE) {
            cachedColor = parseColor(color);
        }
        return cachedColor;
    }

    private static int parseColor(String value) {
        if (value == null) {
            return 0x80FFFFFF;
        }
        try {
            String trimmed = value.trim();
            if (trimmed.startsWith("#")) {
                trimmed = trimmed.substring(1);
            }
            long parsed = Long.parseLong(trimmed, 16);
            // 允许 #RRGGBB（补全不透明 alpha）与 #AARRGGBB
            if (trimmed.length() <= 6) {
                parsed |= 0xFF000000L;
            }
            return (int) parsed;
        } catch (Exception e) {
            return 0x80FFFFFF;
        }
    }
}
