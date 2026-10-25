package org.figuramc.figura.mixin.fabric;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.FeatureRenderDispatcher;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.avatar.AvatarManager;
import org.figuramc.figura.config.Configs;
import org.figuramc.figura.math.matrix.FiguraMat3;
import org.figuramc.figura.mixin.render.PoseStackAccessor;
import org.figuramc.figura.utils.RenderUtils;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixinFabric {
    @Shadow @Final private EntityRenderDispatcher entityRenderDispatcher;

    @Shadow @Final private RenderBuffers renderBuffers;


    @Shadow
    @Final
    private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "submitEntities", at = @At("TAIL"))
    private void renderLevelFirstPerson(PoseStack stack, LevelRenderState levelRenderState, SubmitNodeCollector collector, CallbackInfo ci) {
        Camera camera = Minecraft.getInstance().gameRenderer.mainCamera();
        DeltaTracker deltaTracker = Minecraft.getInstance().getDeltaTracker();
        if (camera.isDetached())
            return;

        float tickDelta = deltaTracker.getGameTimeDeltaPartialTick(false);
        Entity e = camera.entity();
        Avatar avatar = AvatarManager.getAvatar(e);

        if (avatar == null || !(e instanceof LivingEntity livingEntity))
            return;

        EntityRenderer<LivingEntity, LivingEntityRenderState> entityRenderer = (EntityRenderer<LivingEntity, LivingEntityRenderState>) this.entityRenderDispatcher.getRenderer(livingEntity);

        LivingEntityRenderState state = entityRenderer.createRenderState(livingEntity, deltaTracker.getGameTimeDeltaPartialTick(Minecraft.getInstance().level.tickRateManager().isEntityFrozen(e)));

        // first person world parts
        SubmitNodeCollector bufferSource = collector;
        avatar.firstPersonWorldRender(e, bufferSource, stack, camera, tickDelta);

        // first person matrices
        if (Configs.FIRST_PERSON_MATRICES.value) {
            boolean previousFirstPerson = Avatar.firstPerson;
            int lastIndex = ((PoseStackAccessor)stack).getLastIndex();
            stack.pushPose();
            try {
                Avatar.firstPerson = true;
                Vec3 offset = entityRenderer.getRenderOffset(state);
                Vec3 cam = camera.position();
                stack.translate(
                        Mth.lerp(tickDelta, livingEntity.xOld, livingEntity.getX()) - cam.x() + offset.x(),
                        Mth.lerp(tickDelta, livingEntity.yOld, livingEntity.getY()) - cam.y() + offset.y(),
                        Mth.lerp(tickDelta, livingEntity.zOld, livingEntity.getZ()) - cam.z() + offset.z()
                );

                entityRenderer.submit(state, stack, collector, levelRenderState.cameraRenderState);
            } finally {
                while (((PoseStackAccessor)stack).getLastIndex() > lastIndex) {
                    stack.popPose();
                }
                Avatar.firstPerson = previousFirstPerson;
            }
        }

    }

    @Inject(method = "submitEntities", at = @At("HEAD"))
    private void applyFiguraNormals(PoseStack poseStack, LevelRenderState state, SubmitNodeCollector collector, CallbackInfo ci) {
        Avatar avatar = AvatarManager.getAvatar(Minecraft.getInstance().getCameraEntity() == null ? Minecraft.getInstance().player : Minecraft.getInstance().getCameraEntity());
        if (!RenderUtils.vanillaModelAndScript(avatar)) return;

        FiguraMat3 normal = avatar.luaRuntime.renderer.cameraNormal;
        if (normal != null)
            poseStack.last().normal().set(normal.toMatrix3f());
    }
}
