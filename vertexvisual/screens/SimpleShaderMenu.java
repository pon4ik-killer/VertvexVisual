package dev.sxmurxy.vertexvisual.screens;

import java.awt.Color;
import java.util.*;

import com.mojang.blaze3d.matrix.MatrixStack;


import dev.sxmurxy.vertexvisual.settings.SettingsManager;
import dev.sxmurxy.vertexvisual.sky.SkyColorManager;
import dev.sxmurxy.vertexvisual.util.font.icon.IconRenderer;
import dev.sxmurxy.vertexvisual.util.render.BloomHelper;
import dev.sxmurxy.vertexvisual.util.render.BlurHelper;
import dev.sxmurxy.vertexvisual.themes.ThemeManager;
import dev.sxmurxy.vertexvisual.themes.Theme;
import dev.sxmurxy.vertexvisual.util.render.DrawHelper;
import dev.sxmurxy.vertexvisual.util.render.WatermarkRenderer;
import dev.sxmurxy.vertexvisual.VertexVisualMod;
import dev.sxmurxy.vertexvisual.util.font.styled.StyledFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.texture.Texture;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.util.text.StringTextComponent;


public class SimpleShaderMenu extends Screen {

    private double animation = 0;
    private static Minecraft mc = Minecraft.getInstance();
    private long startTime;
    private boolean searchFocused = false;
    private String searchText = "";
    private long cursorBlinkTimer = 0;
    private boolean sideMenuOpen = false;
    private double sideMenuAnimation = 0;
    private String activeCategory = "";
    private double themesStartX = 220;
    private double themesStartY = 110;
    private double themesClickX = 210;
    private double themesClickY = 90;
    private static final float BASE_SCREEN_WIDTH = 1920f;
    private static final float BASE_SCREEN_HEIGHT = 1080f;
    private int lastSelectedTheme = 0; // По умолчанию "Стандарт"
    private int currentActiveTheme = 0;
    private double themesMenuAnimation = 0.0;
    private long themesMenuOpenTime = 0;
    private boolean themesMenuWasOpen = false;
    private Map<Integer, Long> sliderAnimationTimes = new HashMap<>();
    private long sliderAnimationStartTime = System.currentTimeMillis(); // Добавлено поле для анимации слайдеров
    private boolean debugMode = false;
    private boolean hideWatermark = false;
    private double settingsMenuAnimation = 0.0;
    private long settingsMenuOpenTime = 0;
    private boolean settingsMenuWasOpen = false;
    private boolean skyColorEnabled = true;
    private double skyColorAnimation = 0.0;
    private boolean worldMenuWasOpen = false;
    private long worldMenuOpenTime = 0;
    private double worldMenuAnimation = 0.0;
    private boolean movementMenuWasOpen = false;
    private long movementMenuOpenTime = 0L;
    private double movementMenuAnimation = 0.0;
    private double colorPickerAnimation = 0.0;

    // Координаты, по которым рисуется меню и проверяются клики
    private double movementClickX = 0;
    private double movementClickY = 0;

    private Map<String, Long> modernSliderAnimationTimes = new HashMap<>();
    private Map<Integer, Long> worldSliderAnimationTimes = new HashMap<>();
    private Map<String, Double> menuSlideAnimations = new HashMap<>();
    private Map<String, Long> menuOpenTimes = new HashMap<>();
    private Map<String, Boolean> menuWasOpen = new HashMap<>();
    private Map<String, List<Double>> elementAnimations = new HashMap<>();
    private SettingsManager settingsManager = SettingsManager.getInstance();
    private Map<String, Long> settingsSliderAnimationTimes = new HashMap<>();
    private float scaleFactor = 1.0f;

    public SimpleShaderMenu() {
        super(new StringTextComponent(""));
        this.startTime = System.currentTimeMillis();
        this.cursorBlinkTimer = System.currentTimeMillis();

        // Добавьте инициализацию:
        modernSliderAnimationTimes = new HashMap<>();
        worldSliderAnimationTimes = new HashMap<>();
        menuSlideAnimations = new HashMap<>();
        menuOpenTimes = new HashMap<>();
        menuWasOpen = new HashMap<>();
        elementAnimations = new HashMap<>();

        calculateScaleFactor();
    }


    public static void toggle() {
        if (mc.screen instanceof SimpleShaderMenu) {
            mc.setScreen(null);
        } else {
            mc.setScreen(new SimpleShaderMenu());
        }
    }

    private void calculateScaleFactor() {
        float screenWidth = mc.getWindow().getGuiScaledWidth();
        float screenHeight = mc.getWindow().getGuiScaledHeight();

        if (screenWidth <= 800) {
            scaleFactor = 0.6f;
        } else if (screenWidth <= 1200) {
            scaleFactor = 0.75f;
        } else if (screenWidth <= 1600) {
            scaleFactor = 0.9f;
        } else {
            scaleFactor = 1.0f;
        }

        if (screenWidth < 600) scaleFactor = 0.5f;
        if (screenWidth < 400) scaleFactor = 0.4f;
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        calculateScaleFactor();

        boolean removeExternalGlow = settingsManager.getBoolean(SettingsManager.REMOVE_EXTERNAL_GLOW);

        List<Runnable> blurEffects = new ArrayList<>();
        List<Runnable> bloomEffects = new ArrayList<>();

        animation = Math.min(animation + partialTicks * 0.4f, 1.0);

        if (sideMenuOpen) {
            sideMenuAnimation = Math.min(sideMenuAnimation + partialTicks * 0.3f, 1.0);
        } else {
            sideMenuAnimation = Math.max(sideMenuAnimation - partialTicks * 0.3f, 0.0);
        }

        String[] animatedMenus = {"Themes", "World", "Movement", "Settings"};
        for (String menu : animatedMenus) {
            if (activeCategory.equals(menu)) {
                if (!menuWasOpen.getOrDefault(menu, false)) {
                    menuOpenTimes.put(menu, System.currentTimeMillis());
                    menuWasOpen.put(menu, true);
                    menuSlideAnimations.put(menu, 0.0);

                    List<Double> elementAnims = new ArrayList<>();
                    int elementCount = getElementCountForMenu(menu);
                    for (int i = 0; i < elementCount; i++) {
                        elementAnims.add(0.0);
                    }
                    elementAnimations.put(menu, elementAnims);
                }

                double slideProgress = Math.min(menuSlideAnimations.getOrDefault(menu, 0.0) + partialTicks * 0.15f, 1.0);
                menuSlideAnimations.put(menu, slideProgress);

                updateElementAnimations(menu, partialTicks);

            } else {
                if (menuWasOpen.getOrDefault(menu, false)) {
                    double slideProgress = Math.max(menuSlideAnimations.getOrDefault(menu, 0.0) - partialTicks * 0.15f, 0.0);
                    menuSlideAnimations.put(menu, slideProgress);

                    updateElementAnimationsOnClose(menu, partialTicks);

                    if (slideProgress <= 0.0) {
                        menuWasOpen.put(menu, false);
                    }
                }
            }
        }

        WatermarkRenderer.render(matrixStack);

        drawDarkBackground();

        blurEffects.add(() -> {
            drawBlurredOverlay();
        });

        double windowWidth = (width * 0.8) * scaleFactor;
        double windowHeight = (height * 0.0) * scaleFactor;

        windowWidth = Math.max(windowWidth, 500 * scaleFactor);
        windowHeight = Math.max(windowHeight, 300 * scaleFactor);

        double x = (width - windowWidth) / 2;
        double y = (height - windowHeight) / 2;

        double animatedWidth = windowWidth * animation;
        double animatedHeight = windowHeight * animation;
        double animatedX = x + (windowWidth - animatedWidth) / 2;
        double animatedY = y + (windowHeight - animatedHeight) / 2;

        double yOffset = windowHeight * 0.9;

        double time = (System.currentTimeMillis() - startTime) * 0.001;

        drawExternalGlow(animatedX, animatedY + yOffset, animatedWidth, animatedHeight, time);

        if (!hideWatermark) {
            WatermarkRenderer.render(matrixStack);
        }

        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();
        DrawHelper.drawRoundedGradientRect(animatedX, animatedY + yOffset, animatedWidth, animatedHeight, 15 * scaleFactor,
                getAnimatedColor(currentGradient, 0, time),
                getAnimatedColor(currentGradient, 1, time),
                getAnimatedColor(currentGradient, 2, time),
                getAnimatedColor(currentGradient, 0, time));

        bloomEffects.add(() -> {
            drawInternalGlow(animatedX, animatedY + yOffset, animatedWidth, animatedHeight, time);
        });

        drawArrowButton(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight, mouseX, mouseY, bloomEffects);

        drawSideMenu(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight, mouseX, mouseY, time);

        for (String menu : animatedMenus) {
            if (activeCategory.equals(menu)) {
                double slideProgress = menuSlideAnimations.getOrDefault(menu, 0.0);
                if (slideProgress > 0) {
                    switch (menu) {
                        case "Themes":
                            drawThemesMenuNew(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight, mouseX, mouseY, slideProgress);
                            break;
                        case "World":
                            drawWorldMenuNew(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight, mouseX, mouseY, slideProgress);
                            break;
                        case "Movement":
                            drawMovementMenuNew(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight, mouseX, mouseY, slideProgress);
                            break;
                        case "Settings":
                            drawSettingsMenuNew(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight, mouseX, mouseY, slideProgress);
                            break;
                    }
                }
            }
        }

        drawSearchPanel(matrixStack, animatedX, animatedY + yOffset, animatedWidth, animatedHeight);

        // ПАЛИТРА ДОЛЖНА БЫТЬ САМОЙ ВЕРХНЕЙ - РИСУЕМ ЕЕ ПОСЛЕДНЕЙ
        if (colorPickerAnimation > 0) {
            drawColorPickerOverlay(matrixStack, mouseX, mouseY, partialTicks);
        }

        // Blur эффекты (один вызов для всех)
        if (!blurEffects.isEmpty()) {
            BlurHelper.registerRenderCall(() -> {
                for (Runnable effect : blurEffects) {
                    effect.run();
                }
            });
            BlurHelper.draw(15);
        }

        // Bloom эффекты (один вызов для всех)
        if (!bloomEffects.isEmpty()) {
            BloomHelper.registerRenderCall(() -> {
                for (Runnable effect : bloomEffects) {
                    effect.run();
                }
            });
            BloomHelper.draw(8);
        }

        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    private void drawColorPickerOverlay(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        // Сначала рисуем саму палитру
        double pickerWidth = 400 * scaleFactor;
        double pickerHeight = 250 * scaleFactor;
        double pickerX = (width - pickerWidth) / 2;
        double pickerY = (height - pickerHeight) / 2 + 250 * scaleFactor;

        drawColorPicker(matrixStack, pickerX, pickerY, pickerWidth, pickerHeight, mouseX, mouseY, colorPickerAnimation);

        // Затем рисуем темный фон ПОД палитрой (но поверх всего остального)
        Color overlayColor = new Color(0, 0, 0, (int)(200 * colorPickerAnimation));
        DrawHelper.drawRect(0, 0, width, height, overlayColor);
    }



    private void drawThemesMenuNew(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, int mouseX, int mouseY, double slideProgress) {
        if (slideProgress <= 0) return;

        List<Theme> themes = ThemeManager.getThemes();
        int currentThemeIndex = ThemeManager.getCurrentThemeIndex();
        currentActiveTheme = currentThemeIndex;

        // Анимация открытия/закрытия палитры
        if (ThemeManager.isColorPickerOpen()) {
            colorPickerAnimation = Math.min(colorPickerAnimation + 0.1f, 1.0);
        } else {
            colorPickerAnimation = Math.max(colorPickerAnimation - 0.1f, 0.0);
        }

        double themeWidth = 150 * scaleFactor;
        double themeHeight = 40 * scaleFactor;
        double baseStartX = themesStartX;
        double baseStartY = themesStartY;

        double slideOffset = 100 * (1 - slideProgress);
        double startX = baseStartX - slideOffset;
        double startY = baseStartY;

        // Заголовок
        String title = "Выбор темы";
        drawThemesTitleWithPicker(matrixStack, menuX, menuY, menuWidth, startY, slideProgress, mouseX, mouseY);

        // Рисуем стандартные темы (немного прозрачными если открыта палитра)
        float themesAlpha = ThemeManager.isColorPickerOpen() ? (float) (1.0 - colorPickerAnimation * 0.7) : 1.0f;

        for (int i = 0; i < themes.size(); i++) {
            Theme theme = themes.get(i);
            double elementAnim = getElementAnimation("Themes", i);
            if (elementAnim <= 0) continue;

            double themeX = startX + (i % 2) * (themeWidth + 20 * scaleFactor);
            double themeY = startY + (i / 2) * (themeHeight + 15 * scaleFactor) - (15 * scaleFactor);

            double elementOffsetY = 10 * (1 - elementAnim);
            double elementAlpha = elementAnim * themesAlpha;
            double animatedY = themeY - elementOffsetY;

            boolean isSelected = i == currentActiveTheme;
            boolean isHovered = mouseX >= themeX && mouseX <= themeX + themeWidth &&
                    mouseY >= animatedY - 19 && mouseY <= animatedY + themeHeight - 19;

            drawSmokeElementBackground(themeX, animatedY, themeWidth, themeHeight, elementAlpha, isSelected, isHovered);

            // Цветной квадратик темы
            double colorBoxSize = 20 * scaleFactor;
            double colorBoxX = themeX + (7 * scaleFactor);
            double colorBoxY = animatedY - (8 * scaleFactor);

            Color[] themeColors = theme.getGradientColors();
            Color[] animatedThemeColors = new Color[]{
                    new Color(themeColors[0].getRed(), themeColors[0].getGreen(), themeColors[0].getBlue(), (int) (255 * elementAlpha)),
                    new Color(themeColors[1].getRed(), themeColors[1].getGreen(), themeColors[1].getBlue(), (int) (255 * elementAlpha)),
                    new Color(themeColors[2].getRed(), themeColors[2].getGreen(), themeColors[2].getBlue(), (int) (255 * elementAlpha))
            };

            DrawHelper.drawRoundedGradientRect(colorBoxX, colorBoxY, colorBoxSize, colorBoxSize, 4 * scaleFactor,
                    animatedThemeColors[0], animatedThemeColors[1], animatedThemeColors[2], animatedThemeColors[0]);

            // Название темы
            if (VertexVisualMod.textFont != null) {
                Color textColor = new Color(255, 255, 255, (int) (255 * elementAlpha));
                StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, theme.getName(),
                        (float) colorBoxX + colorBoxSize + (2 * scaleFactor),
                        (float) colorBoxY - (3 * scaleFactor),
                        textColor);
            }

            // Слайдер активации темы
            double sliderX = themeX + themeWidth - (40 * scaleFactor) - (10 * scaleFactor);
            double sliderY = themeY - (9 * scaleFactor);
            String sliderId = "theme_" + i;

            boolean isThemeActive = i == currentThemeIndex;
            drawModernSlider(matrixStack, sliderX, sliderY, isThemeActive, elementAlpha, mouseX, mouseY, sliderId);

            // ДОБАВЛЯЕМ КНОПКУ ПАЛИТРЫ ТОЛЬКО ДЛЯ ТЕМЫ "МОЯ ТЕМА"
            if (i == ThemeManager.getCustomThemeIndex()) {
                // Кнопка палитры справа от темы
                double pickerButtonSize = 20 * scaleFactor;
                double pickerButtonX = themeX + themeWidth + (5 * scaleFactor);
                double pickerButtonY = themeY - (10 * scaleFactor);

                boolean isPickerHovered = mouseX >= pickerButtonX && mouseX <= pickerButtonX + pickerButtonSize &&
                        mouseY >= pickerButtonY && mouseY <= pickerButtonY + pickerButtonSize;

                // Рисуем кнопку палитры
                Color pickerButtonColor = ThemeManager.isColorPickerOpen() ?
                        new Color(0, 150, 255, (int) (255 * elementAlpha)) :
                        new Color(100, 100, 120, (int) (255 * elementAlpha));

                DrawHelper.drawRoundedRect(pickerButtonX, pickerButtonY, pickerButtonSize, pickerButtonSize, 4 * scaleFactor, pickerButtonColor);

                // Иконка палитры
                if (VertexVisualMod.iconFont2 != null) {
                    Color iconColor = new Color(255, 255, 255, (int) (255 * elementAlpha));
                    IconRenderer.drawCenteredXYIcon(matrixStack, VertexVisualMod.iconFont2, 'N',
                            (float) (pickerButtonX + pickerButtonSize / 2 + (11 * scaleFactor)),
                            (float) (pickerButtonY + pickerButtonSize / 2 - (21 * scaleFactor)),
                            iconColor);
                }
            }
        }
    }

    private void drawCustomThemeWithPicker(MatrixStack matrixStack, double startX, double startY, double themeWidth, double themeHeight, int themesCount, int mouseX, int mouseY, double slideProgress, float alpha) {
        // Позиция кнопки "Моя тема" - под всеми темами
        int themesPerColumn = (int) Math.ceil(themesCount / 2.0);
        double customThemeX = startX;
        double customThemeY = startY + (themesPerColumn * (themeHeight + 15 * scaleFactor));

        double elementAnim = getElementAnimation("Themes", themesCount);
        if (elementAnim <= 0) return;

        double elementOffsetY = 10 * (1 - elementAnim);
        double elementAlpha = elementAnim * alpha;
        double animatedY = customThemeY - elementOffsetY;

        // Рисуем кнопку "Моя тема"
        boolean isCustomHovered = mouseX >= customThemeX && mouseX <= customThemeX + themeWidth &&
                mouseY >= animatedY && mouseY <= animatedY + themeHeight;

        drawSmokeElementBackground(customThemeX, animatedY, themeWidth, themeHeight, elementAlpha, false, isCustomHovered);

        // Превью кастомной темы
        double previewSize = 20 * scaleFactor;
        double previewX = customThemeX + (10 * scaleFactor);
        double previewY = animatedY + (10 * scaleFactor);

        DrawHelper.drawRoundedGradientRect(previewX, previewY, previewSize, previewSize, 4 * scaleFactor,
                ThemeManager.getCurrentEditingColor1(),
                ThemeManager.getCurrentEditingColor2(),
                ThemeManager.getCurrentEditingColor3(),
                ThemeManager.getCurrentEditingColor1());

        // Текст "Моя тема"
        if (VertexVisualMod.textFont != null) {
            Color textColor = new Color(255, 255, 255, (int) (255 * elementAlpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, "Моя тема",
                    (float) previewX + previewSize + (5 * scaleFactor),
                    (float) previewY + (5 * scaleFactor),
                    textColor);
        }

        // КНОПКА ПАЛИТРЫ СПРАВА ОТ "МОЕЙ ТЕМЫ"
        double pickerButtonSize = 25 * scaleFactor;
        double pickerButtonX = customThemeX + themeWidth + (10 * scaleFactor); // Справа от кнопки "Моя тема"
        double pickerButtonY = animatedY + (themeHeight - pickerButtonSize) / 2;

        boolean isPickerHovered = mouseX >= pickerButtonX && mouseX <= pickerButtonX + pickerButtonSize &&
                mouseY >= pickerButtonY && mouseY <= pickerButtonY + pickerButtonSize;

        // Рисуем кнопку палитры
        Color pickerButtonColor = ThemeManager.isColorPickerOpen() ?
                new Color(0, 150, 255, (int) (255 * elementAlpha)) :
                new Color(100, 100, 120, (int) (255 * elementAlpha));

        DrawHelper.drawRoundedRect(pickerButtonX, pickerButtonY, pickerButtonSize, pickerButtonSize, 5 * scaleFactor, pickerButtonColor);

        // Слайдер для кастомной темы
        double sliderX = customThemeX + themeWidth - (40 * scaleFactor) - (10 * scaleFactor);
        double sliderY = customThemeY + (10 * scaleFactor);
        boolean isCustomActive = ThemeManager.isCustomThemeActive();
        drawModernSlider(matrixStack, sliderX, sliderY, isCustomActive, elementAlpha, mouseX, mouseY, "theme_custom");
    }

    private void drawThemesTitle(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double startY, double slideProgress) {
        float titleY = (float) (startY - (100 * scaleFactor));
        float titleAlpha = (float) slideProgress;

        String title = "Выбор темы";
        float titleWidth = VertexVisualMod.textFont != null ?
                VertexVisualMod.textFont.getWidth(title) * scaleFactor :
                mc.font.width(title) * scaleFactor;
        float titleX = (float) (menuX + (200 * scaleFactor));

        // Рисуем заголовок
        if (VertexVisualMod.textFont != null) {
            Color titleColor = new Color(255, 255, 255, (int) (255 * titleAlpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, title, titleX, titleY, titleColor);
        }
    }

    private void drawThemesTitleWithPicker(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double startY, double slideProgress, int mouseX, int mouseY) {
        float titleY = (float) (startY - (60 * scaleFactor));
        float titleAlpha = (float) slideProgress;

        String title = "Выбор темы";
        float titleWidth = VertexVisualMod.textFont != null ?
                VertexVisualMod.textFont.getWidth(title) * scaleFactor :
                mc.font.width(title) * scaleFactor;

        // ВОЗВРАЩАЕМ правильную позицию заголовка (как было в drawThemesTitle)
        float titleX = (float) (menuX + (210 * scaleFactor));

        // Рисуем только заголовок (без кнопки палитры)
        if (VertexVisualMod.textFont != null) {
            Color titleColor = new Color(255, 255, 255, (int) (255 * titleAlpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, title, titleX, titleY, titleColor);
        } else {
            // fallback на стандартный шрифт
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, title, titleX / scaleFactor, titleY / scaleFactor,
                    new Color(255, 255, 255, (int) (255 * titleAlpha)).getRGB());
            matrixStack.popPose();
        }
    }

    private void drawCustomThemeButton(MatrixStack matrixStack, double x, double y, double width, double height, int mouseX, int mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        drawSmokeElementBackground(x, y, width, height, alpha, false, isHovered);

        // Превью кастомной темы
        double previewSize = 20 * scaleFactor;
        double previewX = x + (10 * scaleFactor);
        double previewY = y + (10 * scaleFactor);

        DrawHelper.drawRoundedGradientRect(previewX, previewY, previewSize, previewSize, 4 * scaleFactor,
                ThemeManager.getCurrentEditingColor1(),
                ThemeManager.getCurrentEditingColor2(),
                ThemeManager.getCurrentEditingColor3(),
                ThemeManager.getCurrentEditingColor1());

        // Текст
        if (VertexVisualMod.textFont != null) {
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, "Моя тема",
                    (float) previewX + previewSize + (5 * scaleFactor),
                    (float) previewY + (5 * scaleFactor),
                    textColor);
        }
    }

    private void drawColorPicker(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, int mouseX, int mouseY, double animation) {
        double pickerWidth = 400 * scaleFactor;
        double pickerHeight = 350 * scaleFactor;
        double pickerX = menuX;
        double pickerY = menuY;

        // Фон палитры с анимацией
        Color bgColor = new Color(20, 20, 30, (int) (230 * animation));
        DrawHelper.drawRoundedRect(pickerX, pickerY, pickerWidth, pickerHeight, 15 * scaleFactor, bgColor);

        // Заголовок
        String title = "Палитра цветов";
        if (VertexVisualMod.textFont != null) {
            Color titleColor = new Color(255, 255, 255, (int) (255 * animation));
            drawCenteredText(matrixStack, title, pickerX - 10, pickerY - 320 * scaleFactor, pickerWidth, titleColor);
        }

        // Круговая палитра - УБРАЛИ ДОПОЛНИТЕЛЬНОЕ СМЕЩЕНИЕ
        double colorWheelSize = 200 * scaleFactor;
        double colorWheelX = pickerX + (pickerWidth - colorWheelSize) / 2;
        double colorWheelY = pickerY - 300 * scaleFactor; // ← нормальная позиция

       drawColorWheel(matrixStack, colorWheelX, colorWheelY, colorWheelSize, mouseX, mouseY, animation);

        // Ползунок яркости
        double brightnessSliderWidth = 180 * scaleFactor;
        double brightnessSliderHeight = 20 * scaleFactor;
        double brightnessSliderX = colorWheelX + (colorWheelSize - brightnessSliderWidth) / 2;
        double brightnessSliderY = colorWheelY + colorWheelSize + 60 * scaleFactor;

        drawBrightnessSlider(matrixStack, brightnessSliderX, brightnessSliderY, brightnessSliderWidth, brightnessSliderHeight, mouseX, mouseY, animation);

        // Превью выбранного цвета
        double previewSize = 40 * scaleFactor;
        double previewX = brightnessSliderX + brightnessSliderWidth + 15 * scaleFactor;
        double previewY = brightnessSliderY;

        DrawHelper.drawRoundedRect(previewX, previewY, previewSize, previewSize, 5 * scaleFactor, ThemeManager.getCurrentSelectedColor());
        DrawHelper.drawRoundedRectOutline(previewX, previewY, previewSize, previewSize, 5 * scaleFactor, 2 * scaleFactor, new Color(255, 255, 255, (int) (200 * animation)));

        // Кнопки - ПОДНЯЛИ ВЫШЕ
        double buttonWidth = 100 * scaleFactor;
        double buttonHeight = 30 * scaleFactor;
        double buttonsY = brightnessSliderY + brightnessSliderHeight + 20 * scaleFactor;
        double buttonsStartX = pickerX + (pickerWidth - buttonWidth * 2 - 5 * scaleFactor) / 2;

        // Кнопка применения (новый стиль)
        double applyButtonX = buttonsStartX;
        boolean isApplyHovered = mouseX >= applyButtonX && mouseX <= applyButtonX + buttonWidth &&
                mouseY >= buttonsY && mouseY <= buttonsY + buttonHeight;
        boolean isApplyPressed = false;

        drawStyledButton(matrixStack, applyButtonX, buttonsY, buttonWidth, buttonHeight,
                "Применить", isApplyHovered, isApplyPressed, animation);

        // Кнопка закрытия (новый стиль)
        double closeButtonX = applyButtonX + buttonWidth + 10 * scaleFactor;
        boolean isCloseHovered = mouseX >= closeButtonX && mouseX <= closeButtonX + buttonWidth &&
                mouseY >= buttonsY && mouseY <= buttonsY + buttonHeight;
        boolean isClosePressed = false;

        drawStyledButton(matrixStack, closeButtonX, buttonsY, buttonWidth, buttonHeight,
                "Закрыть", isCloseHovered, isClosePressed, animation);
    }

    private void drawStyledButton(MatrixStack matrixStack, double x, double y, double width, double height,
                                  String text, boolean isHovered, boolean isPressed, double animation) {

        // Плавная анимация затемнения/осветления
        long currentTime = System.currentTimeMillis();
        String buttonId = "button_" + text;

        // Инициализируем или получаем время начала анимации
        if (!modernSliderAnimationTimes.containsKey(buttonId)) {
            modernSliderAnimationTimes.put(buttonId, currentTime);
        }

        long startTime = modernSliderAnimationTimes.get(buttonId);
        long elapsed = currentTime - startTime;
        double animProgress = Math.min(1.0, elapsed / 300.0);

        // Если состояние изменилось, сбрасываем анимацию
        String currentState = buttonId + "_" + isHovered;
        if (!modernSliderAnimationTimes.containsKey(currentState)) {
            modernSliderAnimationTimes.put(currentState, currentTime);
            modernSliderAnimationTimes.put(buttonId, currentTime);
        }

        // ЗЕЛЕНЫЕ ЦВЕТА ДЛЯ КНОПКИ "ПРИМЕНИТЬ", СЕРЫЕ ДЛЯ "ЗАКРЫТЬ"
        Color normalColor, hoverColor;

        if (text.equals("Применить")) {
            normalColor = new Color(0, 128, 0, (int)(200 * animation)); // Зеленый обычно
            hoverColor = new Color(0, 180, 0, (int)(200 * animation));   // Ярко-зеленый при наведении
        } else {
            normalColor = new Color(128, 128, 128, (int)(200 * animation)); // Серый обычно
            hoverColor = new Color(80, 80, 80, (int)(200 * animation));     // Темно-серый при наведении
        }

        // Плавный переход между цветами
        Color backgroundColor;
        if (isHovered) {
            backgroundColor = interpolateColor(normalColor, hoverColor, animProgress);
        } else {
            backgroundColor = interpolateColor(hoverColor, normalColor, animProgress);
        }

        Color borderColor = new Color(0, 0, 0, (int)(255 * animation));
        Color textColor = new Color(255, 255, 255, (int)(255 * animation));

        // Фон кнопки с закругленными углами
        DrawHelper.drawRoundedRect(x, y, width, height, 15 * scaleFactor, backgroundColor);

        // Черная обводка
        DrawHelper.drawRoundedRectOutline(x, y, width, height, 15 * scaleFactor,
                2.0f * scaleFactor, borderColor);

        // ПОДСВЕТКА - ЗЕЛЕНАЯ ДЛЯ "ПРИМЕНИТЬ", БЕЛАЯ ДЛЯ "ЗАКРЫТЬ"
        if (isHovered) {
            Color glowColor;
            if (text.equals("Применить")) {
                glowColor = new Color(0, 255, 0, (int)(50 * animProgress * animation)); // Зеленая подсветка
            } else {
                glowColor = new Color(255, 255, 255, (int)(30 * animProgress * animation)); // Белая подсветка
            }

            DrawHelper.drawRoundedBlurredRect(
                    x - 3 * scaleFactor,
                    y - 3 * scaleFactor,
                    width + 6 * scaleFactor,
                    height + 6 * scaleFactor,
                    18 * scaleFactor,
                    4 * scaleFactor,
                    glowColor
            );
        }

        // Легкая анимация масштаба при наведении
        double scale = isHovered ? 0.95 : 1.0;
        double scaledWidth = width * scale;
        double scaledHeight = height * scale;
        double scaledX = x + (width - scaledWidth) / 2;
        double scaledY = y + (height - scaledHeight) / 2;

        // Текст с ФИКСИРОВАННОЙ позицией
        if (VertexVisualMod.textFont != null) {
            float textWidth = VertexVisualMod.textFont.getWidth(text) * scaleFactor;

            float textX = (float)(scaledX + (width - textWidth) / 2);
            float textXOffset = - 13 * scaleFactor;
            textX += textXOffset;

            // ФИКСИРОВАННАЯ позиция по Y
            float textYOffset = - 16.5f * scaleFactor;
            float textY = (float)(scaledY + (height - VertexVisualMod.textFont.getFontHeight() * scaleFactor) / 2 + textYOffset);

            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, text, textX, textY, textColor);
        } else {
            float textWidth = mc.font.width(text) * scaleFactor;

            // Для стандартного шрифта тоже фиксированная позиция
            float textX = (float)(scaledX + (width - textWidth) / 2); // используем исходную width

            float textYOffset = 2 * scaleFactor;
            float textY = (float)(scaledY + (height - mc.font.lineHeight * scaleFactor) / 2 + textYOffset);

            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, text, textX / scaleFactor, textY / scaleFactor, textColor.getRGB());
            matrixStack.popPose();
        }

        // Эффект легкого нажатия
        if (isPressed) {
            Color pressEffect = new Color(0, 0, 0, (int)(80 * animation));
            DrawHelper.drawRoundedRect(x, y, width, height, 15 * scaleFactor, pressEffect);
        }
    }

    private Texture colorWheelTexture = null;
    private boolean needsColorWheelUpdate = true;

    private void drawColorWheel(MatrixStack matrixStack, double x, double y, double size, int mouseX, int mouseY, double animation) {
        double centerX = x + size / 2;
        double centerY = y + size / 2;
        double radius = size / 2;

        int angleStep = 2;
        int radiusStep = 2;

        for (int angle = 0; angle < 360; angle += angleStep) {
            for (int r = 0; r < radius; r += radiusStep) {
                double progress = (double) r / radius;
                double rad = Math.toRadians(angle);

                double pointX = centerX + r * Math.cos(rad);
                double pointY = centerY + r * Math.sin(rad);

                Color color = getColorFromWheel(angle, progress);
                // УВЕЛИЧИВАЕМ размер точек до 3x3 чтобы закрыть пробелы
                DrawHelper.drawRect(pointX, pointY, 3, 3, color);
            }
        }

        // Маркер выбранного цвета
        if (ThemeManager.isDraggingColorWheel() || colorPickerAnimation > 0) {
            double markerRadius = 8 * scaleFactor;
            double currentAngle = ThemeManager.getColorWheelAngle();
            double currentRadius = ThemeManager.getColorWheelRadius();

            double markerX = centerX + currentRadius * radius * Math.cos(Math.toRadians(currentAngle)) - markerRadius / 2;
            double markerY = centerY + currentRadius * radius * Math.sin(Math.toRadians(currentAngle)) - markerRadius / 2;

            DrawHelper.drawRoundedRect(markerX, markerY, markerRadius, markerRadius, markerRadius / 2,
                    new Color(255, 255, 255, (int)(255 * animation)));
            DrawHelper.drawRoundedRectOutline(markerX, markerY, markerRadius, markerRadius, markerRadius / 2,
                    1 * scaleFactor, new Color(0, 0, 0, (int)(255 * animation)));
        }
    }

    private void drawColorWheelFallback(MatrixStack matrixStack, double x, double y, double size, int mouseX, int mouseY, double animation) {
        double centerX = x + size / 2;
        double centerY = y + size / 2;
        double radius = size / 2;

        // ПЛОТНАЯ отрисовка без пробелов
        for (int angle = 0; angle < 360; angle += 4) {
            for (int r = 0; r < radius; r += 4) {
                double progress = (double) r / radius;
                double rad = Math.toRadians(angle);

                double pointX = centerX + r * Math.cos(rad);
                double pointY = centerY + r * Math.sin(rad);

                Color color = getColorFromWheel(angle, progress);
                DrawHelper.drawRect(pointX, pointY, 4, 4, color);
            }
        }
    }

    // Метод для обновления текстуры при изменении яркости
    public void onBrightnessChanged() {
        needsColorWheelUpdate = true;
    }

    private void drawModernButton(MatrixStack matrixStack, double x, double y, double width, double height,
                                  String text, boolean isHovered, boolean isPressed, double animation) {

        // Основные цвета
        Color borderColor = new Color(61, 106, 255, (int)(255 * animation));
        Color backgroundColor = isHovered ?
                new Color(61, 106, 255, (int)(200 * animation)) :
                new Color(0, 0, 0, 0);
        Color textColor = new Color(255, 255, 255, (int)(255 * animation));

        // Рисуем фон (прозрачный или синий при наведении)
        if (isHovered) {
            DrawHelper.drawRoundedRect(x, y, width, height, 7 * scaleFactor, backgroundColor);
        }

        // Рисуем обводку
        DrawHelper.drawRoundedRectOutline(x, y, width, height, 7 * scaleFactor,
                1.0f * scaleFactor, borderColor);

        // Эффект свечения при наведении
        if (isHovered && !isPressed) {
            Color glowColor = new Color(0, 142, 236, (int)(100 * animation));
            // DrawHelper.drawRoundedBlurredRect(
                    // x - 5 * scaleFactor,
                   //  y - 5 * scaleFactor,
                   //  width + 10 * scaleFactor,
                    // height + 10 * scaleFactor,
                   //  10 * scaleFactor,
                   //  8 * scaleFactor,
                   //  glowColor
           //  );
        }

        // Анимация белой полосы (упрощенная версия)
        if (isHovered) {
            long time = System.currentTimeMillis() % 1000;
            double progress = time / 1000.0;
            double stripeWidth = 20 * scaleFactor;
            double stripeX = x - stripeWidth + (width + stripeWidth) * progress;

            Color stripeColor = new Color(255, 255, 255, (int)(80 * animation));
            DrawHelper.drawRoundedRect(
                    stripeX, y + height * 0.07,
                    stripeWidth, height * 0.86,
                    3 * scaleFactor, stripeColor
            );
        }

        // Рисуем текст
        if (VertexVisualMod.textFont != null) {
            float textWidth = VertexVisualMod.textFont.getWidth(text) * scaleFactor;
            float textX = (float)(x + (width - textWidth) / 2);
            float textY = (float)(y + (height - VertexVisualMod.textFont.getFontHeight() * scaleFactor) / 2);

            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, text, textX, textY, textColor);
        } else {
            // Fallback на стандартный шрифт
            float textWidth = mc.font.width(text) * scaleFactor;
            float textX = (float)(x + (width - textWidth) / 2);
            float textY = (float)(y + (height - mc.font.lineHeight * scaleFactor) / 2);

            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, text, textX / scaleFactor, textY / scaleFactor, textColor.getRGB());
            matrixStack.popPose();
        }

        // Эффект нажатия
        if (isPressed) {
            Color pressEffect = new Color(61, 106, 255, (int)(100 * animation));
            DrawHelper.drawRoundedRect(x, y, width, height, 7 * scaleFactor, pressEffect);
        }
    }

    private void drawBrightnessSlider(MatrixStack matrixStack, double x, double y, double width, double height, int mouseX, int mouseY, double animation) {
        // Фон ползунка - градиент от черного к белому
        for (double i = 0; i < width; i += 1) {
            double progress = i / width;
            int brightness = (int) (255 * progress);
            Color color = new Color(brightness, brightness, brightness);
            DrawHelper.drawRect(x + i, y, 1, height, color);
        }

        // Рамка
        DrawHelper.drawRoundedRectOutline(x, y, width, height, height / 2, 1 * scaleFactor, new Color(200, 200, 200, (int) (200 * animation)));

        // Ползунок
        double sliderPos = x + width * ThemeManager.getBrightnessValue();
        double sliderSize = 14 * scaleFactor;
        double sliderY = y + (height - sliderSize) / 2 - 5.5 * scaleFactor;

        DrawHelper.drawRoundedRect(sliderPos - sliderSize / 2, sliderY, sliderSize, sliderSize, sliderSize / 2, new Color(255, 255, 255, (int) (255 * animation)));
        DrawHelper.drawRoundedRectOutline(sliderPos - sliderSize / 2, sliderY, sliderSize, sliderSize, sliderSize / 2, 1 * scaleFactor, new Color(0, 0, 0, (int) (255 * animation)));

        // ПРИ ИЗМЕНЕНИИ ПОЗИЦИИ ПОЛЗУНКА - ОБНОВЛЯЕМ ФЛАГ
        if (ThemeManager.isDraggingBrightness()) {
            needsColorWheelUpdate = true;
        }
    }

    private void drawCenteredText(MatrixStack matrixStack, String text, double x, double y, double areaWidth, Color color) {
        if (VertexVisualMod.textFont != null) {
            float textWidth = VertexVisualMod.textFont.getWidth(text) * scaleFactor;
            float centeredX = (float) (x + (areaWidth - textWidth) / 2);
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, text, centeredX, (float) y, color);
        } else {
            // fallback на стандартный шрифт
            float textWidth = mc.font.width(text) * scaleFactor;
            float centeredX = (float) (x + (areaWidth - textWidth) / 2);
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, text, centeredX / scaleFactor, (float) y / scaleFactor, color.getRGB());
            matrixStack.popPose();
        }
    }

    private Color getColorFromWheel(double angle, double saturation) {
        // Используем текущее значение яркости из ThemeManager
        float hue = (float) (angle / 360.0);
        float sat = (float) saturation;
        float bright = (float) ThemeManager.getBrightnessValue();

        return Color.getHSBColor(hue, sat, bright);
    }

    private void drawWorldMenuNew(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, int mouseX, int mouseY, double slideProgress) {
        if (slideProgress <= 0) return;

        // Инициализируем время анимации для слайдера
        worldSliderAnimationTimes.putIfAbsent(0, System.currentTimeMillis());

        double worldOptionWidth = 250 * scaleFactor;
        double worldOptionHeight = 60 * scaleFactor;
        double baseStartX = themesStartX;
        double baseStartY = themesStartY;

        // Анимация скольжения слева
        double slideOffset = 100 * (1 - slideProgress);
        double startX = baseStartX - slideOffset;
        double startY = baseStartY;

        // Заголовок
        String title = "Настройки мира";
        drawAnimatedTitle(matrixStack, title, menuX, menuY, menuWidth, startY, slideProgress);

        // Опция "Цвет неба"
        double elementAnim = getElementAnimation("World", 0);
        if (elementAnim > 0) {
            double optionY = startY;
            double elementOffsetY = 10 * (1 - elementAnim);
            double elementAlpha = elementAnim;
            double animatedY = optionY - elementOffsetY;

            drawWorldOptionNew(matrixStack, "Цвет неба", skyColorEnabled, startX, animatedY, worldOptionWidth, worldOptionHeight, mouseX, mouseY, (float) elementAlpha);
        }
    }

    private void drawMovementMenuNew(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, int mouseX, int mouseY, double slideProgress) {
        if (slideProgress <= 0) return;

        double movementOptionWidth = 250 * scaleFactor;
        double movementOptionHeight = 60 * scaleFactor;
        double baseStartX = themesStartX;
        double baseStartY = themesStartY;

        // Анимация скольжения слева
        double slideOffset = 100 * (1 - slideProgress);
        double startX = baseStartX - slideOffset;
        double startY = baseStartY;

        // Заголовок
        String title = "Настройки движения";
        drawAnimatedTitle(matrixStack, title, menuX, menuY, menuWidth, startY, slideProgress);

        // Примерные настройки движения (можно добавить реальные позже)
        String[] movementOptions = {"Ускорение", "Высота прыжка", "Скорость полета"};
        boolean[] movementStates = {true, false, true}; // примерные состояния

        for (int i = 0; i < movementOptions.length; i++) {
            double elementAnim = getElementAnimation("Movement", i);
            if (elementAnim <= 0) continue;

            double optionY = startY + i * (movementOptionHeight + 10 * scaleFactor);
            double elementOffsetY = 10 * (1 - elementAnim);
            double elementAlpha = elementAnim;
            double animatedY = optionY - elementOffsetY;

            drawMovementOption(matrixStack, movementOptions[i], movementStates[i], startX, animatedY, movementOptionWidth, movementOptionHeight, mouseX, mouseY, (float) elementAlpha);
        }
    }

    private void drawSettingsMenuNew(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, int mouseX, int mouseY, double slideProgress) {
        if (slideProgress <= 0) return;

        double settingsOptionWidth = 200 * scaleFactor;
        double settingsOptionHeight = 40 * scaleFactor;

        double baseStartX = themesStartX;
        double baseStartY = themesStartY;

        // Анимация скольжения слева
        double slideOffset = 100 * (1 - slideProgress);
        double startX = baseStartX - slideOffset;
        double startY = baseStartY;
        // Заголовок
        String title = "Настройки";
        drawAnimatedTitle(matrixStack, title, menuX, menuY, menuWidth, startY, slideProgress);

        // Получаем все настройки
        Map<String, Boolean> settings = settingsManager.getAllBooleanSettings();
        String[] settingKeys = {
                SettingsManager.REMOVE_EXTERNAL_GLOW,
                SettingsManager.HIDE_UID,
                SettingsManager.STREAMER_MODE,
                SettingsManager.DISABLE_ALL_MODS
        };

        String[] settingNames = {
                "Убрать свечение",
                "Скрыть UID",
                "Режим стримера",
                "Отключить все"
        };

        for (int i = 0; i < settingKeys.length; i++) {
            double elementAnim = getElementAnimation("Settings", i);
            if (elementAnim <= 0) continue;

            // ИСПРАВЛЕННЫЕ КООРДИНАТЫ ДЛЯ КАЖДОЙ ОПЦИИ
            double optionY = startY + i * (settingsOptionHeight + 15 * scaleFactor);
            double elementOffsetY = 10 * (1 - elementAnim);
            double elementAlpha = elementAnim;
            double animatedSettingsY = optionY - elementOffsetY; // Переименовали здесь

            boolean isEnabled = settings.get(settingKeys[i]);

            // Сохраняем координаты для обработки кликов
            if (settingKeys[i].equals(SettingsManager.REMOVE_EXTERNAL_GLOW)) {
                // Запоминаем координаты для отладки
                movementClickX = startX;
                movementClickY = animatedSettingsY;
            }

            drawSettingsOption(matrixStack, settingNames[i], isEnabled, startX, animatedSettingsY,
                    settingsOptionWidth, settingsOptionHeight, mouseX, mouseY, (float) elementAlpha, settingKeys[i]);
        }
    }

    private void drawSettingsOption(MatrixStack matrixStack, String optionName, boolean isEnabled,
                                    double x, double y, double width, double height,
                                    int mouseX, int mouseY, float alpha, String settingKey) {
        boolean isHovered = mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;

        // Фон опции с дымовым эффектом
        drawSmokeElementBackground(x, y, width, height, alpha, false, isHovered);

        // Проверяем, содержит ли текст перенос строки
        String[] lines = optionName.split("\n");

        if (VertexVisualMod.textFont != null) {
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));

            // Рисуем первую строку
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, lines[0],
                    (float) x + (15 * scaleFactor),
                    (float) y + (15 * scaleFactor),
                    textColor);

            // Если есть вторая строка, рисуем ее
            if (lines.length > 1) {
                StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, lines[1],
                        (float) x + (15 * scaleFactor),
                        (float) y + (30 * scaleFactor),
                        textColor);
            }
        } else {
            // Fallback для стандартного шрифта
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);

            float textY = (float) (y + (15 * scaleFactor)) / scaleFactor;
            for (String line : lines) {
                mc.font.drawShadow(matrixStack, line,
                        (float) (x + (15 * scaleFactor)) / scaleFactor,
                        textY,
                        new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
                textY += mc.font.lineHeight + 2;
            }
            matrixStack.popPose();
        }

        // Описание настройки (меньшим шрифтом)
        String description = getSettingDescription(settingKey);
        if (VertexVisualMod.textFont != null && description != null) {
            Color descColor = new Color(180, 180, 180, (int) (200 * alpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, description,
                    (float) x + (15 * scaleFactor),
                    (float) y + (45 * scaleFactor),
                    descColor);
        }

        // Слайдер с анимацией - ТОЧНО КАК В THEMES
        double sliderWidth = 40 * scaleFactor;
        double sliderHeight = 20 * scaleFactor;
        double sliderX = x + width - sliderWidth - (10 * scaleFactor);
        double sliderY = y + (height - sliderHeight) / 2 - (10 * scaleFactor);
        drawModernSlider(matrixStack, sliderX, sliderY, isEnabled, alpha, mouseX, mouseY, "settings_" + settingKey);
    }

    private String getSettingDescription(String settingKey) {
        switch (settingKey) {
            case SettingsManager.REMOVE_EXTERNAL_GLOW:
                return "";
            case SettingsManager.HIDE_UID:
                return "";
            case SettingsManager.STREAMER_MODE:
                return "";
            case SettingsManager.DISABLE_ALL_MODS:
                return "";
            default:
                return null;
        }
    }

    private void updateElementAnimationsOnClose(String menu, float partialTicks) {
        List<Double> anims = elementAnimations.get(menu);
        if (anims == null) return;

        double slideProgress = menuSlideAnimations.getOrDefault(menu, 0.0);

        for (int i = 0; i < anims.size(); i++) {
            double baseDelay = 0.1;
            double elementDelay = baseDelay * i;
            double targetProgress = Math.max(0, Math.min(1, (slideProgress - elementDelay) / (1 - elementDelay)));

            double currentAnim = anims.get(i);
            if (targetProgress < currentAnim) {
                double newAnim = Math.max(currentAnim - partialTicks * 0.4f, targetProgress);
                anims.set(i, newAnim);
            }
        }
    }

    private int getElementCountForMenu(String menu) {
        switch (menu) {
            case "Themes":
                return ThemeManager.getThemes().size() + 1;
            case "World":
                return 1;
            case "Movement":
                return 3;
            case "Settings":
                return 4; // Теперь 4 настройки
            default:
                return 1;
        }
    }

    private void drawWorldOptionNew(MatrixStack matrixStack, String optionName, boolean isEnabled, double x, double y, double width, double height, int mouseX, int mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        // Фон опции с дымовым эффектом
        drawSmokeElementBackground(x, y, width, height, alpha, false, isHovered);

        // Текст опции с анимацией
        if (VertexVisualMod.textFont != null) {
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, optionName,
                    (float) x + (15 * scaleFactor),
                    (float) y + (20 * scaleFactor),
                    textColor);
        } else {
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, optionName,
                    (float) (x + (15 * scaleFactor)) / scaleFactor,
                    (float) (y + (20 * scaleFactor)) / scaleFactor,
                    new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
            matrixStack.popPose();
        }

        // Слайдер с анимацией
        double sliderX = x + width - (40 * scaleFactor) - (15 * scaleFactor);
        double sliderY = y + (height - (20 * scaleFactor)) / 2;
        drawModernSlider(matrixStack, sliderX, sliderY, isEnabled, alpha, mouseX, mouseY, "world_skycolor");

        // Предпросмотр цвета неба (только если включено)
        if (isEnabled) {
            double previewSize = 25 * scaleFactor;
            double previewX = x + width - previewSize - (60 * scaleFactor);
            double previewY = y + (height - previewSize) / 2;

            Color skyPreviewColor = SkyColorManager.getFogColor();
            Color previewColor = new Color(
                    skyPreviewColor.getRed(),
                    skyPreviewColor.getGreen(),
                    skyPreviewColor.getBlue(),
                    (int) (255 * alpha)
            );

            DrawHelper.drawRoundedRect(previewX, previewY, previewSize, previewSize, 5 * scaleFactor, previewColor);
            DrawHelper.drawRoundedRectOutline(previewX, previewY, previewSize, previewSize,
                    5 * scaleFactor, 1.5f * scaleFactor, new Color(255, 255, 255, (int) (150 * alpha)));
        }
    }

    private void drawMovementOption(MatrixStack matrixStack, String optionName, boolean isEnabled, double x, double y, double width, double height, int mouseX, int mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        // Фон опции с дымовым эффектом
        drawSmokeElementBackground(x, y, width, height, alpha, false, isHovered);

        // Текст опции с анимацией
        if (VertexVisualMod.textFont != null) {
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, optionName,
                    (float) x + (15 * scaleFactor),
                    (float) y + (20 * scaleFactor),
                    textColor);
        } else {
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, optionName,
                    (float) (x + (15 * scaleFactor)) / scaleFactor,
                    (float) (y + (20 * scaleFactor)) / scaleFactor,
                    new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
            matrixStack.popPose();
        }

        // Слайдер с анимацией
        double sliderX = x + width - (40 * scaleFactor) - (15 * scaleFactor);
        double sliderY = y + (height - (20 * scaleFactor)) / 2;
        String sliderId = "movement_" + optionName; // Уникальный ID для каждой опции движения
        drawModernSlider(matrixStack, sliderX, sliderY, isEnabled, alpha, mouseX, mouseY, sliderId);
    }

    private void drawSettingsOptionNew(MatrixStack matrixStack, String optionName, boolean isEnabled, double x, double y, double width, double height, int mouseX, int mouseY, float alpha) {
        boolean isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        // Фон опции с дымовым эффектом
        drawSmokeElementBackground(x, y, width, height, alpha, false, isHovered);

        // Текст опции с анимацией
        if (VertexVisualMod.textFont != null) {
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, optionName,
                    (float) x + (15 * scaleFactor),
                    (float) y + (20 * scaleFactor),
                    textColor);
        } else {
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, optionName,
                    (float) (x + (15 * scaleFactor)) / scaleFactor,
                    (float) (y + (20 * scaleFactor)) / scaleFactor,
                    new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
            matrixStack.popPose();
        }

        // Слайдер с анимацией
        double sliderX = x + width - (40 * scaleFactor) - (15 * scaleFactor);
        double sliderY = y + (height - (20 * scaleFactor)) / 2;
        drawModernSlider(matrixStack, sliderX, sliderY, isEnabled, alpha, mouseX, mouseY, "settings_watermark");
    }

    private void drawModernSlider(MatrixStack matrixStack, double x, double y, boolean isEnabled, double alpha, int mouseX, int mouseY, String sliderId) {
        double sliderWidth = 40 * scaleFactor;
        double sliderHeight = 20 * scaleFactor;

        boolean isHovered = mouseX >= x && mouseX <= x + sliderWidth &&
                mouseY >= y && mouseY <= y + sliderHeight;

        // Плавная анимация цвета фона
        double animationProgress = getModernSliderAnimationProgress(sliderId, isEnabled);

        // Цвета для состояний
        Color enabledColor = new Color(0, 123, 255, (int) (255 * alpha)); // Голубой когда включен
        Color disabledColor = new Color(173, 181, 189, (int) (255 * alpha)); // Серый когда выключен

        // Цвета границы
        Color enabledBorder = new Color(0, 100, 200, (int) (255 * alpha));
        Color disabledBorder = new Color(140, 140, 140, (int) (255 * alpha));

        // Интерполяция цветов
        Color sliderBgColor;
        Color borderColor;

        if (isEnabled) {
            // Анимация от серого к голубому при включении
            sliderBgColor = interpolateColor(disabledColor, enabledColor, animationProgress);
            borderColor = interpolateColor(disabledBorder, enabledBorder, animationProgress);
        } else {
            // Анимация от голубого к серому при выключении
            sliderBgColor = interpolateColor(enabledColor, disabledColor, animationProgress);
            borderColor = interpolateColor(enabledBorder, disabledBorder, animationProgress);
        }

        // Рисуем фон слайдера
        DrawHelper.drawRoundedRect(x, y, sliderWidth, sliderHeight, sliderHeight / 2, sliderBgColor);

        // Рисуем границу
        DrawHelper.drawRoundedRectOutline(x, y, sliderWidth, sliderHeight,
                sliderHeight / 2, 1.0f * scaleFactor, borderColor);

        // Кружок слайдера
        double circleSize = 16 * scaleFactor;
        double circleY = y + (sliderHeight - circleSize) / 2 - (4 * scaleFactor);

        // Позиция кружка с анимацией - ИСПРАВЛЕНИЕ ЗДЕСЬ
        double minCircleX = x + (2 * scaleFactor);
        double maxCircleX = x + sliderWidth - circleSize - (2 * scaleFactor);

        // Ключевое исправление: позиция кружка зависит от состояния isEnabled
        double circleX;
        if (isEnabled) {
            // При включении: кружок идет слева направо
            circleX = minCircleX + (maxCircleX - minCircleX) * animationProgress;
        } else {
            // При выключении: кружок идет справа налево
            circleX = maxCircleX - (maxCircleX - minCircleX) * animationProgress;
        }

        // Цвет кружка - белый когда включен, серый когда выключен
        Color circleEnabled = new Color(255, 255, 255, (int) (255 * alpha));
        Color circleDisabled = new Color(220, 220, 220, (int) (255 * alpha));

        Color circleColor = isEnabled ?
                interpolateColor(circleDisabled, circleEnabled, animationProgress) :
                interpolateColor(circleEnabled, circleDisabled, animationProgress);

        // Рисуем кружок
        DrawHelper.drawRoundedRect(circleX, circleY, circleSize, circleSize, circleSize / 2, circleColor);

        // Эффект фокуса как в CSS
        if (isHovered) {
            Color glowColor = new Color(0, 123, 255, (int) (30 * alpha * animationProgress));
            DrawHelper.drawRoundedBlurredRect(
                    x - (1 * scaleFactor),
                    y - (1 * scaleFactor),
                    sliderWidth + (2 * scaleFactor),
                    sliderHeight + (2 * scaleFactor),
                    (sliderHeight / 2) + (1 * scaleFactor),
                    2 * scaleFactor,
                    glowColor
            );
        }
    }

    // Вспомогательный метод для интерполяции цветов
    private Color interpolateColor(Color start, Color end, double progress) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * progress);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * progress);
        int b = (int) (start.getBlue() + (end.getBlue() - start.getBlue()) * progress);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * progress);

        return new Color(
                Math.min(Math.max(r, 0), 255),
                Math.min(Math.max(g, 0), 255),
                Math.min(Math.max(b, 0), 255),
                Math.min(Math.max(a, 0), 255)
        );
    }

    private double getModernSliderAnimationProgress(String sliderId, boolean isEnabled) {
        if (modernSliderAnimationTimes == null) {
            modernSliderAnimationTimes = new HashMap<>();
        }

        // Ключ для текущего состояния
        String currentStateKey = sliderId + "_" + isEnabled;

        // Если состояние изменилось, обновляем время анимации
        if (!modernSliderAnimationTimes.containsKey(currentStateKey)) {
            modernSliderAnimationTimes.put(currentStateKey, System.currentTimeMillis());

            // Удаляем противоположное состояние
            String oppositeStateKey = sliderId + "_" + !isEnabled;
            modernSliderAnimationTimes.remove(oppositeStateKey);
        }

        Long startTime = modernSliderAnimationTimes.get(currentStateKey);
        if (startTime == null) {
            startTime = System.currentTimeMillis();
            modernSliderAnimationTimes.put(currentStateKey, startTime);
        }

        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - startTime;
        double animationDuration = 300.0; // 300ms для плавной анимации

        double progress = Math.min(1.0, elapsed / animationDuration);

        // Для включенного состояния progress идет от 0 до 1
        // Для выключенного состояния progress тоже идет от 0 до 1, но логика инвертируется в drawModernSlider
        return progress;
    }

    private void updateElementAnimations(String menu, float partialTicks) {
        List<Double> anims = elementAnimations.get(menu);
        if (anims == null) return;

        double baseDelay = 0.1; // Задержка между элементами
        double slideProgress = menuSlideAnimations.getOrDefault(menu, 0.0);

        for (int i = 0; i < anims.size(); i++) {
            double elementDelay = baseDelay * i;
            double targetProgress = Math.max(0, Math.min(1, (slideProgress - elementDelay) / (1 - elementDelay)));

            double currentAnim = anims.get(i);
            if (targetProgress > currentAnim) {
                double newAnim = Math.min(currentAnim + partialTicks * 0.4f, targetProgress);
                anims.set(i, newAnim);
            }
        }
    }

    private double getElementAnimation(String menu, int index) {
        List<Double> anims = elementAnimations.get(menu);
        return anims != null && index < anims.size() ? anims.get(index) : 0.0;
    }

    private void drawSmokeBackground(double x, double y, double width, double height, double progress) {
        // Дымовой эффект - размытый полупрозрачный фон
        Color smokeColor = new Color(30, 30, 40, (int) (80 * progress));
        DrawHelper.drawRoundedBlurredRect(x - 10, y - 10, width + 20, height + 20, 15 * scaleFactor, 15 * scaleFactor, smokeColor);

        // Дополнительный дымовой эффект по краям
        Color edgeSmoke = new Color(50, 50, 60, (int) (60 * progress));
        for (int i = 0; i < 3; i++) {
            double offset = i * 3;
            DrawHelper.drawRoundedRectOutline(x - offset, y - offset, width + offset * 2, height + offset * 2,
                    15 * scaleFactor, 1.0f * scaleFactor, edgeSmoke);
        }
    }

    private void drawSmokeElementBackground(double x, double y, double width, double height, double alpha, boolean isSelected, boolean isHovered) {
        Color bgColor;
        if (isSelected) {
            bgColor = new Color(80, 80, 80, (int) (200 * alpha));
        } else if (isHovered) {
            bgColor = new Color(60, 60, 60, (int) (180 * alpha));
        } else {
            bgColor = new Color(40, 40, 40, (int) (150 * alpha));
        }

        DrawHelper.drawRoundedRect(x, y, width, height, 8 * scaleFactor, bgColor);

        // Дымовой эффект вокруг элемента
        if (isHovered) {
            Color smokeGlow = new Color(255, 255, 255, (int) (30 * alpha));
            DrawHelper.drawRoundedBlurredRect(x - 2, y + 2.5, width + 4, height + 4, 10 * scaleFactor, 5 * scaleFactor, smokeGlow);
        }
    }

    private void drawAnimatedTitle(MatrixStack matrixStack, String title, double menuX, double menuY, double menuWidth, double startY, double progress) {
        float titleY = (float) (startY - (50 * scaleFactor));
        float titleAlpha = (float) progress;

        if (VertexVisualMod.textFont != null) {
            float titleWidth = VertexVisualMod.textFont.getWidth(title) * scaleFactor;
            float titleX = (float) (menuX + (menuWidth - titleWidth) / 2);
            Color titleColor = new Color(255, 255, 255, (int) (255 * titleAlpha));
            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, title, titleX, titleY, titleColor);
        } else {
            float titleWidth = mc.font.width(title) * scaleFactor;
            float titleX = (float) (menuX + (menuWidth - titleWidth) / 2);
            float titleYScaled = (float) (startY - (30 * scaleFactor));
            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, title, titleX / scaleFactor, titleYScaled / scaleFactor,
                    new Color(255, 255, 255, (int) (255 * titleAlpha)).getRGB());
            matrixStack.popPose();
        }
    }

    private void drawArrowButton(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, int mouseX, int mouseY, List<Runnable> bloomEffects) {
        double arrowX = 300 * scaleFactor;
        double arrowY = 239 * scaleFactor;
        double arrowSize = 25 * scaleFactor;

        boolean isHovered = mouseX >= arrowX && mouseX <= arrowX + arrowSize &&
                mouseY >= arrowY && mouseY <= arrowY + arrowSize;

        if (isHovered) {
            double time = (System.currentTimeMillis() - startTime) * 0.001;

            double glowX = 280 * scaleFactor;
            double glowY = 262 * scaleFactor;
            double glowWidth = 30 * scaleFactor;
            double glowHeight = 30 * scaleFactor;

            Color[] glowColors = {
                    new Color(91, 1, 116),
                    new Color(167, 0, 189),
                    new Color(95, 0, 107)
            };

            Color[] animatedGlowColors = getAnimatedGlowColors(glowColors, time);

            drawArrowGlow(glowX, glowY + 4, glowWidth, glowHeight, time);

            DrawHelper.drawRoundedGradientRect(glowX, glowY, glowWidth, glowHeight, 5 * scaleFactor,
                    animatedGlowColors[0], animatedGlowColors[1], animatedGlowColors[2], animatedGlowColors[0]);

            // ДОБАВЛЯЕМ эффект стрелки в список bloomEffects (ВМЕСТО прямого вызова BloomHelper)
            if (bloomEffects != null) {
                bloomEffects.add(() -> {
                    drawArrowInnerGlow(glowX, glowY, glowWidth, glowHeight, time);
                });
            }
            // УБИРАЕМ старый код:
            // BloomHelper.registerRenderCall(() -> {
            //     drawArrowInnerGlow(glowX, glowY, glowWidth, glowHeight, time);
            // });
            // BloomHelper.draw(5);
        }

        if (VertexVisualMod.iconFont != null) {
            char arrowChar = 'S';

            float scaledFontSize = VertexVisualMod.iconFont.getFontHeight() * scaleFactor;

            float iconWidth = VertexVisualMod.iconFont.getWidth(String.valueOf(arrowChar)) * scaleFactor;
            float iconHeight = scaledFontSize;
            float iconX = (float) (arrowX + (arrowSize - iconWidth) / 2);
            float iconY = (float) (arrowY + (arrowSize - iconHeight) / 2);

            Color arrowColor = new Color(255, 255, 255, (int) (220 * animation));

            IconRenderer.drawCenteredXYIcon(matrixStack, VertexVisualMod.iconFont,
                    arrowChar, iconX, iconY, arrowColor);
        } else {
            Color arrowColor = new Color(255, 255, 255, (int) (200 * animation));
            String arrowText = sideMenuOpen ? "<" : ">";
            float textWidth = mc.font.width(arrowText) * scaleFactor;
            float textX = (float) (arrowX + (arrowSize - textWidth) / 2);
            float textY = (float) (arrowY + (arrowSize - mc.font.lineHeight * scaleFactor) / 2);

            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, arrowText, textX / scaleFactor, textY / scaleFactor, arrowColor.getRGB());
            matrixStack.popPose();
        }
    }

    private void drawSideMenu(MatrixStack matrixStack, double mainMenuX, double mainMenuY, double mainMenuWidth, double mainMenuHeight, int mouseX, int mouseY, double time) {
        if (sideMenuAnimation <= 0) return;

        double sideMenuWidth = mainMenuWidth * 0.3;
        double sideMenuHeight = mainMenuHeight;

        double targetX = mainMenuX - sideMenuWidth - (10 * scaleFactor);
        double currentX = targetX + (sideMenuWidth + (10 * scaleFactor)) * (1 - sideMenuAnimation);
        double currentY = mainMenuY;

        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();
        DrawHelper.drawRoundedGradientRect(currentX, currentY, sideMenuWidth, sideMenuHeight, 15 * scaleFactor,
                getAnimatedColor(currentGradient, 0, time),
                getAnimatedColor(currentGradient, 1, time),
                getAnimatedColor(currentGradient, 2, time),
                getAnimatedColor(currentGradient, 0, time));

        drawModTitle(matrixStack, currentX, currentY, sideMenuWidth, sideMenuHeight, time);

        String[] categories = {"Movement", "Combat", "Render", "World", "Player", "Settings", "Themes"};
        double categoryHeight = 30 * scaleFactor;
        double startY = currentY - (270 * scaleFactor);

        float[] categoryXOffsets = new float[]{0f, 0f, 0f, 0f, 0f, 0f, 0f};
        float[] outlineYOffsets = new float[]{8f, 8f, 8f, 8f, 8f, 8f, 8f};

        for (int i = 0; i < categories.length; i++) {
            String category = categories[i];
            double categoryY = startY + (i * 1.2 * categoryHeight);
            double categoryWidth = sideMenuWidth - (32 * scaleFactor);
            double categoryX = currentX + (10 * scaleFactor) + (categoryXOffsets[i] * scaleFactor);

            // ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД ДЛЯ ЦВЕТОВ КАТЕГОРИЙ
            Color[] currentCategoryColors = ThemeManager.getCategoryColorsForMenu();
            Color[] animatedColors = getAnimatedCategoryColors(currentCategoryColors, time, i);

            DrawHelper.drawRoundedRectOutline(categoryX + 2, categoryY + 15 + (outlineYOffsets[i] * scaleFactor), categoryWidth, categoryHeight,
                    6 * scaleFactor, 2.0f * scaleFactor, animatedColors[1]);

            float textScale = 15;
            if (VertexVisualMod.textFont != null) {
                float textWidth = VertexVisualMod.textFont.getWidth(category) * scaleFactor * textScale;
                float textX = (float) (categoryX + (35 * scaleFactor));
                float textY = (float) (categoryY + (26 * scaleFactor));

                StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, category, textX, textY, animatedColors[1]);
            } else {
                float textWidth = mc.font.width(category) * scaleFactor;
                float textX = (float) (categoryX + (35 * scaleFactor));
                float textY = (float) (categoryY + (95 * scaleFactor));

                matrixStack.pushPose();
                matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
                mc.font.drawShadow(matrixStack, category, textX / scaleFactor, textY / scaleFactor, animatedColors[1].getRGB());
                matrixStack.popPose();
            }
        }
    }

    private double getWorldSliderAnimationProgress(boolean isEnabled) {
        long animationStartTime = worldSliderAnimationTimes.getOrDefault(0, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        double animationDuration = 300.0;

        double progress = Math.min(1.0, elapsed / animationDuration);
        return progress;
    }

    private void drawWorldSliderHoverEffect(double sliderX, double sliderY, double sliderWidth, double sliderHeight, double progress, float alpha) {
        float glowIntensity = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.3 + 0.7);
        Color glowColor = new Color(255, 255, 255, (int) (50 * glowIntensity * alpha));

        DrawHelper.drawRoundedRectOutline(
                sliderX - (1 * scaleFactor),
                sliderY - (1 * scaleFactor),
                sliderWidth + (2 * scaleFactor),
                sliderHeight + (2 * scaleFactor),
                8 * scaleFactor,
                1.0f * scaleFactor,
                glowColor
        );
    }

    // Методы для анимации слайдеров
    private double getSliderAnimationProgress(int themeIndex, boolean isSelected) {
        // Анимация работает только для активной темы
        if (themeIndex != currentActiveTheme) {
            return isSelected ? 1.0 : 0.0; // Статичное состояние для неактивных тем
        }

        long animationStartTime = sliderAnimationTimes.getOrDefault(themeIndex, System.currentTimeMillis());
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - animationStartTime;
        double animationDuration = 300.0;

        double progress = Math.min(1.0, elapsed / animationDuration);

        return progress; // Всегда от 0 до 1 для активной темы
    }

    private Color getAnimatedSliderColor(boolean isSelected, double progress, double alpha) {
        if (isSelected) {
            int r = (int) (100 + 50 * progress);
            int g = (int) (100 + 100 * progress);
            int b = (int) (100 - 80 * progress);
            return new Color(r, g, b, (int) (255 * alpha));
        } else {
            return new Color(100, 100, 100, (int) (255 * alpha));
        }
    }

    private void drawSliderHoverEffect(double sliderX, double sliderY, double sliderWidth, double sliderHeight, double progress, double alpha) {
        float glowIntensity = (float) (Math.sin(System.currentTimeMillis() * 0.005) * 0.3 + 0.7);
        Color glowColor = new Color(255, 255, 255, (int) (50 * glowIntensity * alpha));

        DrawHelper.drawRoundedRectOutline(
                sliderX - (1 * scaleFactor),
                sliderY - (1 * scaleFactor),
                sliderWidth + (2 * scaleFactor),
                sliderHeight + (2 * scaleFactor),
                8 * scaleFactor,
                1.0f * scaleFactor,
                glowColor
        );
    }

    private Color getAnimatedCircleColor(boolean isSelected, double progress) {
        float pulse = (float) (Math.sin(System.currentTimeMillis() * 0.01) * 0.1 + 0.9);

        if (isSelected) {
            int alpha = (int) (200 + 55 * progress * pulse);
            return new Color(255, 255, 255, alpha);
        } else {
            int gray = (int) (200 + 55 * (1 - progress) * pulse);
            return new Color(gray, gray, gray);
        }
    }

    private void drawModTitle(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight, double time) {
        String title = "Выбери вкладку";

        double titleY = menuY - menuHeight + (28.5 * scaleFactor);
        double titleX = menuX + (menuWidth / 2.8);

        // ИСПОЛЬЗУЕМ НОВЫЙ МЕТОД ДЛЯ ЦВЕТОВ КАТЕГОРИЙ
        Color[] currentCategoryColors = ThemeManager.getCategoryColorsForMenu();
        Color[] animatedPinkColors = getAnimatedCategoryColors(currentCategoryColors, time, 0);

        float textScale = 2.0f;

        if (VertexVisualMod.textFont != null) {
            float textWidth = VertexVisualMod.textFont.getWidth(title) * scaleFactor;
            float textX = (float) (titleX - textWidth / 2);
            float textY = (float) titleY;

            StyledFontRenderer.drawString(matrixStack, VertexVisualMod.textFont, title, textX, textY, animatedPinkColors[1]);
        } else {
            float textWidth = mc.font.width(title) * scaleFactor;
            float textX = (float) (titleX - textWidth / 2);
            float textY = (float) titleY;

            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, title, textX / scaleFactor, textY / scaleFactor, animatedPinkColors[1].getRGB());
            matrixStack.popPose();
        }
    }

    private Color[] getAnimatedCategoryColors(Color[] baseColors, double time, int index) {
        Color[] animatedColors = new Color[4];

        for (int i = 0; i < 3; i++) {
            Color baseColor = baseColors[i];

            // Если цвет белый, не применяем анимацию пульсации
            if (baseColor.getRed() == 255 && baseColor.getGreen() == 255 && baseColor.getBlue() == 255) {
                animatedColors[i] = new Color(255, 255, 255, 180);
            } else {
                // Для обычных цветов применяем анимацию
                float pulse = (float) (Math.sin(time * 3 + index + i) * 0.2 + 0.8);

                int r = (int) (baseColor.getRed() * pulse);
                int g = (int) (baseColor.getGreen() * pulse);
                int b = (int) (baseColor.getBlue() * pulse);

                animatedColors[i] = new Color(
                        Math.min(Math.max(r, 0), 255),
                        Math.min(Math.max(g, 0), 255),
                        Math.min(Math.max(b, 0), 255),
                        180
                );
            }
        }
        animatedColors[3] = animatedColors[0];

        return animatedColors;
    }

    private void drawSearchPanel(MatrixStack matrixStack, double menuX, double menuY, double menuWidth, double menuHeight) {
        double searchHeight = 15.0 * scaleFactor;
        double searchWidth = menuWidth * 0.6;
        double searchX = menuX + (menuWidth - searchWidth) / 2;
        double searchY = menuY + (15.0 * scaleFactor);

        Color searchPanelColor = new Color(80, 80, 80, (int) (150 * animation));
        DrawHelper.drawRoundedRect(searchX, searchY, searchWidth, searchHeight, 5 * scaleFactor, searchPanelColor);

        double lineStartX = searchX + (20 * scaleFactor);
        double lineY = searchY - searchHeight / 2;
        Color lineColor = new Color(120, 120, 120, (int) (150 * animation));
        DrawHelper.drawRect(lineStartX, lineY - (searchHeight - 25.5f * scaleFactor) / 2,
                1.0 * scaleFactor, searchHeight - (4 * scaleFactor), lineColor);

        if (!searchFocused) {
            String searchLabel = "поиск";
            float textX = (float) searchX + (25f * scaleFactor);
            float textY = (float) searchY - (12f * scaleFactor);
            Color textColor = new Color(255, 255, 255, 200);

            matrixStack.pushPose();
            matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
            mc.font.drawShadow(matrixStack, searchLabel, textX / scaleFactor, textY / scaleFactor, textColor.getRGB());
            matrixStack.popPose();
        } else {
            if (!searchText.isEmpty()) {
                float textX = (float) searchX + (25f * scaleFactor);
                float textY = (float) searchY - (12f * scaleFactor);
                Color textColor = new Color(220, 220, 220, (int) (200 * animation));

                matrixStack.pushPose();
                matrixStack.scale(scaleFactor, scaleFactor, 1.0f);
                mc.font.drawShadow(matrixStack, searchText, textX / scaleFactor, textY / scaleFactor, textColor.getRGB());
                matrixStack.popPose();
            }

            long currentTime = System.currentTimeMillis();
            if ((currentTime - cursorBlinkTimer) % 1000 < 500) {
                float cursorX = (float) searchX + (25f * scaleFactor) + mc.font.width(searchText) * scaleFactor;
                float cursorY = (float) searchY - (2.5f * scaleFactor);
                DrawHelper.drawRect(cursorX, cursorY, 1 * scaleFactor, 10 * scaleFactor,
                        new Color(220, 220, 220, (int) (200 * animation)));
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        calculateScaleFactor();

// === ГЛОБАЛЬНАЯ БЛОКИРОВКА КЛИКОВ ПРИ ОТКРЫТОЙ ПАЛИТРЕ ===
        if (ThemeManager.isColorPickerOpen() && colorPickerAnimation > 0) {
            // ОБНОВЛЕННЫЕ КООРДИНАТЫ ПАЛИТРЫ
            double pickerWidth = 400 * scaleFactor;
            double pickerHeight = 250 * scaleFactor;
            double pickerX = (width - pickerWidth) / 2;
            double pickerY = (height - pickerHeight) / 2 + 250 * scaleFactor;

            // ОБЪЯВЛЯЕМ ПЕРЕМЕННЫЕ ЗАРАНЕЕ (выносим из блоков if)
            double colorWheelSize = 200 * scaleFactor;
            double colorWheelX = pickerX + (pickerWidth - colorWheelSize) / 2;
            double colorWheelY = pickerY - 300 * scaleFactor;
            double centerX = colorWheelX + colorWheelSize / 2;
            double centerY = colorWheelY + colorWheelSize / 2;

            double brightnessSliderWidth = 180 * scaleFactor;
            double brightnessSliderHeight = 20 * scaleFactor;
            double brightnessSliderX = colorWheelX + (colorWheelSize - brightnessSliderWidth) / 2;
            double brightnessSliderY = colorWheelY + colorWheelSize + 60 * scaleFactor;

            // СНАЧАЛА проверяем клики по элементам палитры
            boolean clickedOnPaletteElement = false;

            // 1. Проверяем клик по круговой палитре
            double distance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
            if (distance <= colorWheelSize / 2) {
                ThemeManager.setDraggingColorWheel(true);
                ThemeManager.setColorWheelAngle(Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX)));
                if (ThemeManager.getColorWheelAngle() < 0)
                    ThemeManager.setColorWheelAngle(ThemeManager.getColorWheelAngle() + 360);
                ThemeManager.setColorWheelRadius(Math.min(distance / (colorWheelSize / 2), 1.0));
                ThemeManager.updateSelectedColorFromWheel();
                clickedOnPaletteElement = true;
            }

            // 2. Проверяем клик по ползунку яркости
            if (!clickedOnPaletteElement) {
                brightnessSliderWidth = 180 * scaleFactor;
                brightnessSliderHeight = 20 * scaleFactor;
                brightnessSliderX = colorWheelX + (colorWheelSize - brightnessSliderWidth) / 2;
                brightnessSliderY = colorWheelY + colorWheelSize + 40 * scaleFactor;

                if (mouseX >= brightnessSliderX && mouseX <= brightnessSliderX + brightnessSliderWidth &&
                        mouseY >= brightnessSliderY && mouseY <= brightnessSliderY + brightnessSliderHeight) {
                    ThemeManager.setDraggingBrightness(true);
                    double newBrightness = (mouseX - brightnessSliderX) / brightnessSliderWidth;
                    ThemeManager.setBrightnessValue(Math.max(0, Math.min(1, newBrightness)));
                    ThemeManager.updateSelectedColorFromWheel();
                    clickedOnPaletteElement = true;
                } else {
                }
            }

            // 3. Проверяем клик по кнопкам (теперь brightnessSliderY доступна)
            if (!clickedOnPaletteElement) {
                double buttonWidth = 100 * scaleFactor;
                double buttonHeight = 30 * scaleFactor;
                double buttonsY = brightnessSliderY + brightnessSliderHeight + 20 * scaleFactor;
                double buttonsStartX = pickerX + (pickerWidth - buttonWidth * 2 - 5 * scaleFactor) / 2;

                double applyButtonX = buttonsStartX;
                if (mouseX >= applyButtonX && mouseX <= applyButtonX + buttonWidth &&
                        mouseY >= buttonsY && mouseY <= buttonsY + buttonHeight) {
                    ThemeManager.updateSelectedColorFromWheel();
                    ThemeManager.applyCustomTheme();
                    ThemeManager.setColorPickerOpen(false);
                    clickedOnPaletteElement = true;
                }


                double closeButtonX = applyButtonX + buttonWidth + 10 * scaleFactor;
                if (!clickedOnPaletteElement && mouseX >= closeButtonX && mouseX <= closeButtonX + buttonWidth &&
                        mouseY >= buttonsY && mouseY <= buttonsY + buttonHeight) {
                    ThemeManager.setColorPickerOpen(false);
                    clickedOnPaletteElement = true;
                }
            }

            // 4. Проверяем клик по кнопкам выбора цвета
            if (!clickedOnPaletteElement) {
                double colorButtonsY = pickerY + 10 * scaleFactor;
                double colorButtonSize = 25 * scaleFactor;
                double colorButtonsStartX = pickerX + 20 * scaleFactor;

                for (int i = 0; i < 3; i++) {
                    double colorButtonX = colorButtonsStartX + i * (colorButtonSize + 10 * scaleFactor);
                    if (mouseX >= colorButtonX && mouseX <= colorButtonX + colorButtonSize &&
                            mouseY >= colorButtonsY && mouseY <= colorButtonsY + colorButtonSize) {
                        ThemeManager.setCurrentEditingColorIndex(i);
                        if (i == 0) {
                            ThemeManager.updateSelectedColorFromWheel();
                        }
                        clickedOnPaletteElement = true;
                        break;
                    }
                }
            }

            // 5. Если кликнули на любом элементе палитры - блокируем дальнейшую обработку
            if (clickedOnPaletteElement) {
                return true;
            }

            // 6. Если клик был ВНЕ палитры - тоже блокируем
            boolean isClickInPicker = mouseX >= pickerX && mouseX <= pickerX + pickerWidth &&
                    mouseY >= pickerY && mouseY <= pickerY + pickerHeight;

            if (!isClickInPicker) {
                return true; // Блокируем клик
            }

            return true; // Блокируем все клики при открытой палитре
        }

        // ОСТАЛЬНОЙ КОД ОБРАБОТКИ КЛИКОВ (когда палитра закрыта)
        double windowWidth = (width * 0.5) * scaleFactor;
        double windowHeight = (height * 0.5) * scaleFactor;

        windowWidth = Math.max(windowWidth, 500 * scaleFactor);
        windowHeight = Math.max(windowHeight, 400 * scaleFactor);

        double x = (width - windowWidth) / 2;
        double y = (height - windowHeight) / 2;

        double animatedWidth = windowWidth * animation;
        double animatedHeight = windowHeight * animation;
        double animatedX = x + (windowWidth - animatedWidth) / 2;
        double animatedY = y + (windowHeight - animatedHeight) / 2;

        double yOffset = windowHeight * 0.9;

        double arrowSize = 25 * scaleFactor;
        double arrowX = 280 * scaleFactor;
        double arrowY = 230 * scaleFactor;

        if (mouseX >= arrowX && mouseX <= arrowX + arrowSize &&
                mouseY >= arrowY && mouseY <= arrowY + arrowSize) {
            sideMenuOpen = !sideMenuOpen;
            if (!sideMenuOpen) {
                activeCategory = "";
            }
            return true;
        }

        double searchHeight = 15.0 * scaleFactor;
        double searchWidth = animatedWidth * 0.6;
        double searchX = animatedX + (animatedWidth - searchWidth) / 2;
        double searchY = animatedY + yOffset + (15.0 * scaleFactor);

        double searchTop = searchY - searchHeight;

        if (mouseX >= searchX && mouseX <= searchX + searchWidth &&
                mouseY >= searchTop && mouseY <= searchY) {
            searchFocused = true;
            cursorBlinkTimer = System.currentTimeMillis();
            return true;
        }

        if (sideMenuOpen && sideMenuAnimation > 0) {
            double sideMenuWidth = animatedWidth * 0.3;
            double sideMenuHeight = animatedHeight;

            double targetX = animatedX - sideMenuWidth - (10 * scaleFactor);
            double currentX = targetX + (sideMenuWidth + (10 * scaleFactor)) * (1 - sideMenuAnimation);
            double currentY = animatedY;

            String[] categories = {"Movement", "Combat", "Render", "World", "Player", "Settings", "Themes"};
            double categoryHeight = 30 * scaleFactor;
            double startY = currentY + (50 * scaleFactor);

            for (int i = 0; i < categories.length; i++) {
                String category = categories[i];
                double categoryY = startY + (i * 1.2 * categoryHeight);
                double categoryWidth = sideMenuWidth - (32 * scaleFactor);
                double categoryX = currentX + (10 * scaleFactor);

                if (mouseX >= categoryX && mouseX <= categoryX + categoryWidth &&
                        mouseY >= categoryY && mouseY <= categoryY + categoryHeight) {

                    // Если кликаем на уже активную категорию - закрываем ее
                    if (activeCategory.equals(category)) {
                        activeCategory = "";

                        if (category.equals("Themes")) {
                            themesMenuWasOpen = false;
                        } else if (category.equals("World")) {
                            worldMenuWasOpen = false;
                        } else if (category.equals("Movement")) {
                            movementMenuWasOpen = false;
                        }
                    } else {
                        // Иначе открываем новую категорию
                        activeCategory = category;

                        if (category.equals("Themes")) {
                            themesMenuWasOpen = false;
                            themesMenuAnimation = 0.0;
                        } else if (category.equals("World")) {
                            worldMenuWasOpen = false;
                            worldMenuAnimation = 0.0;
                        } else if (category.equals("Movement")) {
                            movementMenuWasOpen = true;
                            movementMenuAnimation = 0.0;
                            movementMenuOpenTime = System.currentTimeMillis();
                        }
                    }
                    return true;
                }
            }
        }

        // Обработка кликов в World меню
        if (activeCategory.equals("World")) {
            double worldOptionWidth = 250 * scaleFactor;
            double worldOptionHeight = 60 * scaleFactor;
            double worldStartX = themesStartX;
            double worldStartY = themesStartY;

            double worldX = worldStartX;
            double worldY = worldStartY;

            // Проверяем клик по опции "Цвет неба"
            if (mouseX >= worldX && mouseX <= worldX + worldOptionWidth &&
                    mouseY >= worldY && mouseY <= worldY + worldOptionHeight) {
                skyColorEnabled = !skyColorEnabled;
                SkyColorManager.setEnabled(skyColorEnabled);

                // Обновляем время анимации для слайдера
                worldSliderAnimationTimes.put(0, System.currentTimeMillis());
                return true;
            }
        }

        // Обработка кликов в Themes меню (когда палитра закрыта)
        if (activeCategory.equals("Themes") && !ThemeManager.isColorPickerOpen()) {
            List<Theme> themes = ThemeManager.getThemes();
            double themeWidth = 150 * scaleFactor;
            double themeHeight = 40 * scaleFactor;

            double slideOffset = 100 * (1 - menuSlideAnimations.getOrDefault("Themes", 0.0));
            double startX = themesStartX - slideOffset;
            double startY = themesStartY;

            for (int i = 0; i < themes.size(); i++) {
                double themeX = startX + (i % 2) * (themeWidth + 20 * scaleFactor);
                double themeY = startY + (i / 2) * (themeHeight + 15 * scaleFactor) - (50 * scaleFactor);

                double elementAnim = getElementAnimation("Themes", i);
                if (elementAnim > 0) {
                    // УЧИТЫВАЕМ АНИМАЦИЮ ЭЛЕМЕНТОВ (как в методе отрисовки)
                    double elementOffsetY = 10 * (1 - elementAnim);
                    double animatedThemeY = themeY - elementOffsetY;

                    // СНАЧАЛА проверяем клик по ползунку (приоритет выше)
                    double sliderWidth = 40 * scaleFactor;
                    double sliderHeight = 20 * scaleFactor;
                    double sliderX = themeX + themeWidth - sliderWidth - (10 * scaleFactor);
                    double sliderY = animatedThemeY + (5 * scaleFactor);

                    if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                            mouseY >= sliderY && mouseY <= sliderY + sliderHeight) {
                        sliderAnimationTimes.put(i, System.currentTimeMillis());
                        currentActiveTheme = i;
                        ThemeManager.setTheme(i);
                        return true;
                    }

                    // ДОБАВЛЯЕМ ПРОВЕРКУ КЛИКА ПО КНОПКЕ ПАЛИТРЫ ДЛЯ КАСТОМНОЙ ТЕМЫ
                    if (i == ThemeManager.getCustomThemeIndex()) {
                        double pickerButtonSize = 20 * scaleFactor;
                        double pickerButtonX = themeX + themeWidth + (5 * scaleFactor);
                        double pickerButtonY = animatedThemeY + (themeHeight - pickerButtonSize) / 2;

                        double adjustedPickerX = pickerButtonX + (3 * scaleFactor);
                        double adjustedPickerY = pickerButtonY - (4 * scaleFactor);

                        if (mouseX >= adjustedPickerX && mouseX <= adjustedPickerX + pickerButtonSize &&
                                mouseY >= adjustedPickerY && mouseY <= adjustedPickerY + pickerButtonSize) {
                            ThemeManager.setColorPickerOpen(true);
                            return true;
                        }
                    }

                    // ПОТОМ проверяем клик по самой теме
                    if (mouseX >= themeX && mouseX <= themeX + themeWidth &&
                            mouseY >= animatedThemeY && mouseY <= animatedThemeY + themeHeight) {
                        sliderAnimationTimes.put(i, System.currentTimeMillis());
                        currentActiveTheme = i;
                        ThemeManager.setTheme(i);
                        return true;
                    }
                }
            }

            // Проверяем клик по кнопке палитры в заголовке
            float titleY = (float) (themesStartY - (50 * scaleFactor));
            String titleText = "Выбор темы";
            float titleWidth = VertexVisualMod.textFont != null ?
                    VertexVisualMod.textFont.getWidth(titleText) * scaleFactor :
                    mc.font.width(titleText) * scaleFactor;
            float titleX = (float) ((width - titleWidth) / 2);

            double pickerButtonSize = 25 * scaleFactor;
            double pickerButtonX = titleX + titleWidth + (20 * scaleFactor);
            double pickerButtonY = titleY - (5 * scaleFactor);

            if (mouseX >= pickerButtonX && mouseX <= pickerButtonX + pickerButtonSize &&
                    mouseY >= pickerButtonY && mouseY <= pickerButtonY + pickerButtonSize) {
                ThemeManager.setColorPickerOpen(true);
                return true;
            }

            // Обработка клика по кнопке "Моя тема"
            if (ThemeManager.isCustomThemeModified()) {
                List<Theme> themesList = ThemeManager.getThemes();
                double themeWidth2 = 150 * scaleFactor;
                double themeHeight2 = 40 * scaleFactor;

                double slideOffset2 = 100 * (1 - menuSlideAnimations.getOrDefault("Themes", 0.0));
                double startX2 = themesStartX - slideOffset2;
                double startY2 = themesStartY;

                int themesPerColumn = (int) Math.ceil(themesList.size() / 2.0);
                double customThemeX = startX2;
                double customThemeY = startY2 + (themesPerColumn * (themeHeight2 + 20 * scaleFactor));

                // Проверяем клик по слайдеру кастомной темы
                double customSliderX = customThemeX + themeWidth2 - (40 * scaleFactor) - (10 * scaleFactor);
                double customSliderY = customThemeY + (10 * scaleFactor);
                double sliderWidth = 40 * scaleFactor;
                double sliderHeight = 20 * scaleFactor;

                if (mouseX >= customSliderX && mouseX <= customSliderX + sliderWidth &&
                        mouseY >= customSliderY && mouseY <= customSliderY + sliderHeight) {
                    ThemeManager.applyCustomTheme();
                    return true;
                }

                // Или клик по самой карточке кастомной темы
                if (mouseX >= customThemeX && mouseX <= customThemeX + themeWidth2 &&
                        mouseY >= customThemeY && mouseY <= customThemeY + themeHeight2) {
                    ThemeManager.applyCustomTheme();
                    return true;
                }
            }
        }

// Обработка кликов в Settings меню - ТОЧНО КАК В THEMES
        if (activeCategory.equals("Settings")) {
            double settingsOptionWidth = 200 * scaleFactor;
            double settingsOptionHeight = 40 * scaleFactor;

            // ТАК ЖЕ как в themes - используем те же координаты и анимацию
            double slideOffset = 100 * (1 - menuSlideAnimations.getOrDefault("Settings", 0.0));
            double startX = themesStartX - slideOffset;
            double startY = themesStartY;

            Map<String, Boolean> settings = settingsManager.getAllBooleanSettings();
            String[] settingKeys = {
                    SettingsManager.REMOVE_EXTERNAL_GLOW,
                    SettingsManager.HIDE_UID,
                    SettingsManager.STREAMER_MODE,
                    SettingsManager.DISABLE_ALL_MODS
            };

            for (int i = 0; i < settingKeys.length; i++) {
                double elementAnim = getElementAnimation("Settings", i);
                if (elementAnim <= 0) continue;

                // ТОЧНО ТАК ЖЕ как в themes menu
                double optionY = startY + i * (settingsOptionHeight + 15 * scaleFactor);
                double elementOffsetY = 10 * (1 - elementAnim);
                double animatedSettingsY = optionY - elementOffsetY; // Переименовали

                // КООРДИНАТЫ СЛАЙДЕРОВ - ТОЧНО КАК В THEMES
                double sliderWidth = 40 * scaleFactor;
                double sliderHeight = 20 * scaleFactor;
                double sliderX = startX + settingsOptionWidth - sliderWidth - (10 * scaleFactor);
                double sliderY = animatedSettingsY + (settingsOptionHeight - sliderHeight) / 2 - (10 * scaleFactor);

                // ПРОВЕРКА КЛИКА - ТОЧНО КАК В THEMES
                if (mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
                        mouseY >= sliderY && mouseY <= sliderY + sliderHeight) {
                    settingsManager.toggleBoolean(settingKeys[i]);
                    settingsSliderAnimationTimes.put(settingKeys[i], System.currentTimeMillis());
                    return true;
                }

                // Дополнительно: клик по всей карточке (как бонус)
                if (mouseX >= startX && mouseX <= startX + settingsOptionWidth &&
                        mouseY >= animatedSettingsY && mouseY <= animatedSettingsY + settingsOptionHeight) {
                    settingsManager.toggleBoolean(settingKeys[i]);
                    settingsSliderAnimationTimes.put(settingKeys[i], System.currentTimeMillis());
                    return true;
                }
            }
        }

        searchFocused = false;
        return super.mouseClicked(mouseX, mouseY, button); // ← ВАЖНО: этот return должен быть в самом конце метода
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (ThemeManager.isColorPickerOpen() && colorPickerAnimation > 0) {
            // ОБНОВЛЕННЫЕ КООРДИНАТЫ ПАЛИТРЫ
            double pickerWidth = 400 * scaleFactor;
            double pickerHeight = 250 * scaleFactor;
            double pickerX = (width - pickerWidth) / 2;
            double pickerY = (height - pickerHeight) / 2 + 250 * scaleFactor;

            if (ThemeManager.isDraggingColorWheel()) {
                double colorWheelSize = 200 * scaleFactor;
                double colorWheelX = pickerX + (pickerWidth - colorWheelSize) / 2;
                double colorWheelY = pickerY - 300 * scaleFactor;
                double centerX = colorWheelX + colorWheelSize / 2;
                double centerY = colorWheelY + colorWheelSize / 2;

                double distance = Math.sqrt(Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2));
                ThemeManager.setColorWheelAngle(Math.toDegrees(Math.atan2(mouseY - centerY, mouseX - centerX)));
                if (ThemeManager.getColorWheelAngle() < 0) ThemeManager.setColorWheelAngle(ThemeManager.getColorWheelAngle() + 360);
                ThemeManager.setColorWheelRadius(Math.min(distance / (colorWheelSize / 2), 1.0));
                ThemeManager.updateSelectedColorFromWheel();
                return true;
            }

            if (ThemeManager.isDraggingBrightness()) {
                double colorWheelSize = 200 * scaleFactor;
                double colorWheelX = pickerX + (pickerWidth - colorWheelSize) / 2;
                double colorWheelY = pickerY - 300 * scaleFactor; // ← ДОБАВЬ ЭТУ СТРОКУ
                double brightnessSliderWidth = 180 * scaleFactor;
                double brightnessSliderX = colorWheelX + (colorWheelSize - brightnessSliderWidth) / 2;
                double brightnessSliderY = colorWheelY + colorWheelSize + 60 * scaleFactor;


                double newBrightness = (mouseX - brightnessSliderX) / brightnessSliderWidth;
                ThemeManager.setBrightnessValue(Math.max(0, Math.min(1, newBrightness)));
                ThemeManager.updateSelectedColorFromWheel();
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        // Завершаем перетаскивание
        ThemeManager.setDraggingColorWheel(false);
        ThemeManager.setDraggingBrightness(false);

        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (searchFocused) {
            if (codePoint >= 32 && codePoint <= 126) {
                searchText += codePoint;
                cursorBlinkTimer = System.currentTimeMillis();
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void drawDarkBackground() {
        DrawHelper.drawRect(0, 0, width, height, new Color(5, 0, 10, 255));
    }

    private void drawBlurredOverlay() {
        if (SkyColorManager.isEnabled()) {
            Vector3f skyColor = SkyColorManager.getSkyColor();
            if (skyColor != null) {
                // Используем ПРИГЛУШЕННЫЙ цвет для дыма
                Color fogColor = new Color(
                        (int) (skyColor.x() * 255),
                        (int) (skyColor.y() * 255),
                        (int) (skyColor.z() * 255),
                        60  // Очень прозрачный дым
                );
                DrawHelper.drawRect(0, 0, width, height, fogColor);
            }
        } else {
            // Стандартный прозрачный черный
            DrawHelper.drawRect(0, 0, width, height, new Color(0, 0, 0, 60));
        }
    }

    private Color getAnimatedColor(Color[] gradientColors, int index, double time) {
        int colorIndex = index % gradientColors.length;
        Color baseColor = gradientColors[colorIndex];

        float pulse = (float) (Math.sin(time * 2 + index) * 0.1 + 0.9);

        int r = (int) (baseColor.getRed() * pulse + 20);
        int g = (int) (baseColor.getGreen() * pulse + 10);
        int b = (int) (baseColor.getBlue() * pulse + 15);
        int a = 255;

        return new Color(
                Math.min(Math.max(r, 0), 255),
                Math.min(Math.max(g, 0), 255),
                Math.min(Math.max(b, 0), 255),
                Math.min(a, 255)
        );
    }

    private Color[] getAnimatedGlowColors(Color[] baseColors, double time) {
        Color[] animatedColors = new Color[4];

        for (int i = 0; i < 3; i++) {
            Color baseColor = baseColors[i];
            float pulse = (float) (Math.sin(time * 3 + i) * 0.15 + 0.85);

            int r = (int) (baseColor.getRed() * pulse);
            int g = (int) (baseColor.getGreen() * pulse);
            int b = (int) (baseColor.getBlue() * pulse);

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

    private void drawArrowGlow(double x, double y, double width, double height, double time) {
        float glowIntensity = (float) (Math.sin(time * 4) * 0.3 + 0.7);
        float glowSize = 8.0f * scaleFactor;

        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();

        Color mainGlowColor = new Color(
                currentGradient[1].getRed(),
                currentGradient[1].getGreen(),
                currentGradient[1].getBlue(),
                (int) (150 * glowIntensity)
        );

        Color secondaryGlowColor = new Color(
                currentGradient[0].getRed(),
                currentGradient[0].getGreen(),
                currentGradient[0].getBlue(),
                (int) (120 * glowIntensity)
        );

        DrawHelper.drawRoundedBlurredRect(
                x - glowSize,
                y,
                width + glowSize * 2,
                height + glowSize * 2,
                5 + glowSize,
                glowSize,
                mainGlowColor
        );

        DrawHelper.drawRoundedBlurredRect(
                x - glowSize / 2,
                y,
                width + glowSize,
                height + glowSize,
                5 + glowSize / 2,
                glowSize / 2,
                secondaryGlowColor
        );
    }

    private void drawArrowInnerGlow(double x, double y, double width, double height, double time) {
        if (scaleFactor <= 0) return;
        float pulse = (float) (Math.sin(time * 5) * 0.4 + 0.6);
        Color innerGlow = new Color(255, 255, 255, (int) (80 * pulse));

        DrawHelper.drawRoundedRectOutline(
                x + (3 * scaleFactor),
                y + (1 * scaleFactor),
                width - (2 * scaleFactor),
                height - (2 * scaleFactor),
                4 * scaleFactor,
                1.0f * scaleFactor,
                innerGlow
        );
    }

    private void drawExternalGlow(double x, double y, double width, double height, double time) {
        float glowIntensity = (float) (Math.sin(time * 2) * 0.4 + 0.6);
        float glowSize = 20.0f * scaleFactor;
        double downOffset = 15.0 * scaleFactor;

        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();

        Color mainGlowColor = new Color(
                currentGradient[1].getRed(),
                currentGradient[1].getGreen(),
                currentGradient[1].getBlue(),
                (int) (200 * glowIntensity * animation)
        );

        Color secondaryGlowColor = new Color(
                currentGradient[0].getRed(),
                currentGradient[0].getGreen(),
                currentGradient[0].getBlue(),
                (int) (180 * glowIntensity * animation)
        );

        DrawHelper.drawRoundedBlurredRect(
                x - glowSize,
                y + downOffset,
                width + glowSize * 2,
                height + glowSize * 2,
                15 + glowSize,
                glowSize,
                mainGlowColor
        );

        DrawHelper.drawRoundedBlurredRect(
                x - glowSize / 2,
                y + downOffset,
                width + glowSize,
                height + glowSize,
                15 + glowSize / 2,
                glowSize / 2,
                secondaryGlowColor
        );
    }

    private void drawInternalGlow(double x, double y, double width, double height, double time) {
        float pulse = (float) (Math.sin(time * 4) * 0.5 + 0.5);

        Color[] currentGradient = ThemeManager.getCurrentTheme().getGradientColors();
        Color innerGlow = new Color(
                currentGradient[1].getRed(),
                currentGradient[1].getGreen(),
                currentGradient[1].getBlue(),
                (int) (120 * pulse * animation)
        );

        DrawHelper.drawRoundedRectOutline(x + (2 * scaleFactor), y + (2 * scaleFactor),
                width - (4 * scaleFactor), height - (4 * scaleFactor),
                13 * scaleFactor, 2.0f * scaleFactor, innerGlow);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) { // ESC key
            if (ThemeManager.isColorPickerOpen() && colorPickerAnimation > 0) {
                ThemeManager.setColorPickerOpen(false);
                return true;
            } else {
                toggle();
                return true;
            }
        }

        if (searchFocused) {
            if (keyCode == 259) { // Backspace
                if (!searchText.isEmpty()) {
                    searchText = searchText.substring(0, searchText.length() - 1);
                    cursorBlinkTimer = System.currentTimeMillis();
                }
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}