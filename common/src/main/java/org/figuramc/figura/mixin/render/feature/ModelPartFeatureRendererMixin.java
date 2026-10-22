package org.figuramc.figura.mixin.render.feature;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Function;

@Mixin(Model.Simple.class)
public class ModelPartFeatureRendererMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void figura$callbacks(ModelPart root, Function<Identifier, RenderType> renderType, CallbackInfo ci) {
        FiguraSubmitCallBackExtension source = (FiguraSubmitCallBackExtension) (Object) root;
        FiguraSubmitCallBackExtension target = (FiguraSubmitCallBackExtension) this;
        source.figura$getPreRenderingCallbacks().forEach(target::figura$addPreRenderingCallback);
        source.figura$getPostRenderingCallbacks().forEach(target::figura$addPostRenderingCallback);
        target.figura$setPreventAnimSetup(source.figura$getPreventAnimSetup());
        source.figura$getPreRenderingCallbacks().clear();
        source.figura$getPostRenderingCallbacks().clear();
        source.figura$setPreventAnimSetup(false);
    }
}
