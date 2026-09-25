package dev.sxmurxy.vertexvisual.util.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.sxmurxy.vertexvisual.VertexVisualMod;
import dev.sxmurxy.vertexvisual.Wrapper;
import dev.sxmurxy.vertexvisual.themes.ThemeManager;
import dev.sxmurxy.vertexvisual.util.font.icon.IconRenderer;
import net.minecraft.client.Minecraft;

import java.awt.Color;

public class LogoRenderer implements Wrapper {

    private static long startTime = System.currentTimeMillis();

    // 🎯 ПОЗИЦИЯ ПАНЕЛИ
    private static final float PANEL_X = 3f;      // 🎯 МЕНЯЙ - позиция панели по X
    private static final float PANEL_Y = 22.7f;   // 🎯 МЕНЯЙ - позиция панели по Y
    private static final float PANEL_WIDTH = 28f;  // 🎯 МЕНЯЙ - ширина панели
    private static final float PANEL_HEIGHT = 19.5f; // 🎯 МЕНЯЙ - высота панели

    // 🎯 ПОЗИЦИЯ ИКОНКИ ОТНОСИТЕЛЬНО ПАНЕЛИ
    private static final float ICON_OFFSET_X = 6f;  // 🎯 МЕНЯЙ - сдвиг иконки по X
    private static final float ICON_OFFSET_Y = -21f;  // 🎯 МЕНЯЙ - сдвиг иконки по Y

    public static void render(MatrixStack matrixStack) {
        if (VertexVisualMod.logoFont == null) return;

        double time = (System.currentTimeMillis() - startTime) * 0.001;

        // 🎯 ОТНОСИТЕЛЬНЫЕ КООРДИНАТЫ ДЛЯ СЕРВЕРА
        float screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        float screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();

        // Сохраняем твои координаты, но делаем их относительными
        final float panelX; // 🎯 ДОБАВЬ final
        final float panelY; // 🎯 ДОБАВЬ final

        if (screenWidth > 1000) {
            panelX = screenWidth * 0.003f;  // 0.3% от ширины
            panelY = screenHeight * 0.032f; // 3.2% от высоты
        } else {
            panelX = PANEL_X; // 🎯 ДОБАВЬ else
            panelY = PANEL_Y; // 🎯 ДОБАВЬ else
        }

        // ИСПОЛЬЗУЕМ ЦВЕТА ИЗ ТЕКУЩЕЙ ТЕМЫ
        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();
        Color[] animatedColors = getAnimatedColors(currentGradient, time);

        // СВЕЧЕНИЕ ПАНЕЛИ
        BloomHelper.registerRenderCall(() -> {
            drawLogoGlow(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, time, currentGradient);
        });
        BloomHelper.draw(10);

        // ОСНОВНАЯ ПАНЕЛЬ
        DrawHelper.drawRoundedGradientRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 8,
                animatedColors[0], animatedColors[1], animatedColors[2], animatedColors[0]);

        // 🎯 ОТДЕЛЬНОЕ ПОЗИЦИОНИРОВАНИЕ ИКОНКИ
        float iconX = panelX + (PANEL_WIDTH / 2) + ICON_OFFSET_X;
        float iconY = panelY + (PANEL_HEIGHT / 2) + ICON_OFFSET_Y;

        // РЕНДЕРИНГ ИКОНКИ
        IconRenderer.drawCenteredXYIcon(matrixStack, VertexVisualMod.logoFont, 'V',
                iconX, iconY, new Color(255, 255, 255, 220));
    }

    private static void drawLogoGlow(float panelX, float panelY, double width, double height, double time, Color[] gradientColors) {
        float glowIntensity = (float) (Math.sin(time * 2) * 0.3 + 0.7);

        // Используем цвет из темы для свечения
        Color baseGlowColor = gradientColors[1];
        Color glowColor = new Color(
                baseGlowColor.getRed(),
                baseGlowColor.getGreen(),
                baseGlowColor.getBlue(),
                (int) (160 * glowIntensity)
        );

        DrawHelper.drawRoundedBlurredRect(
                panelX - 1.5f, panelY + 2, width + 2, height + 3, 2, 3, glowColor
        );
    }

    private static Color[] getAnimatedColors(Color[] gradientColors, double time) {
        Color[] animatedColors = new Color[4];

        for (int i = 0; i < 3; i++) {
            Color baseColor = gradientColors[i];
            float pulse = (float) (Math.sin(time * 2 + i) * 0.1 + 0.9);

            int r = (int) (baseColor.getRed() * pulse + 20);
            int g = (int) (baseColor.getGreen() * pulse + 10);
            int b = (int) (baseColor.getBlue() * pulse + 15);

            animatedColors[i] = new Color(
                    Math.min(Math.max(r, 0), 255),
                    Math.min(Math.max(g, 0), 255),
                    Math.min(Math.max(b, 0), 255),
                    220
            );
        }
        animatedColors[3] = animatedColors[0];

        return animatedColors;
    }
}