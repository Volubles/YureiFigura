package org.figuramc.figura.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import org.figuramc.figura.model.rendering.nodeRenderer.HudRenderContext;

public class FiguraHudRenderer extends PictureInPictureRenderer<FiguraHudRenderState> {
    @Override
    public Class<FiguraHudRenderState> getRenderStateClass() {
        return FiguraHudRenderState.class;
    }

    @Override
    public void prepare(FiguraHudRenderState state, GuiRenderState guiState, FeatureRenderDispatcher dispatcher, int scale) {
        HudRenderContext.withoutDepth(() -> super.prepare(state, guiState, dispatcher, scale));
    }

    @Override
    protected float getTranslateY(int height, int scale) {
        return 0;
    }

    @Override
    protected void renderToTexture(FiguraHudRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        pose.translate(-state.x1() / 2f, 0, 0);
        state.avatar().hudRender(pose, collector, state.entity(), state.tickDelta());
    }

    @Override
    protected String getTextureLabel() {
        return "figura-hud";
    }
}
