package net.optifine;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.GlUtil;
import java.nio.ByteBuffer;
import java.util.Properties;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.optifine.util.TextureUtils;
import org.lwjgl.opengl.GL11;

public class TextureAnimation {
    private String srcTex = null;
    private String dstTex = null;
    ResourceLocation dstTexLoc = null;
    private int dstTextId = -1;
    private int dstX = 0;
    private int dstY = 0;
    private int frameWidth = 0;
    private int frameHeight = 0;
    private TextureAnimationFrame[] frames = null;
    private int currentFrameIndex = 0;
    private boolean interpolate = false;
    private int interpolateSkip = 0;
    private ByteBuffer interpolateData = null;
    byte[] srcData = null;
    private ByteBuffer imageData = null;
    private boolean active = true;
    private boolean valid = true;

    public TextureAnimation(
        String texFrom, byte[] srcData, String texTo, ResourceLocation locTexTo, int dstX, int dstY, int frameWidth, int frameHeight, Properties props
    ) {
        this.srcTex = texFrom;
        this.dstTex = texTo;
        this.dstTexLoc = locTexTo;
        this.dstX = dstX;
        this.dstY = dstY;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        int i = frameWidth * frameHeight * 4;
        if (srcData.length % i != 0) {
            Config.warn("Invalid animated texture length: " + srcData.length + ", frameWidth: " + frameWidth + ", frameHeight: " + frameHeight);
        }

        this.srcData = srcData;
        int j = srcData.length / i;
        if (props.get("tile.0") != null) {
            for (int k = 0; props.get("tile." + k) != null; k++) {
                j = k + 1;
            }
        }

        String s2 = (String)props.get("duration");
        int l = Math.max(Config.parseInt(s2, 1), 1);
        this.frames = new TextureAnimationFrame[j];

        for (int i1 = 0; i1 < this.frames.length; i1++) {
            String s = (String)props.get("tile." + i1);
            int j1 = Config.parseInt(s, i1);
            String s1 = (String)props.get("duration." + i1);
            int k1 = Math.max(Config.parseInt(s1, l), 1);
            TextureAnimationFrame textureanimationframe = new TextureAnimationFrame(j1, k1);
            this.frames[i1] = textureanimationframe;
        }

        this.interpolate = Config.parseBoolean(props.getProperty("interpolate"), false);
        this.interpolateSkip = Config.parseInt(props.getProperty("skip"), 0);
        if (this.interpolate) {
            this.interpolateData = GlUtil.allocateMemory(i);
        }
    }

    public boolean nextFrame() {
        TextureAnimationFrame textureanimationframe = this.getCurrentFrame();
        if (textureanimationframe == null) {
            return false;
        } else {
            textureanimationframe.counter++;
            if (textureanimationframe.counter < textureanimationframe.duration) {
                return this.interpolate;
            } else {
                textureanimationframe.counter = 0;
                this.currentFrameIndex++;
                if (this.currentFrameIndex >= this.frames.length) {
                    this.currentFrameIndex = 0;
                }

                return true;
            }
        }
    }

    public TextureAnimationFrame getCurrentFrame() {
        return this.getFrame(this.currentFrameIndex);
    }

    public TextureAnimationFrame getFrame(int index) {
        if (this.frames.length <= 0) {
            return null;
        } else {
            if (index < 0 || index >= this.frames.length) {
                index = 0;
            }

            return this.frames[index];
        }
    }

    public int getFrameCount() {
        return this.frames.length;
    }

    public void updateTexture() {
        if (this.valid) {
            if (this.dstTextId < 0) {
                AbstractTexture abstracttexture = TextureUtils.getTexture(this.dstTexLoc);
                if (abstracttexture == null) {
                    this.valid = false;
                    return;
                }

                this.dstTextId = abstracttexture.getId();
            }

            if (this.imageData == null) {
                this.imageData = GlUtil.allocateMemory(this.srcData.length);
                this.imageData.put(this.srcData);
                this.imageData.flip();
                this.srcData = null;
            }

            this.active = SmartAnimations.isActive() ? SmartAnimations.isTextureRendered(this.dstTextId) : true;
            if (this.nextFrame()) {
                if (this.active) {
                    int j = this.frameWidth * this.frameHeight * 4;
                    TextureAnimationFrame textureanimationframe = this.getCurrentFrame();
                    if (textureanimationframe != null) {
                        int i = j * textureanimationframe.index;
                        if (i + j <= this.imageData.limit()) {
                            if (!this.interpolate || textureanimationframe.counter <= 0) {
                                this.imageData.position(i);
                                GlStateManager._bindTexture(this.dstTextId);
                                TextureUtils.resetDataUnpacking();
                                GL11.glTexSubImage2D(3553, 0, this.dstX, this.dstY, this.frameWidth, this.frameHeight, 6408, 5121, this.imageData);
                            } else if (this.interpolateSkip <= 1 || textureanimationframe.counter % this.interpolateSkip == 0) {
                                TextureAnimationFrame textureanimationframe1 = this.getFrame(this.currentFrameIndex + 1);
                                double d0 = 1.0 * (double)textureanimationframe.counter / (double)textureanimationframe.duration;
                                this.updateTextureInerpolate(textureanimationframe, textureanimationframe1, d0);
                            }
                        }
                    }
                }
            }
        }
    }

    private void updateTextureInerpolate(TextureAnimationFrame frame1, TextureAnimationFrame frame2, double k) {
        int i = this.frameWidth * this.frameHeight * 4;
        int j = i * frame1.index;
        if (j + i <= this.imageData.limit()) {
            int kx = i * frame2.index;
            if (kx + i <= this.imageData.limit()) {
                this.interpolateData.clear();

                for (int l = 0; l < i; l++) {
                    int i1 = this.imageData.get(j + l) & 255;
                    int j1 = this.imageData.get(kx + l) & 255;
                    int k1 = this.mix(i1, j1, k);
                    byte b0 = (byte)k1;
                    this.interpolateData.put(b0);
                }

                this.interpolateData.flip();
                GlStateManager._bindTexture(this.dstTextId);
                TextureUtils.resetDataUnpacking();
                GL11.glTexSubImage2D(3553, 0, this.dstX, this.dstY, this.frameWidth, this.frameHeight, 6408, 5121, this.interpolateData);
            }
        }
    }

    private int mix(int col1, int col2, double k) {
        return (int)((double)col1 * (1.0 - k) + (double)col2 * k);
    }

    public String getSrcTex() {
        return this.srcTex;
    }

    public String getDstTex() {
        return this.dstTex;
    }

    public ResourceLocation getDstTexLoc() {
        return this.dstTexLoc;
    }

    public boolean isActive() {
        return this.active;
    }
}