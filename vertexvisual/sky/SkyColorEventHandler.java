package dev.sxmurxy.vertexvisual.sky;

import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "yourmodid", value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SkyColorEventHandler {
    @SubscribeEvent
    public static void onFogColors(EntityViewRenderEvent.FogColors event) {
        Vector3f skyColor = SkyColorManager.getSkyColor();
        if (skyColor != null && SkyColorManager.isEnabled()) {
            float softness = 5f;
            event.setRed(skyColor.x() * softness);
            event.setGreen(skyColor.y() * softness);
            event.setBlue(skyColor.z() * softness);
        }
    }
}