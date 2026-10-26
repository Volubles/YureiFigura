package org.figuramc.figura.gui;

import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState;
import net.minecraft.world.entity.Entity;
import org.figuramc.figura.avatar.Avatar;
import org.joml.Matrix3x2fc;

public record FiguraHudRenderState(Avatar avatar, Entity entity, float tickDelta, int x1, int y1, Matrix3x2fc pose)
        implements PictureInPictureRenderState {
    @Override public int x0() { return 0; }
    @Override public int y0() { return 0; }
    @Override public float scale() { return 1; }
    @Override public ScreenRectangle scissorArea() { return null; }
    @Override public ScreenRectangle bounds() { return new ScreenRectangle(0, 0, x1, y1).transformMaxBounds(pose); }
}
