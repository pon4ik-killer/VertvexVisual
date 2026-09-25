package dev.sxmurxy.vertexvisual.themes;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class ThemeManager {
    private static final List<Theme> themes = new ArrayList<>();
    private static int currentThemeIndex = 0;

    // ИЗМЕНЯЕМ: кастомная тема теперь последняя
    private static int customThemeIndex = -1; // Будет вычисляться динамически
    // НОВЫЕ ПОЛЯ ДЛЯ РАБОТЫ С ПАЛИТРОЙ
    private static boolean colorPickerOpen = false;
    private static Color currentEditingColor1, currentEditingColor2, currentEditingColor3;
    private static int currentEditingColorIndex = 0;
    private static double colorWheelAngle = 0;
    private static double colorWheelRadius = 0.5;
    private static double brightnessValue = 0.5;
    private static boolean isDraggingColorWheel = false;
    private static boolean isDraggingBrightness = false;

    static {
        // Сначала добавляем все стандартные темы
        themes.add(new Theme("Стандарт",
                new Color[]{
                        new Color(1, 0, 15),
                        new Color(33, 0, 82),
                        new Color(0, 10, 3)
                },
                new Color[]{
                        new Color(0x65, 0x00, 0xb8),
                        new Color(0x80, 0x01, 0xfe),
                        new Color(0x49, 0x00, 0xa3)
                }
        ));

        themes.add(new Theme("Кровавый",
                new Color[]{
                        new Color(0x00, 0x00, 0x00),
                        new Color(0x47, 0x00, 0x00),
                        new Color(0x00, 0x00, 0x00)
                },
                new Color[]{
                        new Color(0x8f, 0x00, 0x00),
                        new Color(0xff, 0x00, 0x00),
                        new Color(0x8f, 0x00, 0x00)
                }
        ));

        themes.add(new Theme("Сакура",
                new Color[]{
                        new Color(0x52, 0x00, 0x4b),
                        new Color(0x70, 0x00, 0x36),
                        new Color(0x99, 0x00, 0x42),
                        new Color(0xc2, 0x00, 0x54),
                        new Color(0xff, 0x00, 0x7b)
                },
                new Color[]{
                        new Color(0xff, 0xff, 0xff),
                        new Color(0xff, 0xff, 0xff),
                        new Color(0xff, 0xff, 0xff),
                        new Color(0xff, 0xff, 0xff),
                        new Color(0xff, 0xff, 0xff)
                }
        ));

        themes.add(new Theme("Изумруд",
                new Color[]{
                        new Color(0x00, 0x00, 0x00),
                        new Color(0x00, 0x33, 0x32),
                        new Color(0x00, 0x00, 0x00)
                },
                new Color[]{
                        new Color(0x00, 0x99, 0x66),
                        new Color(0x00, 0xff, 0xaa),
                        new Color(0x00, 0x99, 0x73)
                }
        ));

        themes.add(new Theme("Синий",
                new Color[]{
                        new Color(0x00, 0x00, 0x00),
                        new Color(0x01, 0x00, 0x4d),
                        new Color(0x00, 0x00, 0x00)
                },
                new Color[]{
                        new Color(0, 100, 255),
                        new Color(50, 150, 255),
                        new Color(0, 80, 200)
                }
        ));

        themes.add(new Theme("Янтарный",
                new Color[]{
                        new Color(0x00, 0x00, 0x00),
                        new Color(0x4d, 0x3c, 0x00),
                        new Color(0x00, 0x00, 0x00)
                },
                new Color[]{
                        new Color(255, 200, 0),
                        new Color(255, 230, 100),
                        new Color(220, 170, 0)
                }
        ));

        themes.add(new Theme("Лайм",
                new Color[]{
                        new Color(0x00, 0x1f, 0x06),
                        new Color(0x00, 0x75, 0x17),
                        new Color(0x00, 0x33, 0x0d)
                },
                new Color[]{
                        new Color(0x51, 0x94, 0x00),
                        new Color(0x4d, 0xfe, 0x01),
                        new Color(0x37, 0x8a, 0x00)
                }
        ));

        // Теперь добавляем кастомную тему ПОСЛЕ всех стандартных
        Color[] defaultColors = themes.get(0).getGradientColors();
        currentEditingColor1 = defaultColors[0];
        currentEditingColor2 = defaultColors[1];
        currentEditingColor3 = defaultColors[2];

        // ИЗМЕНЯЕМ: добавляем в конец, а не в начало!
        themes.add(new Theme("Моя тема",
                new Color[]{ currentEditingColor1, currentEditingColor2, currentEditingColor3 },
                new Color[]{  // Белые цвета для категорий
                        new Color(255, 255, 255),
                        new Color(255, 255, 255),
                        new Color(255, 255, 255)
                }
        ));
        customThemeIndex = themes.size() - 1;
    }

    public static Color[] getCategoryColorsForMenu() {
        if (currentThemeIndex == customThemeIndex) {  // ← ИСПРАВЛЕНО ЗДЕСЬ
            // Для кастомной темы возвращаем белые цвета
            return new Color[]{
                    new Color(255, 255, 255),
                    new Color(255, 255, 255),
                    new Color(255, 255, 255)
            };
        } else {
            // Для стандартных тем используем обычные цвета
            return getCurrentTheme().getCategoryColors();
        }
    }

    public static Color getDarkenedColor(Color baseColor, float darkenFactor) {
        // Создаем более темную версию цвета
        float[] hsb = Color.RGBtoHSB(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), null);

        // Уменьшаем яркость и насыщенность
        float newBrightness = Math.max(0, hsb[2] * darkenFactor);
        float newSaturation = Math.min(1, hsb[1] * 1.1f); // немного увеличиваем насыщенность для контраста

        return Color.getHSBColor(hsb[0], newSaturation, newBrightness);
    }

    public static void updateCustomTheme(Color color1, Color color2, Color color3) {
        Theme customTheme = new Theme("Моя тема",
                new Color[]{color1, color2, color3},
                new Color[]{  // Белые цвета для категорий
                        new Color(255, 255, 255),
                        new Color(255, 255, 255),
                        new Color(255, 255, 255)
                });
        themes.set(customThemeIndex, customTheme);  // ← ИСПРАВЛЕНО
    }

    public static int getCustomThemeIndex() {
        return customThemeIndex;  // ← ИСПРАВЛЕНО
    }

    public static Theme getCurrentTheme() {
        return themes.get(currentThemeIndex);
    }

    public static void nextTheme() {
        currentThemeIndex = (currentThemeIndex + 1) % themes.size();
    }

    public static void previousTheme() {
        currentThemeIndex = (currentThemeIndex - 1 + themes.size()) % themes.size();
    }

    public static void setTheme(int index) {
        if (index >= 0 && index < themes.size()) {
            currentThemeIndex = index;
            dev.sxmurxy.vertexvisual.sky.SkyColorManager.onThemeChanged();
        }
    }

    public static List<Theme> getThemes() {
        return new ArrayList<>(themes);
    }

    public static int getCurrentThemeIndex() {
        return currentThemeIndex;
    }

    // НОВЫЕ МЕТОДЫ ДЛЯ РАБОТЫ С ПАЛИТРОЙ И КАСТОМНОЙ ТЕМОЙ
    public static void applyCustomTheme() {
        currentEditingColor2 = getDarkenedColor(currentEditingColor1, 0.6f);
        updateCustomTheme(currentEditingColor1, currentEditingColor2, currentEditingColor3);
        setTheme(customThemeIndex);  // ← ИСПРАВЛЕНО
    }

    public static boolean isCustomThemeActive() {
        return currentThemeIndex == customThemeIndex;  // ← ИСПРАВЛЕНО
    }

    public static boolean isCustomThemeModified() {
        Color[] defaultColors = themes.get(0).getGradientColors(); // Сравниваем со стандартной темой (индекс 0)
        return !currentEditingColor1.equals(defaultColors[0]) ||
                !currentEditingColor2.equals(defaultColors[1]) ||
                !currentEditingColor3.equals(defaultColors[2]);
    }

    public static Color getCurrentSelectedColor() {
        return getColorFromWheel(colorWheelAngle, colorWheelRadius);
    }

    private static Color getColorFromWheel(double angle, double saturation) {
        float hue = (float) (angle / 360.0);
        float sat = (float) saturation;
        float bright = (float) brightnessValue;
        return Color.getHSBColor(hue, sat, bright);
    }

    public static void updateSelectedColorFromWheel() {
        Color selectedColor = getCurrentSelectedColor();
        switch (currentEditingColorIndex) {
            case 0:
                currentEditingColor1 = selectedColor;
                // Автоматически создаем темный цвет для второго слота
                currentEditingColor2 = getDarkenedColor(selectedColor, 0.6f); // 60% яркости
                break;
            case 1:
                currentEditingColor2 = selectedColor;
                break;
            case 2:
                currentEditingColor3 = selectedColor;
                break;
        }
    }

    // ГЕТТЕРЫ И СЕТТЕРЫ ДЛЯ ПАЛИТРЫ

    public static boolean isColorPickerOpen() {
        return colorPickerOpen;
    }

    public static void setColorPickerOpen(boolean open) {
        colorPickerOpen = open;
    }

    public static Color getCurrentEditingColor1() {
        return currentEditingColor1;
    }

    public static Color getCurrentEditingColor2() {
        return currentEditingColor2;
    }

    public static Color getCurrentEditingColor3() {
        return currentEditingColor3;
    }

    public static int getCurrentEditingColorIndex() {
        return currentEditingColorIndex;
    }

    public static void setCurrentEditingColorIndex(int index) {
        currentEditingColorIndex = index;
    }

    public static double getColorWheelAngle() {
        return colorWheelAngle;
    }

    public static void setColorWheelAngle(double angle) {
        colorWheelAngle = angle;
    }

    public static double getColorWheelRadius() {
        return colorWheelRadius;
    }

    public static void setColorWheelRadius(double radius) {
        colorWheelRadius = radius;
    }

    public static double getBrightnessValue() {
        return brightnessValue;
    }

    public static void setBrightnessValue(double value) {
        brightnessValue = value;
    }

    public static boolean isDraggingColorWheel() {
        return isDraggingColorWheel;
    }

    public static void setDraggingColorWheel(boolean dragging) {
        isDraggingColorWheel = dragging;
    }

    public static boolean isDraggingBrightness() {
        return isDraggingBrightness;
    }

    public static void setDraggingBrightness(boolean dragging) {
        isDraggingBrightness = dragging;
    }
}