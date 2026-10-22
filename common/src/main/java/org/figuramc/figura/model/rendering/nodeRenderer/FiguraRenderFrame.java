package org.figuramc.figura.model.rendering.nodeRenderer;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import java.util.IdentityHashMap;
import java.util.Map;

/** holds the frame's resources, even for geometry we ditch before gpu prep. */
public final class FiguraRenderFrame implements AutoCloseable {
    private final List<FiguraSubmission> submissions = new ArrayList<>();
    private final FeatureFrameContext context;
    private ModelFeatureRenderer models;
    private ItemFeatureRenderer items;
    private final Map<RenderTypeFeatureRenderer<?>, Integer> groups = new IdentityHashMap<>();
    private boolean closed;

    public FiguraRenderFrame(FeatureFrameContext context) {
        this.context = context;
    }

    public void prepare(NativePreparation pending, SubmitNodeCollector collector) {
        if (pending.source() instanceof ModelFeatureRenderer.Submit<?> model) {
            if (models == null) {
                models = new ModelFeatureRenderer();
                models.beginPrepare(context);
            }
            prepare(pending, collector, models, model);
        } else if (pending.source() instanceof ItemFeatureRenderer.Submit item) {
            if (items == null) {
                items = new ItemFeatureRenderer();
                items.beginPrepare(context);
            }
            prepare(pending, collector, items, item);
        } else {
            throw new IllegalArgumentException("Unsupported Figura callback submit: " + pending.source());
        }
    }

    private <S extends SubmitNode> void prepare(NativePreparation pending, SubmitNodeCollector collector,
                                               RenderTypeFeatureRenderer<S> renderer, S submit) {
        int index = groups.getOrDefault(renderer, 0);
        FiguraCallbackContext.prepare(collector, () -> renderer.prepareGroup(context, List.of(submit), true));
        groups.put(renderer, index + 1);
        pending.publish().accept(new PreparedNativeSubmission<>(renderer, index, submit));
    }

    public void finishPrepare() {
        if (models != null) {
            models.finishPrepare(context);
        }
        if (items != null) {
            items.finishPrepare(context);
        }
    }

    public void addAll(List<FiguraSubmission> geometry) {
        submissions.addAll(geometry);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        Throwable failure = null;
        for (FiguraSubmission submission : submissions) {
            try {
                submission.close();
            } catch (RuntimeException | Error exception) {
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            }
        }
        submissions.clear();
        for (RenderTypeFeatureRenderer<?> renderer : new RenderTypeFeatureRenderer<?>[] {models, items}) {
            if (renderer == null) {
                continue;
            }
            try {
                renderer.finishExecute(context);
            } catch (RuntimeException | Error exception) {
                if (failure == null) {
                    failure = exception;
                } else if (failure != exception) {
                    failure.addSuppressed(exception);
                }
            }
        }
        groups.clear();
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }
}
