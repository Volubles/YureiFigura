package org.figuramc.figura.model.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;

import java.util.function.Consumer;

final class CapturedVertexBatch {
    // Position, RGBA, UV, overlay UV, light UV, normal, line width and setter variants.
    private static final int STRIDE = 18;
    private static final int FLOAT_COLOR = 1;
    private static final int PACKED_COLOR = 2;
    private static final int PACKED_OVERLAY = 4;
    private static final int PACKED_LIGHT = 8;
    private final int[] data;

    private CapturedVertexBatch(int[] data) {
        this.data = data;
    }

    static CapturedVertexBatch capture(int vertexCount, Consumer<VertexConsumer> writer) {
        Builder builder = new Builder(vertexCount);
        writer.accept(builder);
        return builder.finish();
    }

    void replay(VertexConsumer output) {
        for (int offset = 0; offset < data.length; offset += STRIDE) {
            int variants = data[offset + 17];
            VertexConsumer vertex = output.addVertex(readFloat(offset), readFloat(offset + 1), readFloat(offset + 2));
            if ((variants & FLOAT_COLOR) != 0) {
                vertex = vertex.setColor(readFloat(offset + 3), readFloat(offset + 4), readFloat(offset + 5), readFloat(offset + 6));
            } else if ((variants & PACKED_COLOR) != 0) {
                vertex = vertex.setColor(data[offset + 3]);
            } else {
                vertex = vertex.setColor(data[offset + 3], data[offset + 4], data[offset + 5], data[offset + 6]);
            }
            vertex = vertex.setUv(readFloat(offset + 7), readFloat(offset + 8));
            if ((variants & PACKED_OVERLAY) != 0) {
                vertex = vertex.setOverlay(data[offset + 9]);
            } else {
                vertex = vertex.setUv1(data[offset + 9], data[offset + 10]);
            }
            if ((variants & PACKED_LIGHT) != 0) {
                vertex = vertex.setLight(data[offset + 11]);
            } else {
                vertex = vertex.setUv2(data[offset + 11], data[offset + 12]);
            }
            vertex.setNormal(readFloat(offset + 13), readFloat(offset + 14), readFloat(offset + 15))
                    .setLineWidth(readFloat(offset + 16));
        }
    }

    private float readFloat(int index) {
        return Float.intBitsToFloat(data[index]);
    }

    private static final class Builder implements VertexConsumer {
        private static final int ALL_ATTRIBUTES = 63;
        private final int[] data;
        private int offset = -STRIDE;
        private int attributes;
        private boolean finished;

        private Builder(int vertexCount) {
            if (vertexCount < 0) {
                throw new IllegalArgumentException("Negative vertex count");
            }
            data = new int[Math.multiplyExact(vertexCount, STRIDE)];
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            checkWritable();
            checkAttributes();
            if (offset + STRIDE >= data.length) {
                throw new IllegalStateException("Vertex batch capacity exceeded");
            }
            offset += STRIDE;
            attributes = 0;
            data[offset + 17] = 0;
            data[offset] = Float.floatToRawIntBits(x);
            data[offset + 1] = Float.floatToRawIntBits(y);
            data[offset + 2] = Float.floatToRawIntBits(z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int r, int g, int b, int a) {
            checkVertex();
            data[offset + 3] = r;
            data[offset + 4] = g;
            data[offset + 5] = b;
            data[offset + 6] = a;
            data[offset + 17] &= ~(FLOAT_COLOR | PACKED_COLOR);
            attributes |= 1;
            return this;
        }

        @Override
        public VertexConsumer setColor(int color) {
            checkVertex();
            data[offset + 3] = color;
            data[offset + 17] = (data[offset + 17] & ~FLOAT_COLOR) | PACKED_COLOR;
            attributes |= 1;
            return this;
        }

        @Override
        public VertexConsumer setColor(float r, float g, float b, float a) {
            checkVertex();
            data[offset + 3] = Float.floatToRawIntBits(r);
            data[offset + 4] = Float.floatToRawIntBits(g);
            data[offset + 5] = Float.floatToRawIntBits(b);
            data[offset + 6] = Float.floatToRawIntBits(a);
            data[offset + 17] = (data[offset + 17] & ~PACKED_COLOR) | FLOAT_COLOR;
            attributes |= 1;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            checkVertex();
            data[offset + 7] = Float.floatToRawIntBits(u);
            data[offset + 8] = Float.floatToRawIntBits(v);
            attributes |= 2;
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            checkVertex();
            data[offset + 9] = u;
            data[offset + 10] = v;
            data[offset + 17] &= ~PACKED_OVERLAY;
            attributes |= 4;
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            checkVertex();
            data[offset + 11] = u;
            data[offset + 12] = v;
            data[offset + 17] &= ~PACKED_LIGHT;
            attributes |= 8;
            return this;
        }

        @Override
        public VertexConsumer setOverlay(int overlay) {
            checkVertex();
            data[offset + 9] = overlay;
            data[offset + 17] |= PACKED_OVERLAY;
            attributes |= 4;
            return this;
        }

        @Override
        public VertexConsumer setLight(int light) {
            checkVertex();
            data[offset + 11] = light;
            data[offset + 17] |= PACKED_LIGHT;
            attributes |= 8;
            return this;
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            checkVertex();
            data[offset + 13] = Float.floatToRawIntBits(x);
            data[offset + 14] = Float.floatToRawIntBits(y);
            data[offset + 15] = Float.floatToRawIntBits(z);
            attributes |= 16;
            return this;
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            checkVertex();
            data[offset + 16] = Float.floatToRawIntBits(width);
            attributes |= 32;
            return this;
        }

        private void checkWritable() {
            if (finished) {
                throw new IllegalStateException("Vertex batch is already captured");
            }
        }

        private void checkVertex() {
            checkWritable();
            if (offset < 0) {
                throw new IllegalStateException("No vertex has been started");
            }
        }

        private void checkAttributes() {
            if (offset >= 0 && attributes != ALL_ATTRIBUTES) {
                throw new IllegalStateException("Incomplete vertex attributes");
            }
        }

        private CapturedVertexBatch finish() {
            checkAttributes();
            if (offset + STRIDE != data.length) {
                throw new IllegalStateException("Vertex count does not match queued geometry");
            }
            finished = true;
            return new CapturedVertexBatch(data);
        }
    }
}
