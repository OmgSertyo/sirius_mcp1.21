package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.SmartAnimations;
import net.optifine.render.VertexBuilderWrapper;

public class SpriteCoordinateExpander extends VertexBuilderWrapper implements VertexConsumer {
    private final VertexConsumer delegate;
    private final TextureAtlasSprite sprite;

    public SpriteCoordinateExpander(VertexConsumer pDelegate, TextureAtlasSprite pSprite) {
        super(pDelegate);
        if (SmartAnimations.isActive()) {
            SmartAnimations.spriteRendered(pSprite);
        }

        this.delegate = pDelegate;
        this.sprite = pSprite;
    }

    @Override
    public VertexConsumer addVertex(float pX, float pY, float pZ) {
        this.delegate.addVertex(pX, pY, pZ);
        return this;
    }

    @Override
    public VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha) {
        this.delegate.setColor(pRed, pGreen, pBlue, pAlpha);
        return this;
    }

    @Override
    public VertexConsumer setUv(float pU, float pV) {
        this.delegate.setUv(this.sprite.getU(pU), this.sprite.getV(pV));
        return this;
    }

    @Override
    public VertexConsumer setUv1(int pU, int pV) {
        this.delegate.setUv1(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer setUv2(int pU, int pV) {
        this.delegate.setUv2(pU, pV);
        return this;
    }

    @Override
    public VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ) {
        this.delegate.setNormal(pNormalX, pNormalY, pNormalZ);
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
        if (this.delegate.isMultiTexture()) {
            this.delegate.putSprite(this.sprite);
            this.delegate.addVertex(pX, pY, pZ, pColor, pU, pV, pPackedOverlay, pPackedLight, pNormalX, pNormalY, pNormalZ);
        } else {
            this.delegate
                .addVertex(
                    pX,
                    pY,
                    pZ,
                    pColor,
                    this.sprite.getU(pU),
                    this.sprite.getV(pV),
                    pPackedOverlay,
                    pPackedLight,
                    pNormalX,
                    pNormalY,
                    pNormalZ
                );
        }
    }

    @Override
    public boolean canAddVertexFast() {
        return this.delegate.canAddVertexFast();
    }

    @Override
    public void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals) {
        if (this.delegate.isMultiTexture()) {
            this.delegate.putSprite(this.sprite);
            this.delegate.addVertexFast(x, y, z, color, texU, texV, overlayUV, lightmapUV, normals);
        } else {
            float f = this.sprite.getU(texU);
            float f1 = this.sprite.getV(texV);
            this.delegate.addVertexFast(x, y, z, color, f, f1, overlayUV, lightmapUV, normals);
        }
    }
}