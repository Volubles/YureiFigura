package org.figuramc.figura.mixin.render.feature;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.feature.FeatureRendererMap;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import org.figuramc.figura.ducks.FiguraPreparedFrameExtension;
import org.figuramc.figura.ducks.NodeCollectorExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.*;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(FeatureRenderDispatcher.class)
public class FeatureRendererDispatcherMixin {
    @Shadow @Final private FeatureRendererMap featureRenderers;
    @Shadow @Final private FeatureRenderDispatcher.PreparedFrame preparedFrame;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void figura$registerRenderer(CallbackInfo ci) {
        featureRenderers.put(FiguraFeatureRenderer.TYPE, new FiguraFeatureRenderer());
        featureRenderers.put(PreparedNativeSubmission.Renderer.TYPE, new PreparedNativeSubmission.Renderer());
    }

    @WrapMethod(method = "prepareFrameWithContext")
    private FeatureRenderDispatcher.PreparedFrame figura$prepare(FeatureFrameContext context, SubmitNodeStorage storage, Operation<FeatureRenderDispatcher.PreparedFrame> original) {
        FiguraPreparedFrameExtension frameExtension = (FiguraPreparedFrameExtension) preparedFrame;
        if (frameExtension.figura$isActive()) {
            throw new IllegalStateException("PreparedFrame already in use");
        }
        FiguraRenderFrame resources = new FiguraRenderFrame(context);
        try {
            // let lua and models do their thing after extraction, before we build vertices.
            boolean more;
            do {
                more = false;
                for (var entry : new ArrayList<>(storage.getSubmitsPerOrder().int2ObjectEntrySet())) {
                    SubmitNodeCollection collection = entry.getValue();
                    RelativeSubmitNodeCollector collector = new RelativeSubmitNodeCollector(storage, entry.getIntKey());
                    NodeCollectorExtension extension = (NodeCollectorExtension) collection;
                    List<FiguraRenderCall> calls = extension.getFiguraRenderCalls();
                    List<NativePreparation> pending = extension.figura$getNativePreparations();
                    if (!calls.isEmpty() || !pending.isEmpty()) {
                        more = true;
                    }
                    for (int i = 0; i < calls.size(); i++) {
                        FiguraRenderCall call = calls.get(i);
                        if (call.avatar() != null) {
                            call.renderer().apply(call.avatar(), call.renderState(), collector);
                        }
                    }
                    calls.clear();
                    for (int i = 0; i < pending.size(); i++) {
                        resources.prepare(pending.get(i), collector);
                    }
                    pending.clear();
                }
            } while (more);
            resources.finishPrepare();
            for (SubmitNodeCollection collection : storage.getSubmitsPerOrder().values()) {
                resources.addAll(((NodeCollectorExtension) collection).figura$takeGeometry());
            }
            FeatureRenderDispatcher.PreparedFrame frame = original.call(context, storage);
            ((FiguraPreparedFrameExtension) frame).figura$setResources(resources);
            return frame;
        } catch (RuntimeException | Error failure) {
            for (SubmitNodeCollection collection : storage.getSubmitsPerOrder().values()) {
                resources.addAll(((NodeCollectorExtension) collection).figura$takeGeometry());
                ((NodeCollectorExtension) collection).getFiguraRenderCalls().clear();
                ((NodeCollectorExtension) collection).figura$getNativePreparations().clear();
            }
            try (resources) {
                try {
                    // prep blew up before the queues drained. drop the fucking leftovers.
                    storage.drainPhases(phase -> phase.sortInto((submit, ordered) -> {}));
                } catch (RuntimeException | Error cleanupFailure) {
                    if (cleanupFailure != failure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                try {
                    if (frameExtension.figura$isActive()) {
                        preparedFrame.close();
                    } else {
                        context.stagedVertexBuffer().endDraw();
                    }
                } catch (RuntimeException | Error cleanupFailure) {
                    if (cleanupFailure != failure) {
                        failure.addSuppressed(cleanupFailure);
                    }
                }
                throw failure;
            }
        }
    }
}
