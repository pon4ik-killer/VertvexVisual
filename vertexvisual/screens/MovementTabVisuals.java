package dev.sxmurxy.vertexvisual.screens;

import java.awt.Color;

import com.mojang.blaze3d.matrix.MatrixStack;

import dev.sxmurxy.vertexvisual.util.render.DrawHelper;

/**
 * Separate class for Movement tab visuals outline.
 */
public class MovementTabVisuals {

    /**
     * Draw same outline as Themes for Movement tab.
     */
    public static void drawOutline(MatrixStack matrixStack, double x, double y, double width, double height, float scaleFactor, Color color) {
        DrawHelper.drawRoundedRectOutline(x, y, width, height, 6 * scaleFactor, 2.0f * scaleFactor, color);
    }
}
