package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.renderpearl.api.commands.RenderPass;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.client.renderer.feature.submit.TranslucentSubmit;
import net.minecraft.client.renderer.oit.OitStage;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/** prep vanilla groups before sorting; callbacks can still chuck more work into the queues. */
public record PreparedNativeSubmission<S extends SubmitNode>(
        RenderTypeFeatureRenderer<S> renderer, int groupIndex, S source
) implements TranslucentSubmit {
    @Override
    public float distanceToCameraSq() {
        return source instanceof TranslucentSubmit translucent ? translucent.distanceToCameraSq() : 0;
    }

    @Override
    public FeatureRendererType<PreparedNativeSubmission<?>> featureType() {
        return Renderer.TYPE;
    }

    private void execute(FeatureFrameContext context, @Nullable OitStage stage, RenderPass pass) {
        renderer.executeGroup(context, stage, pass, groupIndex, List.of(source), true);
    }

    public static final class Renderer implements FeatureRenderer<PreparedNativeSubmission<?>> {
        public static final FeatureRendererType<PreparedNativeSubmission<?>> TYPE = FeatureRendererType.create("Figura native callbacks");

        @Override
        public void prepareGroup(FeatureFrameContext context, List<PreparedNativeSubmission<?>> submits, boolean strictlyOrdered) {
            // the frame already staged the geometry before the phase queues got drained.
        }

        @Override
        public void executeGroup(FeatureFrameContext context, @Nullable OitStage stage, RenderPass pass, int index,
                                 List<PreparedNativeSubmission<?>> submits, boolean strictlyOrdered) {
            for (PreparedNativeSubmission<?> submit : submits) {
                submit.execute(context, stage, pass);
            }
        }
    }
}
