package org.figuramc.figura.mixin.render.nodeRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.apache.commons.lang3.function.TriFunction;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.ducks.NodeCollectorExtension;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraRenderCall;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(SubmitNodeStorage.class)
public abstract class SubmitNodeStorageMixin implements NodeCollectorExtension {

    @Shadow
    public abstract SubmitNodeCollection order(int i);

    @Override
    public <S extends EntityRenderState> void submitFiguraModel(Avatar avatar, S renderState, TriFunction<Avatar, S, SubmitNodeCollector, Void> renderer) {
        NodeCollectorExtension extension = (NodeCollectorExtension) this.order(0);
        extension.submitFiguraModel(avatar, renderState, renderer);
    }

    @Override
    public List<FiguraRenderCall> getFiguraRenderCalls() {
        return ((NodeCollectorExtension) this.order(0)).getFiguraRenderCalls();
    }
    @Override
    public void figura$submitGeometry(org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission submission) {
        ((NodeCollectorExtension) this.order(0)).figura$submitGeometry(submission);
    }

    @Override
    public List<org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission> figura$takeGeometry() {
        return ((NodeCollectorExtension) this.order(0)).figura$takeGeometry();
    }

    @Override
    public List<org.figuramc.figura.model.rendering.nodeRenderer.NativePreparation> figura$getNativePreparations() {
        return ((NodeCollectorExtension) this.order(0)).figura$getNativePreparations();
    }
}
