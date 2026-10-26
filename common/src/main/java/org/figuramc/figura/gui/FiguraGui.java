package org.figuramc.figura.gui;

import com.mojang.blaze3d.ProjectionType;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.joml.Matrix4f;
import org.joml.Matrix3x2f;
import org.figuramc.figura.mixin.gui.GuiGraphicsAccessor;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public class FiguraGui {

    public static void onRender(GuiGraphicsExtractor guiGraphics, float tickDelta, CallbackInfo ci) {
        if (AvatarManager.panic)
            return;

        FiguraMod.pushProfiler(FiguraMod.MOD_ID);

        // render popup menu below everything, as if it were in the world
        FiguraMod.pushProfiler("popupMenu");
        PopupMenu.extractRenderState(guiGraphics);
        FiguraMod.popProfiler();

        // get avatar
        Entity entity = Minecraft.getInstance().getCameraEntity();
        Avatar avatar = entity == null ? null : AvatarManager.getAvatar(entity);

        if (avatar != null) {
            var window = Minecraft.getInstance().getWindow();
            ((GuiGraphicsAccessor) guiGraphics).figura$getRenderState().addPicturesInPictureState(
                    new FiguraHudRenderState(avatar, entity, tickDelta, window.getGuiScaledWidth(), window.getGuiScaledHeight(), new Matrix3x2f(guiGraphics.pose())));
            // hud hidden by script
            if (avatar.luaRuntime != null && !avatar.luaRuntime.renderer.renderHUD) {
                // render figura overlays
                renderOverlays(guiGraphics);
                // cancel this method
                ci.cancel();
            }
        }

        FiguraMod.popProfiler();
    }

    public static void renderOverlays(GuiGraphicsExtractor guiGraphics) {
        FiguraMod.pushProfiler(FiguraMod.MOD_ID);

        // render paperdoll
        FiguraMod.pushProfiler("paperdoll");
        PaperDoll.extractRenderState(guiGraphics, false);

        // render wheel
        FiguraMod.popPushProfiler("actionWheel");
        ActionWheel.extractRenderState(guiGraphics);

        FiguraMod.popProfiler(2);
    }
}
