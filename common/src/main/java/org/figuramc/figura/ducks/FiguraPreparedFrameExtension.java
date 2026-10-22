package org.figuramc.figura.ducks;

import org.figuramc.figura.model.rendering.nodeRenderer.FiguraRenderFrame;

public interface FiguraPreparedFrameExtension {
    boolean figura$isActive();
    void figura$setResources(FiguraRenderFrame resources);
}
