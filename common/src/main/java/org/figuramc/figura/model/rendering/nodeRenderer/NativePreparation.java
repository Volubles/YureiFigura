package org.figuramc.figura.model.rendering.nodeRenderer;

import net.minecraft.client.renderer.feature.submit.SubmitNode;
import java.util.function.Consumer;

public record NativePreparation(SubmitNode source, Consumer<PreparedNativeSubmission<?>> publish) {
}
