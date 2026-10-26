
package org.figuramc.figura.ducks;

import org.figuramc.figura.model.rendering.EntityRenderMode;

import java.util.UUID;

public interface GuiEntityRenderStateExtension {

    double getXPos();
    void setXPos(double xPos);

    double getYPos();
    void setYPos(double yPos);

    float getFireRotation();
    void setFireRotation(float rotation);

    float getDollScale();
    void setDollScale(float scale);

    EntityRenderMode getRenderMode();
    void setRenderMode(EntityRenderMode renderMode);
}
