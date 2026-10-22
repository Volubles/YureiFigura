package org.figuramc.figura.mixin.render.nodeRenderer;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import org.figuramc.figura.ducks.FiguraPreparedFrameExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraRenderFrame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FeatureRenderDispatcher.PreparedFrame.class)
public class PreparedFrameMixin implements FiguraPreparedFrameExtension {
    @Shadow private FeatureFrameContext context;

    @Override
    public boolean figura$isActive() {
        return context != null;
    }
    @Unique
    private FiguraRenderFrame figura$resources;

    @Override
    public void figura$setResources(FiguraRenderFrame resources) {
        if (figura$resources != null) {
            throw new IllegalStateException("Previous Figura frame is still open");
        }
        figura$resources = resources;
    }

    @WrapMethod(method = "close")
    private void figura$releaseResources(Operation<Void> original) {
        FiguraRenderFrame resources = figura$resources;
        figura$resources = null;
        try (resources) {
            original.call();
        }
    }
}
