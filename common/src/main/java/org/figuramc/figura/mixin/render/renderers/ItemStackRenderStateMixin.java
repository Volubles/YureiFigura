package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.FiguraItemStackRenderStateExtension;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;
import org.figuramc.figura.ducks.SkullBlockRendererAccessor;
import org.figuramc.figura.ducks.SkullBlockRendererHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;


@Mixin(ItemStackRenderState.class)
public class ItemStackRenderStateMixin implements FiguraItemStackRenderStateExtension, FiguraSubmitCallBackExtension {
    @Shadow
    ItemDisplayContext displayContext;
    @Shadow private ItemStackRenderState.LayerRenderState[] layers;
    @Unique
    ItemStack figura$itemStack;
    @Unique
    private static int figura$submitDepth;
    @Unique
    private final List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> figura$preRenderingCallback = new ArrayList<>();
    @Unique
    private final List<Runnable> figura$postRenderingCallback = new ArrayList<>();

    @Override
    public void figura$setItemStack(@Nullable ItemStack itemStack) {
        this.figura$itemStack = itemStack;
    }

    @Override
    public ItemStack figura$getItemStack() {
        return figura$itemStack;
    }

    @Override
    public boolean figura$isLeftHanded() {
        return displayContext.leftHand();
    }

    @Override
    public ItemDisplayContext figura$getDisplayContext() {
        return displayContext;
    }

    @Override
    public ItemTransform figura$getItemTransform() {
        for (ItemStackRenderState.LayerRenderState layerRenderState : layers) {
            if (((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getTransform() != null)
                return ((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getTransform();
        }
        return ItemTransform.NO_TRANSFORM;
    }

    @Override
    public List<BakedQuad> figura$getQuads() {
        for (ItemStackRenderState.LayerRenderState layerRenderState : layers) {
            if (((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getQuads() != null)
                return ((ItemStackRenderState$LayerRenderStateAccessor)layerRenderState).figura$getQuads().all();
        }
        return List.of();
    }

    @Override
    public void figura$addPreRenderingCallback(BiFunction<SubmitNodeCollector, PoseStack, Boolean> callback) {
        this.figura$preRenderingCallback.add(callback);
    }

    @Override
    public void figura$addPostRenderingCallback(Runnable callback) {
        this.figura$postRenderingCallback.add(callback);
    }

    @Override
    public List<Runnable> figura$getPostRenderingCallbacks() {
        return figura$postRenderingCallback;
    }

    @Override
    public List<BiFunction<SubmitNodeCollector, PoseStack, Boolean>> figura$getPreRenderingCallbacks() {
        return figura$preRenderingCallback;
    }

    @WrapMethod(method = "submit")
    private void figura$submit(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int light, int overlay, int outlineColor, Operation<Void> original) {
        ItemStack previousItem = SkullBlockRendererAccessor.getItem();
        Entity previousEntity = SkullBlockRendererAccessor.getEntity();
        SkullBlockRendererAccessor.SkullRenderMode previousMode = SkullBlockRendererAccessor.getRenderMode();
        Avatar previousAvatar = SkullBlockRendererHelper.getAvatar();
        figura$submitDepth++;
        try {
            SkullBlockRendererAccessor.setRenderMode(switch (displayContext) {
                case FIRST_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_LEFT_HAND;
                case FIRST_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.FIRST_PERSON_RIGHT_HAND;
                case THIRD_PERSON_LEFT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_LEFT_HAND;
                case THIRD_PERSON_RIGHT_HAND -> SkullBlockRendererAccessor.SkullRenderMode.THIRD_PERSON_RIGHT_HAND;
                case HEAD -> SkullBlockRendererAccessor.SkullRenderMode.HEAD;
                default -> SkullBlockRendererAccessor.SkullRenderMode.OTHER;
            });
            SkullBlockRendererAccessor.setItem(figura$itemStack);
            original.call(poseStack, submitNodeCollector, light, overlay, outlineColor);
        } finally {
            // these helpers live on the render thread; only nested submits have a parent to restore.
            boolean nested = --figura$submitDepth > 0;
            SkullBlockRendererAccessor.setItem(nested ? previousItem : null);
            SkullBlockRendererAccessor.setEntity(nested ? previousEntity : null);
            SkullBlockRendererAccessor.setRenderMode(nested ? previousMode : SkullBlockRendererAccessor.SkullRenderMode.OTHER);
            SkullBlockRendererHelper.setAvatar(nested ? previousAvatar : null);
        }
    }

    @Inject(method = "clear", at = @At("HEAD"))
    private void figura$clear(CallbackInfo ci) {
        this.figura$itemStack = null;
    }
}
