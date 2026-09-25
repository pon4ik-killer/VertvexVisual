package dev.sxmurxy.vertexvisual.util.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.sxmurxy.vertexvisual.VertexVisualMod;
import dev.sxmurxy.vertexvisual.Wrapper;
import dev.sxmurxy.vertexvisual.themes.ThemeManager;
import dev.sxmurxy.vertexvisual.util.font.styled.StyledFontRenderer;
import dev.sxmurxy.vertexvisual.settings.SettingsManager;

import java.awt.Color;

public class WatermarkRenderer implements Wrapper {

    private static final Color[] WATERMARK_COLORS = {
            new Color(1, 0, 15),
            new Color(33, 0, 82),
            new Color(0, 10, 3)
    };

    private static long startTime = System.currentTimeMillis();

    public static void render(MatrixStack matrixStack) {
        // СНАЧАЛА РЕНДЕРИМ ЛОГОТИП
        LogoRenderer.render(matrixStack);

        // ПОТОМ ОСНОВНУЮ ВАТЕРМАРКУ
        String watermark = "VertexVisual";
        double time = (System.currentTimeMillis() - startTime) * 0.001;

        PingRenderer.render(matrixStack);
        UsernameRenderer.render(matrixStack);

        // 🎯 ОТНОСИТЕЛЬНЫЕ КООРДИНАТЫ ДЛЯ СЕРВЕРА
        float screenWidth = MC.getWindow().getGuiScaledWidth();
        float screenHeight = MC.getWindow().getGuiScaledHeight();

        // Сохраняем твои координаты, но делаем их относительными
        float centerX = 61f;
        float centerY = 32.5f;

        // Если экран широкий, корректируем позицию
        if (screenWidth > 1000) {
            centerX = screenWidth * 0.057f;  // 5.7% от ширины
            centerY = screenHeight * 0.045f; // 4.5% от высоты
        }

        // 🎯 РАЗМЕРЫ ТЕКСТА КАСТОМНЫМ ШРИФТОМ
        float textWidth;
        float textHeight;

        if (VertexVisualMod.textFont != null) {
            textWidth = VertexVisualMod.textFont.getWidth(watermark);
            textHeight = VertexVisualMod.textFont.getFontHeight();
        } else {
            textWidth = MC.font.width(watermark);
            textHeight = MC.font.lineHeight;
        }

        // Размеры панели (немного больше текста)
        double width = textWidth + 9;
        double height = 19.5;

        // Координаты панели (центрируем)
        float panelX = centerX - (float) width / 2;
        float panelY = centerY - (float) height / 2;

        // Координаты текста (центрируем относительно панели)
        float textX = panelX + (float) (width - textWidth) / 2;
        float textY = panelY + (float) (height - textHeight) / 2;

        // Анимированные цвета как в меню
        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();
        Color[] animatedColors = getAnimatedColors(currentGradient, time);

        // СНАЧАЛА СВЕЧЕНИЕ
        BloomHelper.registerRenderCall(() -> {
            drawWatermarkGlow(panelX, panelY, width, height, time, currentGradient); // ← ДОБАВЬ currentGradient
        });
        BloomHelper.draw(10);

        // ПОТОМ ОСНОВНАЯ ПАНЕЛЬ
        DrawHelper.drawRoundedGradientRect(panelX, panelY, width, height, 8,
                animatedColors[0], animatedColors[1], animatedColors[2], animatedColors[0]);

        // ТЕКСТ (с измененным положением)
        Color textColor = new Color(255, 255, 255, 200);

        // 🎯 НОВЫЕ КООРДИНАТЫ ТЕКСТА
        float newTextX = textX + 0.5f;  // 🎯 Сохраняем твою настройку
        float newTextY = textY - 10f;   // 🎯 Сохраняем твою настройку

        // 🎯 ИСПОЛЬЗУЕМ КАСТОМНЫЙ ШРИФТ
        if (VertexVisualMod.textFont != null) {
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, watermark, newTextX, newTextY, textColor);
        } else {
            MC.font.drawShadow(matrixStack, watermark, newTextX, newTextY, textColor.getRGB());
        }
    }

    private static void drawWatermarkGlow(float panelX, float panelY, double width, double height, double time, Color[] gradientColors) {
        float glowIntensity = (float) (Math.sin(time * 2) * 0.3 + 0.7);

        // Используем цвет из темы для свечения
        Color baseGlowColor = gradientColors[1]; // берем средний цвет градиента
        Color glowColor = new Color(
                baseGlowColor.getRed(),
                baseGlowColor.getGreen(),
                baseGlowColor.getBlue(),
                (int) (170 * glowIntensity)
        );

        DrawHelper.drawRoundedBlurredRect(
                panelX - 5, panelY + 5, width + 8, height + 8, 10, 4, glowColor
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

