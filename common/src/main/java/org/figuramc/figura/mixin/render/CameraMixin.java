package org.figuramc.figura.mixin.render;

import net.minecraft.client.Camera;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.joml.Matrix4f;
import org.joml.Quaternionfc;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.math.vector.FiguraVec3;
import org.figuramc.figura.utils.RenderUtils;
import org.figuramc.figura.utils.ui.UIHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin implements org.figuramc.figura.ducks.CameraProjectionAccess {

    @Shadow private float xRot;
    @Shadow private float yRot;

    @Unique private Avatar avatar;
    @Shadow private Entity entity;
    @Shadow private float fovModifier;
    @Shadow protected abstract float calculateFov(float partialTicks);
    @Shadow protected abstract float calculateHudFov(float partialTicks);

    @Override
    public double figura$getFov(float delta, boolean changingFov) {
        return changingFov ? calculateFov(delta) : calculateHudFov(delta);
    }

    @Shadow protected abstract void setRotation(float yaw, float pitch);
    @Shadow protected abstract void move(float x, float y, float z);

    @Inject(method = "alignWithEntity", at = @At(value = "HEAD"))
    private void setupAvatarVar(float delta, CallbackInfo ci) {
        avatar = AvatarManager.getAvatar(entity);
    }

    // Neo adds roll in addition to pitch and yaw
    @Inject(method = "alignWithEntity", at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setRotation(FF)V", shift = At.Shift.AFTER)}, require = 1)
    private void setupRot(float delta, CallbackInfo ci) {
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            avatar = null;
            return;
        }

        float x = xRot;
        float y = yRot;

        FiguraVec3 rot = avatar.luaRuntime.renderer.cameraRot;
        if (rot != null && rot.notNaN()) {
            x = (float) rot.x;
            y = (float) rot.y;
        }

        FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetRot;
        if (offset != null && offset.notNaN()) {
            x += (float) offset.x;
            y += (float) offset.y;
        }

        setRotation(y, x);
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 0)
    private double setupPivotX(double originalX) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            double x = originalX;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                x = piv.x;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                x += offset.x;
            }
            return x;
        }
        return originalX;
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 1)
    private double setupPivotY(double originalY) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            double y = originalY;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                y = piv.y;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                y += offset.y;
            }
            return y;
        }
        return originalY;
    }

    @ModifyArg(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(DDD)V"), index = 2)
    private double setupPivotZ(double originalZ) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            double z = originalZ;

            FiguraVec3 piv = avatar.luaRuntime.renderer.cameraPivot;
            if (piv != null && piv.notNaN()) {
                z = piv.z;
            }

            FiguraVec3 offset = avatar.luaRuntime.renderer.cameraOffsetPivot;
            if (offset != null && offset.notNaN()) {
                z += offset.z;
            }
            return z;
        }
        return originalZ;
    }


    @Inject(method = "alignWithEntity", at = @At(value = "RETURN"))
    private void setupPos(float delta, CallbackInfo ci) {
        if (RenderUtils.vanillaModelAndScript(avatar)) {
            FiguraVec3 pos = avatar.luaRuntime.renderer.cameraPos;
            if (pos != null && pos.notNaN())
                move((float) -pos.z, (float) pos.y, (float) -pos.x);

            avatar = null;
        }
    }

    @WrapOperation(method = "alignWithEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;setPosition(Lnet/minecraft/world/phys/Vec3;)V"))
    private void figura$cartPivot(Camera camera, Vec3 pos, Operation<Void> original) {
        original.call(camera, new Vec3(setupPivotX(pos.x), setupPivotY(pos.y), setupPivotZ(pos.z)));
    }

    @Inject(method = "tickFov", at = @At("RETURN"))
    private void figura$fov(CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (RenderUtils.vanillaModelAndScript(avatar) && avatar.luaRuntime.renderer.fov != null) {
            fovModifier = avatar.luaRuntime.renderer.fov;
        }
    }

    @WrapOperation(method = "getViewRotationMatrix", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;rotation(Lorg/joml/Quaternionfc;)Lorg/joml/Matrix4f;"))
    private Matrix4f figura$view(Matrix4f matrix, Quaternionfc rotation, Operation<Matrix4f> original) {
        Avatar avatar = AvatarManager.getAvatar(entity);
        if (!RenderUtils.vanillaModelAndScript(avatar)) {
            return original.call(matrix, rotation);
        }
        float roll = 0;
        if (avatar.luaRuntime.renderer.cameraRot != null) {
            roll += (float) avatar.luaRuntime.renderer.cameraRot.z;
        }
        if (avatar.luaRuntime.renderer.cameraOffsetRot != null) {
            roll += (float) avatar.luaRuntime.renderer.cameraOffsetRot.z;
        }
        matrix.identity().rotateZ((float) Math.toRadians(roll));
        if (avatar.luaRuntime.renderer.cameraMat != null) {
            matrix.set(avatar.luaRuntime.renderer.cameraMat.toMatrix4f());
        }
        return matrix.rotate(rotation);
    }

    @Inject(method = "xRot", at = @At("HEAD"), cancellable = true)
    private void xRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }

    @Inject(method = "yRot", at = @At("HEAD"), cancellable = true)
    private void yRot(CallbackInfoReturnable<Float> cir) {
        if (UIHelper.paperdoll)
            cir.setReturnValue(0f);
    }
}
