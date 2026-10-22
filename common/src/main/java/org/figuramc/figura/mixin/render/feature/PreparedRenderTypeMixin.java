package org.figuramc.figura.mixin.render.feature;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import net.minecraft.client.renderer.rendertype.PreparedRenderType;
import org.figuramc.figura.model.rendering.nodeRenderer.HudRenderContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(PreparedRenderType.class)
public class PreparedRenderTypeMixin {
    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;getCompiledPipeline(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;)Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;"), index = 0)
    private RenderPipeline figura$hudDepth(RenderPipeline pipeline) {
        return HudRenderContext.pipeline(pipeline);
    }
}
