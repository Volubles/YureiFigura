package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;

/** nested tasks keep their relative order and land in the same owning storage. */
public final class RelativeSubmitNodeCollector extends SubmitNodeStorage {
    private final SubmitNodeStorage owner;
    private final int baseOrder;

    public RelativeSubmitNodeCollector(SubmitNodeStorage owner, int baseOrder) {
        this.owner = owner;
        this.baseOrder = baseOrder;
    }

    @Override
    public SubmitNodeCollection order(int relativeOrder) {
        return owner.order(Math.addExact(baseOrder, relativeOrder));
    }
}
