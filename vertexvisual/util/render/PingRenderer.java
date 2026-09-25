package dev.sxmurxy.vertexvisual.util.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.sxmurxy.vertexvisual.VertexVisualMod;
import dev.sxmurxy.vertexvisual.Wrapper;
import dev.sxmurxy.vertexvisual.themes.ThemeManager;
import dev.sxmurxy.vertexvisual.util.font.styled.StyledFontRenderer;
import net.minecraft.client.network.play.NetworkPlayerInfo;

import java.awt.Color;

public class PingRenderer implements Wrapper {

    private static long startTime = System.currentTimeMillis();

    public static void render(MatrixStack matrixStack) {
        // Получаем пинг
        int ping = getPing();
        String pingText = "Ping:" + ping;

        double time = (System.currentTimeMillis() - startTime) * 0.001;

        // 🎯 ОТНОСИТЕЛЬНЫЕ КООРДИНАТЫ ДЛЯ СЕРВЕРА
        float screenWidth = MC.getWindow().getGuiScaledWidth();
        float screenHeight = MC.getWindow().getGuiScaledHeight();

        // Сохраняем твои координаты, но делаем их относительными
        float centerX = 107f;
        float centerY = 32.5f;

        // Если экран широкий, корректируем позицию
        if (screenWidth > 1000) {
            centerX = screenWidth * 0.1f;    // 10% от ширины
            centerY = screenHeight * 0.045f; // 4.5% от высоты
        }

        // 🎯 РАЗМЕРЫ ТЕКСТА КАСТОМНЫМ ШРИФТОМ
        float textWidth;
        float textHeight;

        if (VertexVisualMod.textFont != null) {
            textWidth = VertexVisualMod.textFont.getWidth(pingText);
            textHeight = VertexVisualMod.textFont.getFontHeight();
        } else {
            textWidth = MC.font.width(pingText);
            textHeight = MC.font.lineHeight;
        }

        // Размеры панели
        double width = textWidth + 9;
        double height = 19.5;

        // Координаты панели
        float panelX = centerX - (float) width / 2;
        float panelY = centerY - (float) height / 2;

        // Координаты текста
        float textX = panelX + (float) (width - textWidth) / 2;
        float textY = panelY + (float) (height - textHeight) / 2;

        // ИСПОЛЬЗУЕМ ЦВЕТА ИЗ ТЕКУЩЕЙ ТЕМЫ
        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();
        Color[] animatedColors = getAnimatedColors(currentGradient, time);

        // СВЕЧЕНИЕ
        BloomHelper.registerRenderCall(() -> {
            drawPingGlow(panelX, panelY, width, height, time, currentGradient);
        });
        BloomHelper.draw(10);

        // ОСНОВНАЯ ПАНЕЛЬ
        DrawHelper.drawRoundedGradientRect(panelX, panelY, width, height, 8,
                animatedColors[0], animatedColors[1], animatedColors[2], animatedColors[0]);

        // ТЕКСТ ПИНГА
        Color textColor = getPingColor(ping);

        // 🎯 НОВЫЕ КООРДИНАТЫ ТЕКСТА
        float newTextX = textX;
        float newTextY = textY - 10f;  // 🎯 Сохраняем твою настройку

        // 🎯 ИСПОЛЬЗУЕМ КАСТОМНЫЙ ШРИФТ ТАК ЖЕ КАК В ВАТЕРМАРКЕ
        if (VertexVisualMod.textFont != null) {
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, pingText, newTextX, newTextY, textColor);
        } else {
            MC.font.drawShadow(matrixStack, pingText, newTextX, newTextY, textColor.getRGB());
        }
    }

    private static int getPing() {
        if (MC.player == null || MC.player.connection == null) return 0;

        NetworkPlayerInfo playerInfo = MC.player.connection.getPlayerInfo(MC.player.getGameProfile().getId());
        return playerInfo != null ? playerInfo.getLatency() : 0;
    }

    private static Color getPingColor(int ping) {
        if (ping < 50) return new Color(255, 255, 255, 200);      // 🟢 Зеленый - отличный пинг
        if (ping < 100) return new Color(255, 255, 255, 200);   // 🟡 Желтый - хороший пинг
        if (ping < 200) return new Color(255, 255, 255, 200);   // 🟠 Оранжевый - средний пинг
        return new Color(255, 255, 255, 200);                     // 🔴 Красный - плохой пинг
    }

    private static void drawPingGlow(float panelX, float panelY, double width, double height, double time, Color[] gradientColors) {
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
                panelX - 4, panelY + 5, width + 8, height + 8, 10, 4, glowColor
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