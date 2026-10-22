package org.figuramc.figura.mixin.render.feature;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbackContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemFeatureRenderer.class)
public class ItemFeatureRendererMixin {
    @WrapMethod(method = "prepareSubmit")
    private void figura$render(ItemFeatureRenderer.Submit submit, Operation<Void> original) {
        FiguraSubmitCallBackExtension callbacks = (FiguraSubmitCallBackExtension) (Object) submit;
        PoseStack pose = new PoseStack();
        pose.last().set(submit.pose());
        org.figuramc.figura.model.rendering.nodeRenderer.FiguraCallbacks.render(callbacks, pose, () -> {
            submit.pose().set(pose.last());
            original.call(submit);
        });
    }
}
