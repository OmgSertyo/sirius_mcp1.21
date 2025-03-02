package net.optifine.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.optifine.Config;
import net.optifine.util.IntArray;
import net.optifine.util.TextureUtils;

public class MultiTextureBuilder {
    private int vertexCount;
    private RenderType blockLayer;
    private TextureAtlasSprite[] quadSprites;
    private boolean reorderingAllowed;
    private boolean[] drawnIcons = new boolean[256];
    private List<SpriteRenderData> spriteRenderDatas = new ArrayList<>();
    private IntArray vertexPositions = new IntArray(16);
    private IntArray vertexCounts = new IntArray(16);

    public MultiTextureData build(int vertexCountIn, RenderType blockLayerIn, TextureAtlasSprite[] quadSpritesIn, int[] quadOrderingIn) {
        if (quadSpritesIn == null) {
            return null;
        } else {
            SpriteRenderData[] aspriterenderdata = this.buildRenderDatas(vertexCountIn, blockLayerIn, quadSpritesIn, quadOrderingIn);
            if (aspriterenderdata == null) {
                return null;
            } else {
                MultiTextureData multitexturedata = new MultiTextureData(aspriterenderdata);
                if (this.blockLayer.isNeedsSorting()) {
                    int i = vertexCountIn / 4;
                    TextureAtlasSprite[] atextureatlassprite = Arrays.copyOfRange(quadSpritesIn, 0, i);
                    multitexturedata.setResortParameters(vertexCountIn, blockLayerIn, atextureatlassprite);
                }

                return multitexturedata;
            }
        }
    }

    public SpriteRenderData[] buildRenderDatas(int vertexCountIn, RenderType blockLayerIn, TextureAtlasSprite[] quadSpritesIn, int[] quadOrderingIn) {
        if (quadSpritesIn == null) {
            return null;
        } else {
            this.vertexCount = vertexCountIn;
            this.blockLayer = blockLayerIn;
            this.quadSprites = quadSpritesIn;
            this.reorderingAllowed = !this.blockLayer.isNeedsSorting();
            int i = Config.getTextureMap().getCountRegisteredSprites();
            if (this.drawnIcons.length <= i) {
                this.drawnIcons = new boolean[i + 1];
            }

            Arrays.fill(this.drawnIcons, false);
            this.spriteRenderDatas.clear();
            int j = 0;
            int k = -1;
            int l = this.vertexCount / 4;

            for (int i1 = 0; i1 < l; i1++) {
                int j1 = quadOrderingIn != null ? quadOrderingIn[i1] : i1;
                TextureAtlasSprite textureatlassprite = this.quadSprites[j1];
                if (textureatlassprite != null) {
                    int k1 = textureatlassprite.getIndexInMap();
                    if (k1 >= this.drawnIcons.length) {
                        this.drawnIcons = Arrays.copyOf(this.drawnIcons, k1 + 1);
                    }

                    if (!this.drawnIcons[k1]) {
                        if (textureatlassprite == TextureUtils.iconGrassSideOverlay) {
                            if (k < 0) {
                                k = i1;
                            }
                        } else {
                            i1 = this.drawForIcon(textureatlassprite, i1, quadOrderingIn) - 1;
                            j++;
                            if (this.reorderingAllowed) {
                                this.drawnIcons[k1] = true;
                            }
                        }
                    }
                }
            }

            if (k >= 0) {
                this.drawForIcon(TextureUtils.iconGrassSideOverlay, k, quadOrderingIn);
                j++;
            }

            return this.spriteRenderDatas.toArray(new SpriteRenderData[this.spriteRenderDatas.size()]);
        }
    }

    private int drawForIcon(TextureAtlasSprite sprite, int startQuadPos, int[] quadOrderingIn) {
        this.vertexPositions.clear();
        this.vertexCounts.clear();
        int i = -1;
        int j = -1;
        int k = this.vertexCount / 4;

        for (int l = startQuadPos; l < k; l++) {
            int i1 = quadOrderingIn != null ? quadOrderingIn[l] : l;
            TextureAtlasSprite textureatlassprite = this.quadSprites[i1];
            if (textureatlassprite == sprite) {
                if (j < 0) {
                    j = l;
                }
            } else if (j >= 0) {
                this.draw(j, l);
                if (!this.reorderingAllowed) {
                    this.spriteRenderDatas.add(new SpriteRenderData(sprite, this.vertexPositions.toIntArray(), this.vertexCounts.toIntArray()));
                    return l;
                }

                j = -1;
                if (i < 0) {
                    i = l;
                }
            }
        }

        if (j >= 0) {
            this.draw(j, k);
        }

        if (i < 0) {
            i = k;
        }

        this.spriteRenderDatas.add(new SpriteRenderData(sprite, this.vertexPositions.toIntArray(), this.vertexCounts.toIntArray()));
        return i;
    }

    private void draw(int startQuadVertex, int endQuadVertex) {
        int i = endQuadVertex - startQuadVertex;
        if (i > 0) {
            int j = startQuadVertex * 4;
            int k = i * 4;
            this.vertexPositions.put(j);
            this.vertexCounts.put(k);
        }
    }
}