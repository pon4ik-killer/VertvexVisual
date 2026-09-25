package dev.sxmurxy.vertexvisual.sky;

import dev.sxmurxy.vertexvisual.themes.ThemeManager;
import dev.sxmurxy.vertexvisual.themes.Theme;
import net.minecraft.util.math.vector.Vector3f;
import java.awt.Color;

public class SkyColorManager {
    private static boolean enabled = true;
    private static Vector3f customSkyColor = null;

    public static void updateSkyColor() {
        if (!enabled) {
            customSkyColor = null;
            return;
        }

        Theme theme = ThemeManager.getCurrentTheme();
        Color[] gradientColors = theme.getGradientColors();

        // Берем цвет из темы и делаем его ПРИГЛУШЕННЫМ
        Color themeColor = gradientColors[1];

        // УМЕНЬШАЕМ яркость и насыщенность для комфорта глаз
        float brightnessReduction = 0.4f;
        float saturationReduction = 0.6f;

        // Конвертируем RGB в HSL для лучшего контроля
        float[] hsb = Color.RGBtoHSB(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), null);

        // Уменьшаем насыщенность и яркость
        hsb[1] = hsb[1] * saturationReduction;
        hsb[2] = hsb[2] * brightnessReduction;

        // Конвертируем обратно в RGB
        int rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]);
        Color softenedColor = new Color(rgb);

        // Конвертируем в вектор для неба
        customSkyColor = new Vector3f(
                softenedColor.getRed() / 255.0f,
                softenedColor.getGreen() / 255.0f,
                softenedColor.getBlue() / 255.0f
        );

        System.out.println("Updated sky color - Original: R=" + themeColor.getRed() +
                " Softened: R=" + softenedColor.getRed());

        // Обновляем космические эффекты
        updateSkyEffects();
    }

    // НОВЫЙ МЕТОД: обновление космических эффектов
    public static void updateSkyEffects() {
        if (enabled) {
        }
    }

    // НОВЫЙ МЕТОД: рендер космических эффектов
    public static void renderSkyEffects(com.mojang.blaze3d.matrix.MatrixStack matrixStack) {
        if (enabled) {
        }
    }

    public static Vector3f getSkyColor() {
        return customSkyColor;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        SkyColorManager.enabled = enabled;
        if (enabled) {
            updateSkyColor();
        } else {
            customSkyColor = null;
        }
    }

    public static void onThemeChanged() {
        if (enabled) {
            updateSkyColor();
        }
    }

    public static Color getFogColor() {
        Vector3f skyColor = getSkyColor();
        if (skyColor != null && isEnabled()) {
            return new Color(
                    (int)(skyColor.x() * 255),
                    (int)(skyColor.y() * 255),
                    (int)(skyColor.z() * 255),
                    80
            );
        }
        return new Color(0, 0, 0, 80);
    }
}