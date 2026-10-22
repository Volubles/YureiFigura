package org.figuramc.figura.model.rendering.nodeRenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import org.figuramc.figura.ducks.FiguraSubmitCallBackExtension;

/** always run the cleanup, even if a callback, vanilla rendering, or another cleanup craps out. */
public final class FiguraCallbacks {
    private FiguraCallbacks() {
    }

    public static void render(FiguraSubmitCallBackExtension callbacks, PoseStack pose, Runnable render) {
        Throwable primary = null;
        try {
            boolean visible = true;
            for (var callback : callbacks.figura$getPreRenderingCallbacks()) {
                visible &= callback.apply(FiguraCallbackContext.collector(), pose);
            }
            if (visible) {
                render.run();
            }
        } catch (RuntimeException | Error failure) {
            primary = failure;
            throw failure;
        } finally {
            callbacks.figura$getPreRenderingCallbacks().clear();
            Throwable cleanup = null;
            for (Runnable callback : callbacks.figura$getPostRenderingCallbacks()) {
                try {
                    callback.run();
                } catch (RuntimeException | Error failure) {
                    if (cleanup == null) {
                        cleanup = failure;
                    } else if (failure != cleanup) {
                        cleanup.addSuppressed(failure);
                    }
                }
            }
            callbacks.figura$getPostRenderingCallbacks().clear();
            if (cleanup != null && primary != null && cleanup != primary) {
                primary.addSuppressed(cleanup);
            } else if (primary == null && cleanup instanceof RuntimeException failure) {
                throw failure;
            } else if (primary == null && cleanup instanceof Error failure) {
                throw failure;
            }
        }
    }
}
