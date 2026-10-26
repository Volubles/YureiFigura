package org.figuramc.figura.gui;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.gui.BlitRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.gui.widgets.permissions.PlayerPermPackElement;
import org.figuramc.figura.mixin.render.PoseStackAccessor;
import org.figuramc.figura.utils.ui.UIHelper;
import java.util.ArrayList;
import java.util.List;

public class FiguraPortraitRenderer extends PictureInPictureRenderer<FiguraPortraitRenderState> {
    // each portrait needs its own damn attachment until the gui is done drawing.
    private final List<FiguraPortraitRenderer> portraits = new ArrayList<>();
    private int usedPortraits;
    private final boolean avatarRenderer;
    private boolean renderSkin;

    public FiguraPortraitRenderer() {
        this(false);
    }

    private FiguraPortraitRenderer(boolean avatarRenderer) {
        this.avatarRenderer = avatarRenderer;
    }

    @Override
    public Class<FiguraPortraitRenderState> getRenderStateClass() {
        return FiguraPortraitRenderState.class;
    }

    @Override
    public void prepare(FiguraPortraitRenderState state, GuiRenderState guiState, FeatureRenderDispatcher dispatcher, int scale) {
        if (!avatarRenderer) {
            if (usedPortraits == portraits.size()) {
                portraits.add(new FiguraPortraitRenderer(true));
            }
            portraits.get(usedPortraits++).prepare(state, guiState, dispatcher, scale);
        } else {
            super.prepare(state, guiState, dispatcher, scale);
        }
    }

    public void endFrame() {
        while (portraits.size() > usedPortraits) {
            portraits.removeLast().close();
        }
        usedPortraits = 0;
    }

    @Override
    protected void renderToTexture(FiguraPortraitRenderState state, PoseStack pose, SubmitNodeCollector collector) {
        boolean previousPaperdoll = UIHelper.paperdoll;
        float previousDollScale = UIHelper.dollScale;
        var renderer = state.avatar() == null ? null : state.avatar().renderer;
        boolean previousPivotParts = renderer == null || renderer.allowPivotParts;
        int lastIndex = ((PoseStackAccessor) pose).getLastIndex();
        try {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
            renderSkin = state.avatar() == null || !state.avatar().renderHeadForPortrait(collector, pose,
                    LightCoordsUtil.FULL_BRIGHT, state.modelScale(), state.upsideDown());
        } finally {
            while (((PoseStackAccessor) pose).getLastIndex() > lastIndex) {
                pose.popPose();
            }
            UIHelper.paperdoll = previousPaperdoll;
            UIHelper.dollScale = previousDollScale;
            if (renderer != null) {
                renderer.allowPivotParts = previousPivotParts;
            }
        }
    }

    @Override
    protected void blitTexture(FiguraPortraitRenderState state, GuiRenderState guiState) {
        if (!renderSkin) {
            super.blitTexture(state, guiState);
        } else if (state.fallbackSkin() != null) {
            blitSkin(state, guiState, state.fallbackSkin(), 8 / 64f, 16 / 64f, 8 / 64f, 16 / 64f);
            blitSkin(state, guiState, state.fallbackSkin(), 40 / 64f, 48 / 64f, 8 / 64f, 16 / 64f);
        } else {
            blitSkin(state, guiState, PlayerPermPackElement.UNKNOWN, 0, 1, 0, 1);
        }
    }

    private void blitSkin(FiguraPortraitRenderState state, GuiRenderState guiState, Identifier id, float u0, float u1, float v0, float v1) {
        var texture = Minecraft.getInstance().getTextureManager().getTexture(id);
        guiState.addBlitToCurrentLayer(new BlitRenderState(RenderPipelines.GUI_TEXTURED,
                TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler()), state.pose(),
                state.x0(), state.y0(), state.x1(), state.y1(), u0, u1, v0, v1, -1, state.scissorArea(), null));
    }

    @Override
    protected String getTextureLabel() {
        return "figura-portrait";
    }

    @Override
    public void close() {
        for (FiguraPortraitRenderer renderer : portraits) {
            renderer.close();
        }
        portraits.clear();
        usedPortraits = 0;
        super.close();
    }
}
