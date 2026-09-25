package dev.sxmurxy.vertexvisual.themes;

import java.awt.Color;

public class Theme {
    private final String name;
    private final Color[] gradientColors;
    private final Color[] categoryColors;

    public Theme(String name, Color[] gradientColors, Color[] categoryColors) {
        this.name = name;
        this.gradientColors = gradientColors;
        this.categoryColors = categoryColors;
    }

    public String getName() { return name; }
    public Color[] getGradientColors() { return gradientColors; }
    public Color[] getCategoryColors() { return categoryColors; }
}