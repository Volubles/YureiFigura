package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FeatureRendererType;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import java.util.List;

public final class FiguraFeatureRenderer extends RenderTypeFeatureRenderer<FiguraSubmission> {
    public static final FeatureRendererType<FiguraSubmission> TYPE = FeatureRendererType.create("Figura");

    @Override
    protected void buildGroup(FeatureFrameContext context, List<FiguraSubmission> submissions) {
        for (FiguraSubmission submission : submissions) {
            submission.replay(getVertexBuilder(submission.material()), (output, u, v) -> output.setUv3(u, v));
        }
    }
}
