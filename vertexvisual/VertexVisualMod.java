package dev.sxmurxy.vertexvisual;

import com.mojang.blaze3d.matrix.MatrixStack;
import dev.sxmurxy.vertexvisual.screens.SimpleShaderMenu;
import dev.sxmurxy.vertexvisual.util.font.common.Lang;
import dev.sxmurxy.vertexvisual.util.font.icon.IconFont; // Добавь этот импорт
import dev.sxmurxy.vertexvisual.util.font.styled.StyledFont;
import dev.sxmurxy.vertexvisual.util.render.WatermarkRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod(VertexVisualMod.MOD_ID)
public class VertexVisualMod {

    public static final String MOD_ID = "vertexvisual";
    public static final String FONT_DIR = "/assets/" + MOD_ID + "/font/";

    public static IconFont logoFont = new IconFont("logo.ttf", 25, 'V');
    public static StyledFont textFont = new StyledFont("sf-ui.ttf", 15, 0.0f, 0.1f, 1.0f, Lang.ENG_RU);
    public static IconFont iconFont = new IconFont("strelka.ttf", 25, 'S');
    public static IconFont iconFont1 = new IconFont("gora.ttf", 25, 'O');
    public static IconFont iconFont2;
    public static IconFont iconFont3 = new IconFont("settings.ttf", 25, 'M');
    public static IconFont iconFont4 = new IconFont("People.ttf", 25, 'P');
    public static IconFont iconFont5 = new IconFont("sword.ttf", 25, 'Q');
    private KeyBinding openMenuKey;

    public VertexVisualMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);
        initFonts(); // Добавь эту строку
    }

    private void initFonts() {
        try {
            logoFont = new IconFont("logo.ttf", 25, 'V');
            textFont = new StyledFont("sf-ui.ttf", 15, 0.0f, 0.1f, 1.0f, Lang.ENG_RU);
            iconFont = new IconFont("strelka.ttf", 25, 'S');
            iconFont1 = new IconFont("gora.ttf", 25, 'O');
            iconFont2 = new IconFont("listik.ttf", 25, 'N'); // Теперь с обработкой ошибок
            iconFont3 = new IconFont("settings.ttf", 25, 'M');
            iconFont4 = new IconFont("People.ttf", 25, 'P');
            iconFont5 = new IconFont("sword.ttf", 25, 'Q');
        } catch (Exception e) {
            System.out.println("Ошибка загрузки шрифтов: " + e.getMessage());
            // Можно установить значения по умолчанию или null
            iconFont2 = null; // Важно - чтобы не было краша
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        openMenuKey = new KeyBinding("Открытие меню",
                GLFW.GLFW_KEY_R, "Vertex Visual");

        ClientRegistry.registerKeyBinding(openMenuKey);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new dev.sxmurxy.vertexvisual.sky.SkyColorEventHandler());
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getInstance();

        if (openMenuKey.isDown()) {
            if (mc.screen instanceof SimpleShaderMenu) {
                mc.setScreen(null); // закрыть
            } else {
                mc.setScreen(new SimpleShaderMenu()); // открыть
            }
        }
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    private static class ClientEvents {

        @SubscribeEvent
        public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
            if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
                WatermarkRenderer.render(new MatrixStack());
            }
        }
    }
}