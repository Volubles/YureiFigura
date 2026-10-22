package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import java.util.Objects;

/** stick to this invocation's collector. grabbing the world collector here is asking for shit. */
public final class FiguraCallbackContext {
    private static final ThreadLocal<SubmitNodeCollector> COLLECTOR = new ThreadLocal<>();

    private FiguraCallbackContext() {
    }

    public static SubmitNodeCollector collector() {
        return Objects.requireNonNull(COLLECTOR.get(), "Figura callback outside frame preparation");
    }

    public static void prepare(SubmitNodeCollector collector, Runnable action) {
        SubmitNodeCollector previous = COLLECTOR.get();
        COLLECTOR.set(collector);
        try {
            action.run();
        } finally {
            if (previous == null) {
                COLLECTOR.remove();
            } else {
                COLLECTOR.set(previous);
            }
        }
    }
}
