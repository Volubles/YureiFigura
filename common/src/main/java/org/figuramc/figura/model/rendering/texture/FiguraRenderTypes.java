package org.figuramc.figura.model.rendering.texture;

import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.renderpearl.api.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;

import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.util.TriState;
import net.minecraft.util.Util;
import org.figuramc.figura.utils.FiguraIdentifier;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.BiFunction;
import java.util.function.Function;

public enum FiguraRenderTypes {
    NONE(null),

    CUTOUT(RenderTypes::entityCutout),
    CUTOUT_CULL(RenderTypes::entityCutoutCull),
    CUTOUT_EMISSIVE_SOLID(resourceLocation -> FiguraRenderType.CUTOUT_EMISSIVE_SOLID.apply(resourceLocation, true)),

    TRANSLUCENT(RenderTypes::entityTranslucent),
    TRANSLUCENT_CULL(RenderTypes::entityTranslucentCull),

    EMISSIVE(RenderTypes::eyes),
    EMISSIVE_SOLID(resourceLocation -> RenderTypes.beaconBeam(resourceLocation, false)),
    EYES(RenderTypes::eyes),

    END_PORTAL(t -> RenderTypes.endPortal(), false),
    END_GATEWAY(t -> RenderTypes.endGateway(), false),
    TEXTURED_PORTAL(FiguraRenderType.TEXTURED_PORTAL),

    GLINT(t -> FiguraRenderType.ENTITY_GLINT, false, false),
    GLINT2(t -> FiguraRenderType.ITEM_GLINT, false, false),
    TEXTURED_GLINT(FiguraRenderType.TEXTURED_GLINT, true, false),

    LINES(t -> RenderTypes.lines(), false),
    LINES_STRIP(t -> RenderTypes.lines(), false),
    SOLID(t -> FiguraRenderType.SOLID, false),

    BLURRY(FiguraRenderType.BLURRY);

    private final Function<Identifier, RenderType> func;
    private final boolean texture, offset;

    FiguraRenderTypes(Function<Identifier, RenderType> func) {
        this(func, true);
    }

    FiguraRenderTypes(Function<Identifier, RenderType> func, boolean texture) {
        this(func, texture, true);
    }

    FiguraRenderTypes(Function<Identifier, RenderType> func, boolean texture, boolean offset) {
        this.func = func;
        this.texture = texture;
        this.offset = offset;
    }

    public boolean isOffset() {
        return offset;
    }

    public RenderType get(Identifier id) {
        if (!texture)
            return func.apply(id);

        return id == null || func == null ? null : func.apply(id);
    }

    public abstract static class FiguraRenderType extends RenderType {
        public FiguraRenderType(String name, RenderSetup setup) {
            super(name, setup);
        }

        public static final RenderType SOLID = create(
                "figura_solid",
                RenderSetup.builder(FiguraRenderPipelines.FIGURA_SOLID).setOitPipelines(FiguraRenderPipelines.FIGURA_SOLID_OIT)
                        .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
                        .setOutline(RenderSetup.OutlineProperty.NONE)
                        .createRenderSetup()
        );

        public static final RenderType ENTITY_GLINT = create("figura_entity_glint", RenderSetup.builder(RenderPipelines.GLINT).withForcedSolidModelPhase()
                .withTexture("Sampler0", net.minecraft.client.renderer.feature.ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
                .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING).createRenderSetup());
        public static final RenderType ITEM_GLINT = create("figura_item_glint", RenderSetup.builder(RenderPipelines.GLINT).withForcedSolidModelPhase()
                .withTexture("Sampler0", net.minecraft.client.renderer.feature.ItemFeatureRenderer.ENCHANTED_GLINT_ITEM)
                .setTextureTransform(TextureTransform.GLINT_TEXTURING).createRenderSetup());

        private static final BiFunction<Identifier, Boolean, RenderType> CUTOUT_EMISSIVE_SOLID = Util.memoize(
                (texture, affectsOutline) ->
                        create("figura_cutout_emissive_solid",
                                RenderSetup.builder(RenderPipelines.BEACON_BEAM_TRANSLUCENT).setOitPipelines(RenderPipelines.OIT_BEACON_BEAM)
                                        .withTexture("Sampler0", texture)
                                        .affectsCrumbling()
                                        .sortOnUpload()
                                        .useOverlay()
                                        .setOutline(affectsOutline ? RenderSetup.OutlineProperty.AFFECTS_OUTLINE : RenderSetup.OutlineProperty.NONE)
                                        .createRenderSetup()
                        )
        );


        public static final Function<Identifier, RenderType> TEXTURED_PORTAL = Util.memoize(
                texture -> create(
                        "figura_textured_portal",
                        RenderSetup.builder(RenderPipelines.END_GATEWAY)
                                .withTexture("Sampler0", texture)
                                .withTexture("Sampler1", texture)
                                .setOutline(RenderSetup.OutlineProperty.NONE)
                                .createRenderSetup()
                )
        );

        public static final Function<Identifier, RenderType> BLURRY = Util.memoize(
                texture -> create(
                        "figura_blurry",
                        RenderSetup.builder(RenderPipelines.ENTITY_TRANSLUCENT).setOitPipelines(RenderPipelines.OIT_ENTITY)
                                .affectsCrumbling()
                                .sortOnUpload()
                                .useLightmap()
                                .useOverlay()
                                .withTexture("Sampler0", texture, () -> {
                                    var sampler = Minecraft.getInstance().getTextureManager().getTexture(texture).getSampler();
                                    return RenderSystem.getSamplerCache().getSampler(sampler.getAddressModeU(), sampler.getAddressModeV(),
                                            FilterMode.LINEAR, FilterMode.LINEAR, sampler.getMaxLod().isEmpty());
                                })
                                .setOutline(RenderSetup.OutlineProperty.AFFECTS_OUTLINE)
                                .createRenderSetup()
                )
        );

        public static final Function<Identifier, RenderType> TEXTURED_GLINT = Util.memoize(
                texture -> create(
                        "figura_textured_glint_direct",
                        RenderSetup.builder(RenderPipelines.GLINT).withForcedSolidModelPhase()
                                .withTexture("Sampler0", texture)
                                .setTextureTransform(TextureTransform.ENTITY_GLINT_TEXTURING)
                                .createRenderSetup()
                )
        );
    }

    public static class FiguraRenderPipelines extends RenderPipelines {
        private static RenderPipeline.Builder solidBuilder() {
            return RenderPipeline.builder(MATRICES_FOG_SNIPPET)
                    .withVertexShader(new FiguraIdentifier("core/solid"))
                    .withFragmentShader("core/rendertype_lines")
                    .withCull(false)
                    .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
                    .withPrimitiveTopology(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.QUADS);
        }

        public static final RenderPipeline FIGURA_SOLID = register(solidBuilder()
                .withLocation(new FiguraIdentifier("pipeline/solid"))
                .withColorTargetState(new com.mojang.renderpearl.api.pipeline.ColorTargetState(BlendFunction.TRANSLUCENT))
                .withDepthStencilState(com.mojang.renderpearl.api.pipeline.DepthStencilState.DEFAULT).build());
        public static final net.minecraft.client.renderer.oit.OitPipelineSet FIGURA_SOLID_OIT = register(
                net.minecraft.client.renderer.oit.OitPipelineSet.builder("figura_solid", solidBuilder()).build());
    }
}
