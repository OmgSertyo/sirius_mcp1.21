package com.mojang.blaze3d.vertex;

import java.nio.ByteBuffer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.util.FastColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeVertexConsumer;
import net.optifine.Config;
import net.optifine.IRandomEntity;
import net.optifine.RandomEntities;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.render.VertexPosition;
import net.optifine.shaders.Shaders;
import net.optifine.util.MathUtils;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public interface VertexConsumer extends IForgeVertexConsumer {
    ThreadLocal<RenderEnv> RENDER_ENV = ThreadLocal.withInitial(() -> new RenderEnv(Blocks.AIR.defaultBlockState(), new BlockPos(0, 0, 0)));
    boolean FORGE = Reflector.ForgeHooksClient.exists();

    default RenderEnv getRenderEnv(BlockState blockState, BlockPos blockPos) {
        RenderEnv renderenv = RENDER_ENV.get();
        renderenv.reset(blockState, blockPos);
        return renderenv;
    }

    VertexConsumer addVertex(float pX, float pY, float pZ);

    VertexConsumer setColor(int pRed, int pGreen, int pBlue, int pAlpha);

    VertexConsumer setUv(float pU, float pV);

    VertexConsumer setUv1(int pU, int pV);

    VertexConsumer setUv2(int pU, int pV);

    VertexConsumer setNormal(float pNormalX, float pNormalY, float pNormalZ);

    default void addVertex(
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
        this.addVertex(pX, pY, pZ);
        this.setColor(pColor);
        this.setUv(pU, pV);
        this.setOverlay(pPackedOverlay);
        this.setLight(pPackedLight);
        this.setNormal(pNormalX, pNormalY, pNormalZ);
    }

    default VertexConsumer setColor(float pRed, float pGreen, float pBlue, float pAlpha) {
        return this.setColor((int)(pRed * 255.0F), (int)(pGreen * 255.0F), (int)(pBlue * 255.0F), (int)(pAlpha * 255.0F));
    }

    default VertexConsumer setColor(int pColor) {
        return this.setColor(
            FastColor.ARGB32.red(pColor),
            FastColor.ARGB32.green(pColor),
            FastColor.ARGB32.blue(pColor),
            FastColor.ARGB32.alpha(pColor)
        );
    }

    default VertexConsumer setWhiteAlpha(int pAlpha) {
        return this.setColor(FastColor.ARGB32.color(pAlpha, -1));
    }

    default VertexConsumer setLight(int pPackedLight) {
        return this.setUv2(pPackedLight & 65535, pPackedLight >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int pPackedOverlay) {
        return this.setUv1(pPackedOverlay & 65535, pPackedOverlay >> 16 & 65535);
    }

    default void putBulkData(
        PoseStack.Pose pPose, BakedQuad pQuad, float pRed, float pGreen, float pBlue, float pAlpha, int pPackedLight, int pPackedOverlay
    ) {
        this.putBulkData(
            pPose,
            pQuad,
            this.getTempFloat4(1.0F, 1.0F, 1.0F, 1.0F),
            pRed,
            pGreen,
            pBlue,
            pAlpha,
            this.getTempInt4(pPackedLight, pPackedLight, pPackedLight, pPackedLight),
            pPackedOverlay,
            false
        );
    }

    @Override
    default void putBulkData(
        PoseStack.Pose matrixEntry,
        BakedQuad bakedQuad,
        float red,
        float green,
        float blue,
        float alpha,
        int packedLight,
        int packedOverlay,
        boolean readExistingColor
    ) {
        this.putBulkData(
            matrixEntry,
            bakedQuad,
            this.getTempFloat4(1.0F, 1.0F, 1.0F, 1.0F),
            red,
            green,
            blue,
            alpha,
            this.getTempInt4(packedLight, packedLight, packedLight, packedLight),
            packedOverlay,
            readExistingColor
        );
    }

    default void putBulkData(
        PoseStack.Pose pPose,
        BakedQuad pQuad,
        float[] pBrightness,
        float pRed,
        float pGreen,
        float pBlue,
        float pAlpha,
        int[] pLightmap,
        int pPackedOverlay,
        boolean pReadAlpha
    ) {
        int[] aint = this.isMultiTexture() ? pQuad.getVertexDataSingle() : pQuad.getVertices();
        this.putSprite(pQuad.getSprite());
        boolean flag = ModelBlockRenderer.isSeparateAoLightValue();
        Vec3i vec3i = pQuad.getDirection().getNormal();
        Matrix4f matrix4f = pPose.pose();
        Vector3f vector3f = pPose.transformNormal((float)vec3i.getX(), (float)vec3i.getY(), (float)vec3i.getZ(), this.getTempVec3f());
        float f = vector3f.x;
        float f1 = vector3f.y;
        float f2 = vector3f.z;
        int i = 8;
        int j = DefaultVertexFormat.BLOCK.getIntegerSize();
        int k = aint.length / j;
        int l = (int)(pAlpha * 255.0F);
        boolean flag1 = Config.isShaders() && Shaders.useVelocityAttrib && Config.isMinecraftThread();
        if (flag1) {
            IRandomEntity irandomentity = RandomEntities.getRandomEntityRendered();
            if (irandomentity != null) {
                VertexPosition[] avertexposition = pQuad.getVertexPositions(irandomentity.getId());
                this.setQuadVertexPositions(avertexposition);
            }
        }

        for (int j1 = 0; j1 < k; j1++) {
            int k1 = j1 * j;
            float f3 = Float.intBitsToFloat(aint[k1 + 0]);
            float f4 = Float.intBitsToFloat(aint[k1 + 1]);
            float f5 = Float.intBitsToFloat(aint[k1 + 2]);
            float f9 = flag ? 1.0F : pBrightness[j1];
            float f6;
            float f7;
            float f8;
            if (pReadAlpha) {
                int i1 = aint[k1 + 3];
                float f10 = (float)(i1 & 0xFF);
                float f11 = (float)(i1 >> 8 & 0xFF);
                float f12 = (float)(i1 >> 16 & 0xFF);
                f6 = f10 * f9 * pRed;
                f7 = f11 * f9 * pGreen;
                f8 = f12 * f9 * pBlue;
            } else {
                f6 = f9 * pRed * 255.0F;
                f7 = f9 * pGreen * 255.0F;
                f8 = f9 * pBlue * 255.0F;
            }

            if (flag) {
                l = (int)(pBrightness[j1] * 255.0F);
            }

            int l1 = FastColor.ARGB32.color(l, (int)f6, (int)f7, (int)f8);
            int i2 = pLightmap[j1];
            if (FORGE) {
                i2 = this.applyBakedLighting(pLightmap[j1], aint, k1);
            }

            float f16 = Float.intBitsToFloat(aint[k1 + 4]);
            float f17 = Float.intBitsToFloat(aint[k1 + 5]);
            float f13 = MathUtils.getTransformX(matrix4f, f3, f4, f5);
            float f14 = MathUtils.getTransformY(matrix4f, f3, f4, f5);
            float f15 = MathUtils.getTransformZ(matrix4f, f3, f4, f5);
            if (FORGE) {
                Vector3f vector3f1 = this.applyBakedNormals(aint, k1, pPose.normal());
                if (vector3f1 != null) {
                    f = vector3f1.x();
                    f1 = vector3f1.y();
                    f2 = vector3f1.z();
                }
            }

            this.addVertex(f13, f14, f15, l1, f16, f17, pPackedOverlay, i2, f, f1, f2);
        }
    }

    default VertexConsumer addVertex(Vector3f pPos) {
        return this.addVertex(pPos.x(), pPos.y(), pPos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pPose, Vector3f pPos) {
        return this.addVertex(pPose, pPos.x(), pPos.y(), pPos.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose pPose, float pX, float pY, float pZ) {
        return this.addVertex(pPose.pose(), pX, pY, pZ);
    }

    default VertexConsumer addVertex(Matrix4f pPose, float pX, float pY, float pZ) {
        Vector3f vector3f = pPose.transformPosition(pX, pY, pZ, this.getTempVec3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose pPose, float pNormalX, float pNormalY, float pNormalZ) {
        Vector3f vector3f = pPose.transformNormal(pNormalX, pNormalY, pNormalZ, this.getTempVec3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default void putSprite(TextureAtlasSprite sprite) {
    }

    default void setSprite(TextureAtlasSprite sprite) {
    }

    default boolean isMultiTexture() {
        return false;
    }

    default RenderType getRenderType() {
        return null;
    }

    default Vector3f getTempVec3f() {
        return new Vector3f();
    }

    default Vector3f getTempVec3f(float x, float y, float z) {
        return this.getTempVec3f().set(x, y, z);
    }

    default Vector3f getTempVec3f(Vector3f vec) {
        return this.getTempVec3f().set(vec);
    }

    default float[] getTempFloat4(float f1, float f2, float f3, float f4) {
        return new float[]{f1, f2, f3, f4};
    }

    default int[] getTempInt4(int i1, int i2, int i3, int i4) {
        return new int[]{i1, i2, i3, i4};
    }

    default MultiBufferSource.BufferSource getRenderTypeBuffer() {
        return null;
    }

    default void setQuadVertexPositions(VertexPosition[] vps) {
    }

    default void setMidBlock(float mbx, float mby, float mbz) {
    }

    default Vector3f getMidBlock() {
        return null;
    }

    default VertexConsumer getSecondaryBuilder() {
        return null;
    }

    default int getVertexCount() {
        return 0;
    }

    default int applyBakedLighting(int lightmapCoord, int[] data, int pos) {
        int i = getLightOffset(0);
        int j = LightTexture.block(data[pos + i]);
        int k = LightTexture.sky(data[pos + i]);
        if (j == 0 && k == 0) {
            return lightmapCoord;
        } else {
            int l = LightTexture.block(lightmapCoord);
            int i1 = LightTexture.sky(lightmapCoord);
            l = Math.max(l, j);
            i1 = Math.max(i1, k);
            return LightTexture.pack(l, i1);
        }
    }

    static int getLightOffset(int v) {
        return v * 8 + 6;
    }

    default Vector3f applyBakedNormals(int[] data, int pos, Matrix3f normalTransform) {
        int i = 7;
        int j = data[pos + i];
        byte b0 = (byte)(j >> 0 & 0xFF);
        byte b1 = (byte)(j >> 8 & 0xFF);
        byte b2 = (byte)(j >> 16 & 0xFF);
        if (b0 == 0 && b1 == 0 && b2 == 0) {
            return null;
        } else {
            Vector3f vector3f = this.getTempVec3f((float)b0 / 127.0F, (float)b1 / 127.0F, (float)b2 / 127.0F);
            MathUtils.transform(vector3f, normalTransform);
            return vector3f;
        }
    }

    default void getBulkData(ByteBuffer buffer) {
    }

    default void putBulkData(ByteBuffer buffer) {
    }

    default boolean canAddVertexFast() {
        return false;
    }

    default void addVertexFast(float x, float y, float z, int color, float texU, float texV, int overlayUV, int lightmapUV, int normals) {
    }
}
