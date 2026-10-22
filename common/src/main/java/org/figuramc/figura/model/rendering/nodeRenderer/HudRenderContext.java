package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import org.figuramc.figura.utils.FiguraIdentifier;
import java.util.IdentityHashMap;
import java.util.Map;

/** turn off depth for the hud's pip pass, then leave everyone else's pipelines alone. */
public final class HudRenderContext {
    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> false);
    private static final Map<RenderPipeline, RenderPipeline> PIPELINES = new IdentityHashMap<>();

    private HudRenderContext() {
    }

    public static void withoutDepth(Runnable draw) {
        boolean previous = ACTIVE.get();
        ACTIVE.set(true);
        try {
            draw.run();
        } finally {
            if (previous) {
                ACTIVE.set(true);
            } else {
                ACTIVE.remove();
            }
        }
    }

    public static RenderPipeline pipeline(RenderPipeline original) {
        return ACTIVE.get() && original.wantsDepthTexture() ? PIPELINES.computeIfAbsent(original, WithoutDepth::new) : original;
    }

    private static final class WithoutDepth extends RenderPipeline {
        private WithoutDepth(RenderPipeline original) {
            super(new FiguraIdentifier("pipeline/hud/" + original.getLocation().getNamespace() + "/" + original.getLocation().getPath()),
                    original.getShaders(), original.getShaderDefines(), original.getBindGroupLayouts(),
                    original.getColorTargetStates().toArray(ColorTargetState[]::new), null,
                    original.getPolygonMode(), original.isCull(), original.getVertexFormatBindings().toArray(VertexFormat[]::new),
                    original.getPrimitiveTopology(), original.pushConstantSize(), original.getSortKey());
        }
    }
}
