package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.figuramc.figura.model.rendering.CapturedVertexBatch;

import java.util.Objects;

/** Frozen geometry and its selected material; replay never evaluates avatar code. */
public final class FiguraSubmission implements AutoCloseable, net.minecraft.client.renderer.feature.submit.SubmitNode {
    private final RenderType material;
    private final CapturedVertexBatch vertices;
    private TextureLease texture;
    private boolean closed;

    /** Transfers the lease to this submission; the pass owner must close it after drawing or discard. */
    public FiguraSubmission(RenderType material, CapturedVertexBatch vertices, TextureLease texture) {
        this.material = Objects.requireNonNull(material);
        this.vertices = Objects.requireNonNull(vertices);
        this.texture = texture;
    }

    @Override
    public net.minecraft.client.renderer.feature.FeatureRendererType<FiguraSubmission> featureType() {
        return FiguraFeatureRenderer.TYPE;
    }

    public RenderType material() {
        return material;
    }

    public void replay(VertexConsumer output) {
        replay(output, null);
    }

    public void replay(VertexConsumer output, CapturedVertexBatch.Uv3Writer uv3Writer) {
        if (closed) {
            throw new IllegalStateException("Figura submission is closed");
        }
        vertices.replay(output, uv3Writer);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        TextureLease retained = texture;
        texture = null;
        if (retained != null) {
            retained.close();
        }
    }

    @FunctionalInterface
    public interface TextureLease extends AutoCloseable {
        @Override
        void close();
    }
}
