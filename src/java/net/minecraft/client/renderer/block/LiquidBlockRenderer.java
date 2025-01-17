package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.reflect.Reflector;
import net.optifine.render.RenderEnv;
import net.optifine.shaders.SVertexBuilder;
import net.optifine.shaders.Shaders;

public class LiquidBlockRenderer {
    private static final float MAX_FLUID_HEIGHT = 0.8888889F;
    private final TextureAtlasSprite[] lavaIcons = new TextureAtlasSprite[2];
    private final TextureAtlasSprite[] waterIcons = new TextureAtlasSprite[2];
    private TextureAtlasSprite waterOverlay;

    protected void setupSprites() {
        this.lavaIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon();
        this.lavaIcons[1] = ModelBakery.LAVA_FLOW.sprite();
        this.waterIcons[0] = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon();
        this.waterIcons[1] = ModelBakery.WATER_FLOW.sprite();
        this.waterOverlay = ModelBakery.WATER_OVERLAY.sprite();
    }

    private static boolean isNeighborSameFluid(FluidState pFirstState, FluidState pSecondState) {
        return pSecondState.getType().isSame(pFirstState.getType());
    }

    private static boolean isFaceOccludedByState(BlockGetter pLevel, Direction pFace, float pHeight, BlockPos pPos, BlockState pState) {
        if (pState.canOcclude()) {
            VoxelShape voxelshape = Shapes.box(0.0, 0.0, 0.0, 1.0, (double)pHeight, 1.0);
            VoxelShape voxelshape1 = pState.getOcclusionShape(pLevel, pPos);
            return Shapes.blockOccudes(voxelshape, voxelshape1, pFace);
        } else {
            return false;
        }
    }

    private static boolean isFaceOccludedByNeighbor(BlockGetter pLevel, BlockPos pPos, Direction pSide, float pHeight, BlockState pBlockState) {
        return isFaceOccludedByState(pLevel, pSide, pHeight, pPos.relative(pSide), pBlockState);
    }

    private static boolean isFaceOccludedBySelf(BlockGetter pLevel, BlockPos pPos, BlockState pState, Direction pFace) {
        return isFaceOccludedByState(pLevel, pFace.getOpposite(), 1.0F, pPos, pState);
    }

    public static boolean shouldRenderFace(
        BlockAndTintGetter pLevel, BlockPos pPos, FluidState pFluidState, BlockState pBlockState, Direction pSide, FluidState pNeighborFluid
    ) {
        return !isFaceOccludedBySelf(pLevel, pPos, pBlockState, pSide) && !isNeighborSameFluid(pFluidState, pNeighborFluid);
    }

    public void tesselate(BlockAndTintGetter pLevel, BlockPos pPos, VertexConsumer pBuffer, BlockState pBlockState, FluidState pFluidState) {
        BlockState blockstate = pFluidState.createLegacyBlock();

        try {
            if (Config.isShaders()) {
                SVertexBuilder.pushEntity(blockstate, pBuffer);
            }

            boolean flag = pFluidState.is(FluidTags.LAVA);
            TextureAtlasSprite[] atextureatlassprite = flag ? this.lavaIcons : this.waterIcons;
            if (Reflector.ForgeHooksClient_getFluidSprites.exists()) {
                TextureAtlasSprite[] atextureatlassprite1 = (TextureAtlasSprite[])Reflector.call(
                    Reflector.ForgeHooksClient_getFluidSprites, pLevel, pPos, pFluidState
                );
                if (atextureatlassprite1 != null) {
                    atextureatlassprite = atextureatlassprite1;
                }
            }

            RenderEnv renderenv = pBuffer.getRenderEnv(blockstate, pPos);
            boolean flag1 = !flag && Minecraft.useAmbientOcclusion();
            int i = -1;
            float f = 1.0F;
            if (Reflector.ForgeHooksClient.exists()) {
                i = IClientFluidTypeExtensions.of(pFluidState).getTintColor(pFluidState, pLevel, pPos);
                f = (float)(i >> 24 & 0xFF) / 255.0F;
            }

            BlockState blockstate1 = pLevel.getBlockState(pPos.relative(Direction.DOWN));
            FluidState fluidstate = blockstate1.getFluidState();
            BlockState blockstate2 = pLevel.getBlockState(pPos.relative(Direction.UP));
            FluidState fluidstate1 = blockstate2.getFluidState();
            BlockState blockstate3 = pLevel.getBlockState(pPos.relative(Direction.NORTH));
            FluidState fluidstate2 = blockstate3.getFluidState();
            BlockState blockstate4 = pLevel.getBlockState(pPos.relative(Direction.SOUTH));
            FluidState fluidstate3 = blockstate4.getFluidState();
            BlockState blockstate5 = pLevel.getBlockState(pPos.relative(Direction.WEST));
            FluidState fluidstate4 = blockstate5.getFluidState();
            BlockState blockstate6 = pLevel.getBlockState(pPos.relative(Direction.EAST));
            FluidState fluidstate5 = blockstate6.getFluidState();
            boolean flag2 = !isNeighborSameFluid(pFluidState, fluidstate1);
            boolean flag3 = shouldRenderFace(pLevel, pPos, pFluidState, pBlockState, Direction.DOWN, fluidstate)
                && !isFaceOccludedByNeighbor(pLevel, pPos, Direction.DOWN, 0.8888889F, blockstate1);
            boolean flag4 = shouldRenderFace(pLevel, pPos, pFluidState, pBlockState, Direction.NORTH, fluidstate2);
            boolean flag5 = shouldRenderFace(pLevel, pPos, pFluidState, pBlockState, Direction.SOUTH, fluidstate3);
            boolean flag6 = shouldRenderFace(pLevel, pPos, pFluidState, pBlockState, Direction.WEST, fluidstate4);
            boolean flag7 = shouldRenderFace(pLevel, pPos, pFluidState, pBlockState, Direction.EAST, fluidstate5);
            if (flag2 || flag3 || flag7 || flag6 || flag4 || flag5) {
                if (i < 0) {
                    i = CustomColors.getFluidColor(pLevel, blockstate, pPos, renderenv);
                }

                float f1 = (float)(i >> 16 & 0xFF) / 255.0F;
                float f2 = (float)(i >> 8 & 0xFF) / 255.0F;
                float f3 = (float)(i & 0xFF) / 255.0F;
                float f4 = pLevel.getShade(Direction.DOWN, true);
                float f5 = pLevel.getShade(Direction.UP, true);
                float f6 = pLevel.getShade(Direction.NORTH, true);
                float f7 = pLevel.getShade(Direction.WEST, true);
                Fluid fluid = pFluidState.getType();
                float f8 = this.getHeight(pLevel, fluid, pPos, pBlockState, pFluidState);
                float f9;
                float f10;
                float f11;
                float f12;
                if (f8 >= 1.0F) {
                    f9 = 1.0F;
                    f10 = 1.0F;
                    f11 = 1.0F;
                    f12 = 1.0F;
                } else {
                    float f13 = this.getHeight(pLevel, fluid, pPos.north(), blockstate3, fluidstate2);
                    float f14 = this.getHeight(pLevel, fluid, pPos.south(), blockstate4, fluidstate3);
                    float f15 = this.getHeight(pLevel, fluid, pPos.east(), blockstate6, fluidstate5);
                    float f16 = this.getHeight(pLevel, fluid, pPos.west(), blockstate5, fluidstate4);
                    f9 = this.calculateAverageHeight(pLevel, fluid, f8, f13, f15, pPos.relative(Direction.NORTH).relative(Direction.EAST));
                    f10 = this.calculateAverageHeight(pLevel, fluid, f8, f13, f16, pPos.relative(Direction.NORTH).relative(Direction.WEST));
                    f11 = this.calculateAverageHeight(pLevel, fluid, f8, f14, f15, pPos.relative(Direction.SOUTH).relative(Direction.EAST));
                    f12 = this.calculateAverageHeight(pLevel, fluid, f8, f14, f16, pPos.relative(Direction.SOUTH).relative(Direction.WEST));
                }

                float f24 = (float)(pPos.getX() & 15);
                float f25 = (float)(pPos.getY() & 15);
                float f26 = (float)(pPos.getZ() & 15);
                if (Config.isRenderRegions()) {
                    int i5 = pPos.getX() >> 4 << 4;
                    int j = pPos.getY() >> 4 << 4;
                    int k = pPos.getZ() >> 4 << 4;
                    int l = 8;
                    int i1 = i5 >> l << l;
                    int j1 = k >> l << l;
                    int k1 = i5 - i1;
                    int l1 = k - j1;
                    f24 += (float)k1;
                    f25 += (float)j;
                    f26 += (float)l1;
                }

                if (Config.isShaders() && Shaders.useMidBlockAttrib) {
                    pBuffer.setMidBlock((float)((double)f24 + 0.5), (float)((double)f25 + 0.5), (float)((double)f26 + 0.5));
                }

                float f27 = 0.001F;
                float f28 = flag3 ? 0.001F : 0.0F;
                if (flag2 && !isFaceOccludedByNeighbor(pLevel, pPos, Direction.UP, Math.min(Math.min(f10, f12), Math.min(f11, f9)), blockstate2)) {
                    f10 -= 0.001F;
                    f12 -= 0.001F;
                    f11 -= 0.001F;
                    f9 -= 0.001F;
                    Vec3 vec3 = pFluidState.getFlow(pLevel, pPos);
                    float f17;
                    float f18;
                    float f19;
                    float f30;
                    float f32;
                    float f34;
                    float f37;
                    float f41;
                    if (vec3.x == 0.0 && vec3.z == 0.0) {
                        TextureAtlasSprite textureatlassprite1 = atextureatlassprite[0];
                        pBuffer.setSprite(textureatlassprite1);
                        f30 = textureatlassprite1.getU(0.0F);
                        f17 = textureatlassprite1.getV(0.0F);
                        f32 = f30;
                        f41 = textureatlassprite1.getV(1.0F);
                        f34 = textureatlassprite1.getU(1.0F);
                        f18 = f41;
                        f37 = f34;
                        f19 = f17;
                    } else {
                        TextureAtlasSprite textureatlassprite = atextureatlassprite[1];
                        pBuffer.setSprite(textureatlassprite);
                        float f20 = (float)Mth.atan2(vec3.z, vec3.x) - (float) (Math.PI / 2);
                        float f21 = Mth.sin(f20) * 0.25F;
                        float f22 = Mth.cos(f20) * 0.25F;
                        float f23 = 0.5F;
                        f30 = textureatlassprite.getU(0.5F + (-f22 - f21));
                        f17 = textureatlassprite.getV(0.5F + -f22 + f21);
                        f32 = textureatlassprite.getU(0.5F + -f22 + f21);
                        f41 = textureatlassprite.getV(0.5F + f22 + f21);
                        f34 = textureatlassprite.getU(0.5F + f22 + f21);
                        f18 = textureatlassprite.getV(0.5F + (f22 - f21));
                        f37 = textureatlassprite.getU(0.5F + (f22 - f21));
                        f19 = textureatlassprite.getV(0.5F + (-f22 - f21));
                    }

                    float f48 = (f30 + f32 + f34 + f37) / 4.0F;
                    float f49 = (f17 + f41 + f18 + f19) / 4.0F;
                    float f50 = atextureatlassprite[0].uvShrinkRatio();
                    f30 = Mth.lerp(f50, f30, f48);
                    f32 = Mth.lerp(f50, f32, f48);
                    f34 = Mth.lerp(f50, f34, f48);
                    f37 = Mth.lerp(f50, f37, f48);
                    f17 = Mth.lerp(f50, f17, f49);
                    f41 = Mth.lerp(f50, f41, f49);
                    f18 = Mth.lerp(f50, f18, f49);
                    f19 = Mth.lerp(f50, f19, f49);
                    int l5 = this.getLightColor(pLevel, pPos);
                    int i2 = l5;
                    int j2 = l5;
                    int k2 = l5;
                    int l2 = l5;
                    if (flag1) {
                        BlockPos blockpos = pPos.north();
                        BlockPos blockpos1 = pPos.south();
                        BlockPos blockpos2 = pPos.east();
                        BlockPos blockpos3 = pPos.west();
                        int i3 = this.getLightColor(pLevel, blockpos);
                        int j3 = this.getLightColor(pLevel, blockpos1);
                        int k3 = this.getLightColor(pLevel, blockpos2);
                        int l3 = this.getLightColor(pLevel, blockpos3);
                        int i4 = this.getLightColor(pLevel, blockpos.west());
                        int j4 = this.getLightColor(pLevel, blockpos1.west());
                        int k4 = this.getLightColor(pLevel, blockpos1.east());
                        int l4 = this.getLightColor(pLevel, blockpos.east());
                        i2 = ModelBlockRenderer.AmbientOcclusionFace.blend(i3, i4, l3, l5);
                        j2 = ModelBlockRenderer.AmbientOcclusionFace.blend(j3, j4, l3, l5);
                        k2 = ModelBlockRenderer.AmbientOcclusionFace.blend(j3, k4, k3, l5);
                        l2 = ModelBlockRenderer.AmbientOcclusionFace.blend(i3, l4, k3, l5);
                    }

                    float f56 = f5 * f1;
                    float f58 = f5 * f2;
                    float f60 = f5 * f3;
                    this.vertexVanilla(pBuffer, f24 + 0.0F, f25 + f10, f26 + 0.0F, f56, f58, f60, f30, f17, i2, f);
                    this.vertexVanilla(pBuffer, f24 + 0.0F, f25 + f12, f26 + 1.0F, f56, f58, f60, f32, f41, j2, f);
                    this.vertexVanilla(pBuffer, f24 + 1.0F, f25 + f11, f26 + 1.0F, f56, f58, f60, f34, f18, k2, f);
                    this.vertexVanilla(pBuffer, f24 + 1.0F, f25 + f9, f26 + 0.0F, f56, f58, f60, f37, f19, l2, f);
                    if (pFluidState.shouldRenderBackwardUpFace(pLevel, pPos.above())) {
                        this.vertexVanilla(pBuffer, f24 + 0.0F, f25 + f10, f26 + 0.0F, f56, f58, f60, f30, f17, i2, f);
                        this.vertexVanilla(pBuffer, f24 + 1.0F, f25 + f9, f26 + 0.0F, f56, f58, f60, f37, f19, l2, f);
                        this.vertexVanilla(pBuffer, f24 + 1.0F, f25 + f11, f26 + 1.0F, f56, f58, f60, f34, f18, k2, f);
                        this.vertexVanilla(pBuffer, f24 + 0.0F, f25 + f12, f26 + 1.0F, f56, f58, f60, f32, f41, j2, f);
                    }
                }

                if (flag3) {
                    pBuffer.setSprite(atextureatlassprite[0]);
                    float f29 = atextureatlassprite[0].getU0();
                    float f31 = atextureatlassprite[0].getU1();
                    float f33 = atextureatlassprite[0].getV0();
                    float f35 = atextureatlassprite[0].getV1();
                    int k5 = this.getLightColor(pLevel, pPos.below());
                    float f39 = pLevel.getShade(Direction.DOWN, true);
                    float f42 = f39 * f1;
                    float f44 = f39 * f2;
                    float f46 = f39 * f3;
                    this.vertexVanilla(pBuffer, f24, f25 + f28, f26 + 1.0F, f42, f44, f46, f29, f35, k5, f);
                    this.vertexVanilla(pBuffer, f24, f25 + f28, f26, f42, f44, f46, f29, f33, k5, f);
                    this.vertexVanilla(pBuffer, f24 + 1.0F, f25 + f28, f26, f42, f44, f46, f31, f33, k5, f);
                    this.vertexVanilla(pBuffer, f24 + 1.0F, f25 + f28, f26 + 1.0F, f42, f44, f46, f31, f35, k5, f);
                }

                int j5 = this.getLightColor(pLevel, pPos);

                for (Direction direction : Direction.Plane.HORIZONTAL) {
                    float f36;
                    float f38;
                    float f40;
                    float f43;
                    float f45;
                    float f47;
                    boolean flag8;
                    switch (direction) {
                        case NORTH:
                            f36 = f10;
                            f38 = f9;
                            f40 = f24;
                            f45 = f24 + 1.0F;
                            f43 = f26 + 0.001F;
                            f47 = f26 + 0.001F;
                            flag8 = flag4;
                            break;
                        case SOUTH:
                            f36 = f11;
                            f38 = f12;
                            f40 = f24 + 1.0F;
                            f45 = f24;
                            f43 = f26 + 1.0F - 0.001F;
                            f47 = f26 + 1.0F - 0.001F;
                            flag8 = flag5;
                            break;
                        case WEST:
                            f36 = f12;
                            f38 = f10;
                            f40 = f24 + 0.001F;
                            f45 = f24 + 0.001F;
                            f43 = f26 + 1.0F;
                            f47 = f26;
                            flag8 = flag6;
                            break;
                        default:
                            f36 = f9;
                            f38 = f11;
                            f40 = f24 + 1.0F - 0.001F;
                            f45 = f24 + 1.0F - 0.001F;
                            f43 = f26;
                            f47 = f26 + 1.0F;
                            flag8 = flag7;
                    }

                    if (flag8 && !isFaceOccludedByNeighbor(pLevel, pPos, direction, Math.max(f36, f38), pLevel.getBlockState(pPos.relative(direction)))) {
                        BlockPos blockpos4 = pPos.relative(direction);
                        TextureAtlasSprite textureatlassprite2 = atextureatlassprite[1];
                        float f51 = 0.0F;
                        float f52 = 0.0F;
                        boolean flag9 = !flag;
                        if (Reflector.IForgeBlockState_shouldDisplayFluidOverlay.exists()) {
                            flag9 = atextureatlassprite[2] != null;
                        }

                        if (flag9) {
                            BlockState blockstate7 = pLevel.getBlockState(blockpos4);
                            Block block = blockstate7.getBlock();
                            boolean flag10 = false;
                            if (Reflector.IForgeBlockState_shouldDisplayFluidOverlay.exists()) {
                                flag10 = Reflector.callBoolean(
                                    blockstate7, Reflector.IForgeBlockState_shouldDisplayFluidOverlay, pLevel, blockpos4, pFluidState
                                );
                            }

                            if (flag10 || block instanceof HalfTransparentBlock || block instanceof LeavesBlock || block == Blocks.BEACON) {
                                textureatlassprite2 = this.waterOverlay;
                            }

                            if (block == Blocks.FARMLAND || block == Blocks.DIRT_PATH) {
                                f51 = 0.9375F;
                                f52 = 0.9375F;
                            }

                            if (block instanceof SlabBlock slabblock && blockstate7.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                                f51 = 0.5F;
                                f52 = 0.5F;
                            }
                        }

                        pBuffer.setSprite(textureatlassprite2);
                        if (!(f36 <= f51) || !(f38 <= f52)) {
                            f51 = Math.min(f51, f36);
                            f52 = Math.min(f52, f38);
                            if (f51 > f27) {
                                f51 -= f27;
                            }

                            if (f52 > f27) {
                                f52 -= f27;
                            }

                            float f53 = textureatlassprite2.getV((1.0F - f51) * 0.5F);
                            float f54 = textureatlassprite2.getV((1.0F - f52) * 0.5F);
                            float f55 = textureatlassprite2.getU(0.0F);
                            float f57 = textureatlassprite2.getU(0.5F);
                            float f59 = textureatlassprite2.getV((1.0F - f36) * 0.5F);
                            float f61 = textureatlassprite2.getV((1.0F - f38) * 0.5F);
                            float f62 = textureatlassprite2.getV(0.5F);
                            float f63 = direction != Direction.NORTH && direction != Direction.SOUTH
                                ? pLevel.getShade(Direction.WEST, true)
                                : pLevel.getShade(Direction.NORTH, true);
                            float f64 = f5 * f63 * f1;
                            float f65 = f5 * f63 * f2;
                            float f66 = f5 * f63 * f3;
                            this.vertexVanilla(pBuffer, f40, f25 + f36, f43, f64, f65, f66, f55, f59, j5, f);
                            this.vertexVanilla(pBuffer, f45, f25 + f38, f47, f64, f65, f66, f57, f61, j5, f);
                            this.vertexVanilla(pBuffer, f45, f25 + f28, f47, f64, f65, f66, f57, f54, j5, f);
                            this.vertexVanilla(pBuffer, f40, f25 + f28, f43, f64, f65, f66, f55, f53, j5, f);
                            if (textureatlassprite2 != this.waterOverlay) {
                                this.vertexVanilla(pBuffer, f40, f25 + f28, f43, f64, f65, f66, f55, f53, j5, f);
                                this.vertexVanilla(pBuffer, f45, f25 + f28, f47, f64, f65, f66, f57, f54, j5, f);
                                this.vertexVanilla(pBuffer, f45, f25 + f38, f47, f64, f65, f66, f57, f61, j5, f);
                                this.vertexVanilla(pBuffer, f40, f25 + f36, f43, f64, f65, f66, f55, f59, j5, f);
                            }
                        }
                    }
                }

                pBuffer.setSprite(null);
            }
        } finally {
            if (Config.isShaders()) {
                SVertexBuilder.popEntity(pBuffer);
            }
        }
    }

    private float calculateAverageHeight(BlockAndTintGetter pLevel, Fluid pFluid, float pCurrentHeight, float pHeight1, float pHeight2, BlockPos pPos) {
        if (!(pHeight2 >= 1.0F) && !(pHeight1 >= 1.0F)) {
            float[] afloat = new float[2];
            if (pHeight2 > 0.0F || pHeight1 > 0.0F) {
                float f = this.getHeight(pLevel, pFluid, pPos);
                if (f >= 1.0F) {
                    return 1.0F;
                }

                this.addWeightedHeight(afloat, f);
            }

            this.addWeightedHeight(afloat, pCurrentHeight);
            this.addWeightedHeight(afloat, pHeight2);
            this.addWeightedHeight(afloat, pHeight1);
            return afloat[0] / afloat[1];
        } else {
            return 1.0F;
        }
    }

    private void addWeightedHeight(float[] pOutput, float pHeight) {
        if (pHeight >= 0.8F) {
            pOutput[0] += pHeight * 10.0F;
            pOutput[1] += 10.0F;
        } else if (pHeight >= 0.0F) {
            pOutput[0] += pHeight;
            pOutput[1]++;
        }
    }

    private float getHeight(BlockAndTintGetter pLevel, Fluid pFluid, BlockPos pPos) {
        BlockState blockstate = pLevel.getBlockState(pPos);
        return this.getHeight(pLevel, pFluid, pPos, blockstate, blockstate.getFluidState());
    }

    private float getHeight(BlockAndTintGetter pLevel, Fluid pFluid, BlockPos pPos, BlockState pBlockState, FluidState pFluidState) {
        if (pFluid.isSame(pFluidState.getType())) {
            BlockState blockstate = pLevel.getBlockState(pPos.above());
            return pFluid.isSame(blockstate.getFluidState().getType()) ? 1.0F : pFluidState.getOwnHeight();
        } else {
            return !pBlockState.isSolid() ? 0.0F : -1.0F;
        }
    }

    private void vertex(
        VertexConsumer pBuffer,
        float pX,
        float pY,
        float pZ,
        float pRed,
        float pGreen,
        float pBlue,
        float pU,
        float pV,
        int pPackedLight
    ) {
        pBuffer.addVertex(pX, pY, pZ)
            .setColor(pRed, pGreen, pBlue, 1.0F)
            .setUv(pU, pV)
            .setLight(pPackedLight)
            .setNormal(0.0F, 1.0F, 0.0F);
    }

    private void vertexVanilla(
        VertexConsumer buffer, float x, float y, float z, float red, float green, float blue, float u, float v, int combinedLight, float alpha
    ) {
        buffer.addVertex(x, y, z).setColor(red, green, blue, alpha).setUv(u, v).setLight(combinedLight).setNormal(0.0F, 1.0F, 0.0F);
    }

    private int getLightColor(BlockAndTintGetter pLevel, BlockPos pPos) {
        int i = LevelRenderer.getLightColor(pLevel, pPos);
        int j = LevelRenderer.getLightColor(pLevel, pPos.above());
        int k = i & 0xFF;
        int l = j & 0xFF;
        int i1 = i >> 16 & 0xFF;
        int j1 = j >> 16 & 0xFF;
        return (k > l ? k : l) | (i1 > j1 ? i1 : j1) << 16;
    }
}