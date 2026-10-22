package org.figuramc.figura.mixin.render.feature;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.FlameSubmitExtension;
import org.figuramc.figura.utils.RenderUtils;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(FlameFeatureRenderer.class)
public class FlameFeatureRendererMixin {
    @WrapMethod(method = "prepare")
    private void figura$fire(FlameFeatureRenderer.Submit submit, VertexConsumer output, TextureAtlasSprite first,
                            TextureAtlasSprite second, Operation<Void> original) {
        Avatar avatar = ((FlameSubmitExtension) (Object) submit).figura$getAvatar();
        TextureAtlasSprite customFirst = RenderUtils.firstFireLayer(avatar);
        TextureAtlasSprite customSecond = RenderUtils.secondFireLayer(avatar);
        original.call(submit, output, customFirst == null ? first : customFirst, customSecond == null ? second : customSecond);
    }
}
