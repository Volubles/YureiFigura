package org.figuramc.figura.mixin.render.renderers;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.CameraRenderStateExtension;
import org.figuramc.figura.ducks.EntityRendererAccessor;
import org.figuramc.figura.ducks.FiguraEntityRenderStateExtension;
import org.figuramc.figura.permissions.Permissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity, S extends EntityRenderState> implements EntityRendererAccessor {

    @Inject(at = @At("HEAD"), method = "shouldRender", cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double d, double e, double f, float delta, CallbackInfoReturnable<Boolean> cir) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (avatar != null && avatar.permissions.get(Permissions.OFFSCREEN_RENDERING) == 1)
            cir.setReturnValue(true);
    }

    @Inject(at = @At("HEAD"), method = "extractRenderState")
    private void extractRenderState(T entity, S entityRenderState, float f, CallbackInfo ci) {
        ((FiguraEntityRenderStateExtension)entityRenderState).figura$setEntityId(entity.getId());
        ((FiguraEntityRenderStateExtension)entityRenderState).figura$setTickDelta(f);
    }

    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void figura$outline(T entity, S state, float delta, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(state);
        if (state.outlineColor != 0 && org.figuramc.figura.utils.RenderUtils.vanillaModelAndScript(avatar)
                && avatar.luaRuntime.renderer.outlineColor != null) {
            state.outlineColor = net.minecraft.util.ARGB.opaque(org.figuramc.figura.utils.ColorUtils.rgbToInt(avatar.luaRuntime.renderer.outlineColor));
        }
    }


    @com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation(
            method = "submitNameDisplay(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZILnet/minecraft/client/renderer/state/level/CameraRenderState;)V"))
    private void figura$nameplate(net.minecraft.client.renderer.SubmitNodeCollector collector, com.mojang.blaze3d.vertex.PoseStack pose,
            net.minecraft.world.phys.Vec3 attachment, int offset, net.minecraft.network.chat.Component text, boolean seeThrough,
            int light, CameraRenderState camera, com.llamalad7.mixinextras.injector.wrapoperation.Operation<Void> original,
            @Local(argsOnly = true) EntityRenderState state) {
        Avatar avatar = AvatarManager.getAvatar(state);
        boolean name = text == state.nameTag;
        var custom = avatar == null || avatar.luaRuntime == null ? null : avatar.luaRuntime.nameplate.ENTITY;
        boolean permitted = custom != null && avatar.permissions.get(Permissions.NAMEPLATE_EDIT) == 1;
        if (permitted && !custom.visible && org.figuramc.figura.config.Configs.ENTITY_NAMEPLATE.value > 0
                && !AvatarManager.panic) {
            return;
        }
        if (avatar != null) {
            CameraRenderState replacement = new CameraRenderState();
            replacement.pos = camera.pos;
            replacement.blockPos = camera.blockPos;
            replacement.entityRenderState = camera.entityRenderState;
            replacement.initialized = camera.initialized;
            replacement.orientation.set(camera.orientation);
            ((CameraRenderStateExtension) replacement).figura$setAvatar(avatar);
            ((CameraRenderStateExtension) replacement).figura$setRenderingNameTag(name);
            camera = replacement;
        }
        int config = org.figuramc.figura.config.Configs.ENTITY_NAMEPLATE.value;
        if (name && state instanceof net.minecraft.client.renderer.entity.state.AvatarRenderState player && config > 0 && !AvatarManager.panic) {
            net.minecraft.network.chat.Component replacement = permitted && custom.getJson() != null ? custom.getJson().copy() : text;
            replacement = org.figuramc.figura.utils.TextUtils.replaceInText(replacement, "\\$\\{name\\}", text);
            var entity = net.minecraft.client.Minecraft.getInstance().level.getEntity(player.id);
            if (entity != null) {
                replacement = org.figuramc.figura.avatar.Badges.appendBadges(replacement, entity.getUUID(), config > 1);
            }
            text = replacement;
        }
        original.call(collector, pose, attachment, offset, text, seeThrough, light, camera);
    }
}
