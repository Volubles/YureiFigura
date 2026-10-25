package org.figuramc.figura.mixin.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.GameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.ducks.GameRendererAccessor;
import org.figuramc.figura.ducks.CameraProjectionAccess;
import org.figuramc.figura.gui.FiguraPortraitRenderer;
import org.figuramc.figura.gui.FiguraHudRenderer;
import org.figuramc.figura.lua.api.ClientAPI;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin implements GameRendererAccessor {
    @Shadow @Final private Minecraft minecraft;
    @Shadow @Final private GuiRenderer guiRenderer;
    @Shadow @Final private GameRenderState gameRenderState;
    @Shadow @Final private List<Identifier> requestedPostEffects;
    @Shadow protected abstract void bobHurt(CameraRenderState camera, PoseStack pose);
    @Shadow protected abstract void bobView(CameraRenderState camera, PoseStack pose);
    @Unique private boolean figura$viewEffects;

    @Inject(method = "update", at = @At("RETURN"))
    private void figura$postEffect(DeltaTracker delta, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(minecraft.getCameraEntity());
        if (RenderUtils.vanillaModelAndScript(avatar) && avatar.luaRuntime.renderer.postShader != null) {
            Identifier effect = avatar.luaRuntime.renderer.postShader;
            if (!requestedPostEffects.contains(effect)) {
                requestedPostEffects.add(effect);
            }
        }
    }

    // bobbing fix courtesy of Iris; https://github.com/IrisShaders/Iris/blob/1.20.1/src/main/java/net/irisshaders/iris/mixin/MixinModelViewBobbing.java
    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void figura$viewEffects(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(minecraft.getCameraEntity() == null ? minecraft.player : minecraft.getCameraEntity());
        figura$viewEffects = RenderUtils.vanillaModelAndScript(avatar) && !ClientAPI.hasShaderPack();
        if (!figura$viewEffects) {
            return;
        }
        CameraRenderState camera = gameRenderState.levelRenderState.cameraRenderState;
        var options = gameRenderState.optionsRenderState;
        var player = gameRenderState.levelRenderState.playerRenderState;
        PoseStack pose = new PoseStack();
        float roll = 0;
        if (avatar.luaRuntime.renderer.cameraRot != null) {
            roll += (float) avatar.luaRuntime.renderer.cameraRot.z;
        }
        if (avatar.luaRuntime.renderer.cameraOffsetRot != null) {
            roll += (float) avatar.luaRuntime.renderer.cameraOffsetRot.z;
        }
        pose.last().pose().rotateZ((float) Math.toRadians(roll));
        if (avatar.luaRuntime.renderer.cameraMat != null) {
            pose.last().pose().set(avatar.luaRuntime.renderer.cameraMat.toMatrix4f());
        }
        bobHurt(camera, pose);
        if (options.bobView) {
            bobView(camera, pose);
        }
        float intensity = Math.max(player.portalEffectIntensity, player.nauseaEffectIntensity) * options.screenEffectScale * options.screenEffectScale;
        if (intensity > 0) {
            float skew = 5f / (intensity * intensity + 5f) - intensity * 0.04f;
            skew *= skew;
            Vector3f axis = new Vector3f(0, (float) Math.sqrt(2) / 2, (float) Math.sqrt(2) / 2);
            float angle = (float) Math.toRadians(player.spinningEffectAngle);
            pose.last().pose().rotate(angle, axis).scale(1f / skew, 1, 1).rotate(-angle, axis);
        }
        camera.viewRotationMatrix.set(pose.last().pose()).rotate(camera.orientation.conjugate(new Quaternionf()));
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void figura$projectionHurt(GameRenderer renderer, CameraRenderState camera, PoseStack pose, Operation<Void> original) {
        if (!figura$viewEffects) {
            original.call(renderer, camera, pose);
        }
    }

    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V"))
    private void figura$projectionBob(GameRenderer renderer, CameraRenderState camera, PoseStack pose, Operation<Void> original) {
        if (!figura$viewEffects) {
            original.call(renderer, camera, pose);
        }
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"))
    private float figura$projectionConfusion(float original) {
        return figura$viewEffects ? 0 : original;
    }

    @ModifyArg(method = "<init>", index = 2, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;<init>(Lnet/minecraft/client/renderer/state/gui/GuiRenderState;Lnet/minecraft/client/renderer/feature/FeatureRenderDispatcher;Ljava/util/List;)V"))
    private List<PictureInPictureRenderer<?>> figura$guiRenderers(List<PictureInPictureRenderer<?>> original) {
        List<PictureInPictureRenderer<?>> renderers = new ArrayList<>(original);
        renderers.add(new FiguraPortraitRenderer());
        renderers.add(new FiguraHudRenderer());
        return renderers;
    }

    @Override
    public double figura$getFov(Camera camera, float delta, boolean changingFov) {
        return ((CameraProjectionAccess) camera).figura$getFov(delta, changingFov);
    }

    @Override
    public GuiRenderer figura$getGuiRenderer() {
        return guiRenderer;
    }
}
