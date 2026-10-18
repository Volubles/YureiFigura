package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import org.figuramc.figura.ducks.NodeCollectorExtension;

import java.util.List;

public class FiguraFeatureRenderer {
    public void render(SubmitNodeCollection submitNodeCollection, MultiBufferSource.BufferSource bufferSource) {
        List<FiguraRenderCall> renderCalls = ((NodeCollectorExtension) submitNodeCollection).getFiguraRenderCalls();

        for (int i = 0; i < renderCalls.size(); i++) {
            FiguraRenderCall renderCall = renderCalls.get(i);
            if (renderCall.avatar() == null) {
                continue;
            }

            renderCall.renderer().apply(
                    renderCall.avatar(),
                    renderCall.renderState(),
                    bufferSource
            );
        }
    }
}
