package org.figuramc.figura.model.rendering.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.textures.GpuTexture;
import org.figuramc.figura.FiguraMod;
import org.figuramc.figura.avatar.Avatar;
import org.figuramc.figura.model.rendering.nodeRenderer.FiguraSubmission;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

/** hang on to the pixels and staging buffer until the gpu read is actually done. */
public final class GpuTextureReadback {
    private GpuTextureReadback() {
    }

    public static FiguraTexture read(Avatar owner, String name, GpuTexture source, int width, int height, boolean ignoreSize) {
        if (source.getFormat() != GpuFormat.RGBA8_UNORM) {
            throw new IllegalArgumentException("Unsupported texture readback format: " + source.getFormat());
        }
        NativeImage image = new NativeImage(width, height, false);
        FiguraTexture destination;
        try {
            destination = owner.registerTexture(name, image, ignoreSize);
        } catch (RuntimeException | Error failure) {
            image.close();
            throw failure;
        }
        FiguraSubmission.TextureLease lease = destination.retainForRender();
        AtomicBoolean callbackStarted = new AtomicBoolean();
        GpuBuffer staging = null;
        try {
            staging = RenderSystem.getDevice().createBuffer(() -> "Figura texture readback", 9,
                    Math.multiplyExact(Math.multiplyExact((long) width, height), 4));
            GpuBuffer buffer = staging;
            RenderSystem.getDevice().createCommandEncoder().copyTextureToBuffer(source, buffer, 0, () -> {
                callbackStarted.set(true);
                try (lease; buffer; GpuBufferSlice.MappedView view = buffer.map(true, false)) {
                    copyPixels(view.data(), image, width, height);
                    destination.update();
                } catch (RuntimeException | Error failure) {
                    FiguraMod.LOGGER.error("Texture readback failed for avatar {} texture {}", owner.owner, name, failure);
                    try (destination) {
                        owner.renderer.customTextures.remove(name, destination);
                        throw failure;
                    }
                }
            }, 0);
            return destination;
        } catch (RuntimeException | Error failure) {
            if (callbackStarted.get()) {
                // some backends finish right here. the callback already cleaned up after itself.
                throw failure;
            }
            // scheduling failed before the callback took over, so this mess is ours to clean up.
            try (destination; lease; GpuBuffer buffer = staging) {
                owner.renderer.customTextures.remove(name, destination);
                throw failure;
            }
        }
    }

    static void copyPixels(ByteBuffer source, NativeImage destination, int width, int height) {
        ByteBuffer pixels = source.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                destination.setPixelABGR(x, height - y - 1, pixels.getInt((x + y * width) * 4) | 0xff000000);
            }
        }
    }
}
