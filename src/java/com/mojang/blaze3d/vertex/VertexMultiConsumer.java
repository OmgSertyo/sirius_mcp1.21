package com.mojang.blaze3d.vertex;

import java.util.function.Consumer;
import net.optifine.render.VertexBuilderWrapper;

public class VertexMultiConsumer {
    public static VertexConsumer create() {
        throw new IllegalArgumentException();
    }

    public static VertexConsumer create(VertexConsumer pConsumer) {
        return pConsumer;
    }

    public static VertexConsumer create(VertexConsumer pFirst, VertexConsumer pSecond) {
        return new VertexMultiConsumer.Double(pFirst, pSecond);
    }

    public static VertexConsumer create(VertexConsumer... pDelegates) {
        return new VertexMultiConsumer.Multiple(pDelegates);
    }

    static class Double extends VertexBuilderWrapper implements VertexConsumer {
        private final VertexConsumer first;
        private final VertexConsumer second;
        private boolean fixMultitextureUV;

        public Double(VertexConsumer pFirst, VertexConsumer pSecond) {
            super(pSecond);
            if (pFirst == pSecond) {
                throw new IllegalArgumentException("Duplicate delegates");
            } else {
                this.first = pFirst;
                this.second = pSecond;
                this.updateFixMultitextureUv();
            }
        }

        @Override
        public VertexConsumer addVertex(float pX, float pY, float pZ) {
            this.first.addVertex(pX, pY, pZ);
            this.second.addVertex(pX, pY, pZ);
            return this;
        }


        @Override
        public VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
            this.first.setColor(pRed, pGreen, pBlue, pAlpha);
            this.second.setColor(pRed, pGreen, pBlue, pAlpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float pU, float pV) {
            this.first.setUv(pU, pV);
            this.second.setUv(pU, pV);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int pU, int pV) {
            this.first.setUv1(pU, pV);
            this.second.setUv1(pU, pV);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int pU, int pV) {
            this.first.setUv2(pU, pV);
            this.second.setUv2(pU, pV);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ) {
            this.first.setNormal(pNormalX, pNormalY, pNormalZ);
            this.second.setNormal(pNormalX, pNormalY, pNormalZ);
            return this;
        }

        @Override
        public void addVertex(
            float pX,
            float pY,
            float pZ,
            int pColor,
            float pU,
            float pV,
            int pPackedOverlay,
            int pPackedLight,
            float pNormalX,
            float pNormalY,
            float pNormalZ
        ) {
            if (this.fixMultitextureUV) {
                this.first
                    .addVertex(
                        pX, pY, pZ, pColor, pU / 32.0F, pV / 32.0F, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ
                    );
            } else {
                this.first
                    .addVertex(pX, pY, pZ, pColor, pU, pV, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ);
            }

            this.second.addVertex(pX, pY, pZ, pColor, pU, pV, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ);
        }

        private void updateFixMultitextureUv() {
            this.fixMultitextureUV = !this.first.isMultiTexture() && this.second.isMultiTexture();
        }

        @Override
        public VertexConsumer getSecondaryBuilder() {
            return this.first;
        }
    }

    static class Multiple extends VertexBuilderWrapper implements VertexConsumer {
        private VertexConsumer[] delegates;

        Multiple(VertexConsumer[] delegates) {
            super(delegates.length > 0 ? delegates[0] : null);
            this.delegates = delegates;

            for (int i = 0; i < delegates.length; i++) {
                for (int j = i + 1; j < delegates.length; j++) {
                    if (delegates[i] == delegates[j]) {
                        throw new IllegalArgumentException("Duplicate delegates");
                    }
                }
            }

            this.delegates = delegates;
        }

        private void forEach(Consumer<VertexConsumer> pAction) {
            for (VertexConsumer vertexconsumer : this.delegates) {
                pAction.accept(vertexconsumer);
            }
        }

        @Override
        public VertexConsumer addVertex(float pX, float pY, float pZ) {
            this.forEach(consumerIn -> consumerIn.addVertex(pX, pY, pZ));
            return this;
        }


        @Override
        public VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
            this.forEach(consumerIn -> consumerIn.setColor(pRed, pGreen, pBlue, pAlpha));
            return this;
        }

        @Override
        public VertexConsumer setUv(float pU, float pV) {
            this.forEach(consumerIn -> consumerIn.setUv(pU, pV));
            return this;
        }

        @Override
        public VertexConsumer setUv1(int pU, int pV) {
            this.forEach(consumerIn -> consumerIn.setUv1(pU, pV));
            return this;
        }

        @Override
        public VertexConsumer setUv2(int pU, int pV) {
            this.forEach(consumerIn -> consumerIn.setUv2(pU, pV));
            return this;
        }

        @Override
        public VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ) {
            this.forEach(consumerIn -> consumerIn.setNormal(pNormalX, pNormalY, pNormalZ));
            return this;
        }

        @Override
        public void addVertex(
            float pX,
            float pY,
            float pZ,
            int pColor,
            float pU,
            float pV,
            int pPackedOverlay,
            int pPackedLight,
            float pNormalX,
            float pNormalY,
            float pNormalZ
        ) {
            this.forEach(
                consumerIn -> consumerIn.addVertex(
                        pX, pY, pZ, pColor, pU, pV, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ
                    )
            );
        }
    }
}