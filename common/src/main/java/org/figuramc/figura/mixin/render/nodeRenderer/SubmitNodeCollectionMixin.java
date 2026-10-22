package org.figuramc.figura.mixin.render.nodeRenderer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.*;
import org.figuramc.figura.model.rendering.nodeRenderer.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import java.util.ArrayList;
import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class SubmitNodeCollectionMixin implements NodeCollectorExtension {
    @Unique private final List<FiguraRenderCall> figura$calls = new ArrayList<>();
    @Unique private final List<FiguraSubmission> figura$geometry = new ArrayList<>();
    @Unique private final List<NativePreparation> figura$nativePreparations = new ArrayList<>();

    @Override
    @SuppressWarnings("unchecked")
    public <S extends EntityRenderState> void submitFiguraModel(Avatar avatar, S state, TriFunction<Avatar, S, SubmitNodeCollector, Void> renderer) {
        figura$calls.add(new FiguraRenderCall(avatar, state, (TriFunction<Avatar, EntityRenderState, SubmitNodeCollector, Void>) (TriFunction<?, ?, ?, ?>) renderer));
    }

    @Override
    public List<FiguraRenderCall> getFiguraRenderCalls() {
        return figura$calls;
    }

    @Override
    public List<NativePreparation> figura$getNativePreparations() {
        return figura$nativePreparations;
    }

    @Override
    public void figura$submitGeometry(FiguraSubmission submission) {
        SubmitNodeCollection collection = (SubmitNodeCollection) (Object) this;
        figura$geometry.add(submission);
        if (submission.material().isOutline()) {
            collection.outline.submit(submission);
        } else if (submission.material().hasBlending() && !submission.material().forceSolidModelPhase()) {
            collection.translucentCustomGeometry.submit(submission);
        } else {
            collection.solid.submit(submission);
        }
    }

    @Override
    public List<FiguraSubmission> figura$takeGeometry() {
        List<FiguraSubmission> geometry = new ArrayList<>(figura$geometry);
        figura$geometry.clear();
        return geometry;
    }

    @Unique
    private static void figura$transferCallbacks(Object source, Object target) {
        FiguraSubmitCallBackExtension from = (FiguraSubmitCallBackExtension) source;
        FiguraSubmitCallBackExtension to = (FiguraSubmitCallBackExtension) target;
        from.figura$getPreRenderingCallbacks().forEach(to::figura$addPreRenderingCallback);
        from.figura$getPostRenderingCallbacks().forEach(to::figura$addPostRenderingCallback);
        to.figura$setPreventAnimSetup(from.figura$getPreventAnimSetup());
        from.figura$getPreRenderingCallbacks().clear();
        from.figura$getPostRenderingCallbacks().clear();
        from.figura$setPreventAnimSetup(false);
    }

    @ModifyExpressionValue(method = "submitModel", at = @At(value = "NEW", target = "net/minecraft/client/renderer/feature/ModelFeatureRenderer$Submit"))
    private <S> ModelFeatureRenderer.Submit<S> figura$model(ModelFeatureRenderer.Submit<S> submit) {
        figura$transferCallbacks(submit.model(), submit);
        return submit;
    }

    @ModifyExpressionValue(method = "submitItem", at = @At(value = "NEW", target = "net/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit"))
    private ItemFeatureRenderer.Submit figura$item(ItemFeatureRenderer.Submit submit) {
        figura$transferCallbacks(submit.displayContext(), submit);
        return submit;
    }

    @ModifyExpressionValue(method = "submitFlame", at = @At(value = "NEW", target = "net/minecraft/client/renderer/feature/FlameFeatureRenderer$Submit"))
    private FlameFeatureRenderer.Submit figura$flame(FlameFeatureRenderer.Submit submit) {
        ((FlameSubmitExtension) (Object) submit).figura$setAvatar(AvatarManager.getAvatar(submit.entityRenderState()));
        return submit;
    }

    @WrapOperation(method = {"submitModel", "submitItem"}, at = {
        @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/SimpleFeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V"),
        @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/feature/phase/FeatureRenderPhase;submit(Lnet/minecraft/client/renderer/feature/submit/SubmitNode;)V")
    })
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void figura$prepareCallbacks(@Coerce FeatureRenderPhase phase, SubmitNode submit, Operation<Void> original) {
        FiguraSubmitCallBackExtension callbacks = (FiguraSubmitCallBackExtension) submit;
        if (!callbacks.figura$getPreRenderingCallbacks().isEmpty() || !callbacks.figura$getPostRenderingCallbacks().isEmpty() || callbacks.figura$getPreventAnimSetup()) {
            figura$nativePreparations.add(new NativePreparation(submit, prepared -> original.call(phase, prepared)));
        } else {
            original.call(phase, submit);
        }
    }
}
