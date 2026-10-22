package org.figuramc.figura.mixin.render.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbackContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ModelFeatureRenderer.class)
public class ModelFeatureRendererMixin {
    @WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;setupAnim(Ljava/lang/Object;)V"))
    private <S> void figura$setup(Model<S> model, S state, Operation<Void> original, @Local(argsOnly = true) ModelFeatureRenderer.Submit<S> submit) {
        if (!((FiguraSubmitCallBackExtension) (Object) submit).figura$getPreventAnimSetup()) {
            original.call(model, state);
        }
    }

    @WrapOperation(method = "prepareModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/Model;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"))
    private <S> void figura$render(Model<S> model, PoseStack pose, VertexConsumer output, int light, int overlay, int tint,
                                   Operation<Void> original, @Local(argsOnly = true) ModelFeatureRenderer.Submit<S> submit) {
        FiguraSubmitCallBackExtension callbacks = (FiguraSubmitCallBackExtension) (Object) submit;
        org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbacks.render(callbacks, pose,
                () -> original.call(model, pose, output, light, overlay, tint));
    }
}
