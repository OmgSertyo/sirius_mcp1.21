package net.minecraft.client.renderer.entity;

import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import com.mojang.math.MatrixUtil;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.ItemModelShaper;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.optifine.Config;
import net.optifine.CustomColors;
import net.optifine.CustomItems;
import net.optifine.EmissiveTextures;
import net.optifine.reflect.Reflector;
import net.optifine.render.VertexBuilderWrapper;
import net.optifine.shaders.Shaders;
import net.optifine.util.SingleIterable;

public class ItemRenderer implements ResourceManagerReloadListener {
    public static final ResourceLocation ENCHANTED_GLINT_ENTITY = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_entity.png");
    public static final ResourceLocation ENCHANTED_GLINT_ITEM = ResourceLocation.withDefaultNamespace("textures/misc/enchanted_glint_item.png");
    private static final Set<Item> IGNORED = Sets.newHashSet(Items.AIR);
    public static final int GUI_SLOT_CENTER_X = 8;
    public static final int GUI_SLOT_CENTER_Y = 8;
    public static final int ITEM_COUNT_BLIT_OFFSET = 200;
    public static final float COMPASS_FOIL_UI_SCALE = 0.5F;
    public static final float COMPASS_FOIL_FIRST_PERSON_SCALE = 0.75F;
    public static final float COMPASS_FOIL_TEXTURE_SCALE = 0.0078125F;
    private static final ModelResourceLocation TRIDENT_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident"));
    public static final ModelResourceLocation TRIDENT_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("trident_in_hand"));
    private static final ModelResourceLocation SPYGLASS_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass"));
    public static final ModelResourceLocation SPYGLASS_IN_HAND_MODEL = ModelResourceLocation.inventory(ResourceLocation.withDefaultNamespace("spyglass_in_hand"));
    private final Minecraft minecraft;
    private final ItemModelShaper itemModelShaper;
    private final TextureManager textureManager;
    private final ItemColors itemColors;
    private final BlockEntityWithoutLevelRenderer blockEntityRenderer;
    public ModelManager modelManager = null;
    private static boolean renderItemGui = false;

    public ItemRenderer(Minecraft pMinecraft, TextureManager pTextureManager, ModelManager pModelManager, ItemColors pItemColors, BlockEntityWithoutLevelRenderer pBlockEntityRenderer) {
        this.minecraft = pMinecraft;
        this.textureManager = pTextureManager;
        this.modelManager = pModelManager;
        if (Reflector.ForgeItemModelShaper_Constructor.exists()) {
            this.itemModelShaper = (ItemModelShaper)Reflector.newInstance(Reflector.ForgeItemModelShaper_Constructor, this.modelManager);
        } else {
            this.itemModelShaper = new ItemModelShaper(pModelManager);
        }

        this.blockEntityRenderer = pBlockEntityRenderer;

        for (Item item : BuiltInRegistries.ITEM) {
            if (!IGNORED.contains(item)) {
                this.itemModelShaper.register(item, ModelResourceLocation.inventory(BuiltInRegistries.ITEM.getKey(item)));
            }
        }

        this.itemColors = pItemColors;
    }

    public ItemModelShaper getItemModelShaper() {
        return this.itemModelShaper;
    }

    public void renderModelLists(BakedModel pModel, ItemStack pStack, int pCombinedLight, int pCombinedOverlay, PoseStack pPoseStack, VertexConsumer pBuffer) {
        RandomSource randomsource = RandomSource.create();
        long i = 42L;

        for (Direction direction : Direction.VALUES) {
            randomsource.setSeed(42L);
            this.renderQuadList(pPoseStack, pBuffer, pModel.getQuads(null, direction, randomsource), pStack, pCombinedLight, pCombinedOverlay);
        }

        randomsource.setSeed(42L);
        this.renderQuadList(pPoseStack, pBuffer, pModel.getQuads(null, null, randomsource), pStack, pCombinedLight, pCombinedOverlay);
    }

    public void render(
        ItemStack pItemStack,
        ItemDisplayContext pDisplayContext,
        boolean pLeftHand,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        int pCombinedLight,
        int pCombinedOverlay,
        BakedModel pModel
    ) {
        if (!pItemStack.isEmpty()) {
            pPoseStack.pushPose();
            boolean flag = pDisplayContext == ItemDisplayContext.GUI || pDisplayContext == ItemDisplayContext.GROUND || pDisplayContext == ItemDisplayContext.FIXED;
            if (flag) {
                if (pItemStack.is(Items.TRIDENT)) {
                    pModel = this.itemModelShaper.getModelManager().getModel(TRIDENT_MODEL);
                } else if (pItemStack.is(Items.SPYGLASS)) {
                    pModel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_MODEL);
                }
            }

            if (Reflector.ForgeHooksClient.exists()) {
                pModel = pModel.applyTransform(pDisplayContext, pPoseStack, pLeftHand);
            } else {
                pModel.getTransforms().getTransform(pDisplayContext).apply(pLeftHand, pPoseStack);
            }

            pPoseStack.translate(-0.5F, -0.5F, -0.5F);
            if (!pModel.isCustomRenderer() && (!pItemStack.is(Items.TRIDENT) || flag)) {
                boolean flag1;
                if (pDisplayContext != ItemDisplayContext.GUI && !pDisplayContext.firstPerson() && pItemStack.getItem() instanceof BlockItem blockitem) {
                    Block block = blockitem.getBlock();
                    flag1 = !(block instanceof HalfTransparentBlock) && !(block instanceof StainedGlassPaneBlock);
                } else {
                    flag1 = true;
                }

                boolean flag2 = Reflector.ForgeHooksClient.exists();
                Iterable<BakedModel> iterable1 = (Iterable<BakedModel>)(flag2 ? pModel.getRenderPasses(pItemStack, flag1) : new SingleIterable<>(pModel));
                Iterable<RenderType> iterable = (Iterable<RenderType>)(flag2
                    ? pModel.getRenderTypes(pItemStack, flag1)
                    : new SingleIterable<>(ItemBlockRenderTypes.getRenderType(pItemStack, flag1)));

                for (BakedModel bakedmodel : iterable1) {
                    pModel = bakedmodel;

                    for (RenderType rendertype : iterable) {
                        VertexConsumer vertexconsumer;
                        if (hasAnimatedTexture(pItemStack) && pItemStack.hasFoil()) {
                            PoseStack.Pose posestack$pose = pPoseStack.last().copy();
                            if (pDisplayContext == ItemDisplayContext.GUI) {
                                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.5F);
                            } else if (pDisplayContext.firstPerson()) {
                                MatrixUtil.mulComponentWise(posestack$pose.pose(), 0.75F);
                            }

                            vertexconsumer = getCompassFoilBuffer(pBufferSource, rendertype, posestack$pose);
                        } else if (flag1) {
                            vertexconsumer = getFoilBufferDirect(pBufferSource, rendertype, true, pItemStack.hasFoil());
                        } else {
                            vertexconsumer = getFoilBuffer(pBufferSource, rendertype, true, pItemStack.hasFoil());
                        }

                        if (Config.isCustomItems()) {
                            pModel = CustomItems.getCustomItemModel(pItemStack, pModel, ItemOverrides.lastModelLocation, false);
                            ItemOverrides.lastModelLocation = null;
                        }

                        if (EmissiveTextures.isActive()) {
                            EmissiveTextures.beginRender();
                        }

                        this.renderModelLists(pModel, pItemStack, pCombinedLight, pCombinedOverlay, pPoseStack, vertexconsumer);
                        if (EmissiveTextures.isActive()) {
                            if (EmissiveTextures.hasEmissive()) {
                                EmissiveTextures.beginRenderEmissive();
                                VertexConsumer vertexconsumer1 = vertexconsumer instanceof VertexBuilderWrapper
                                    ? ((VertexBuilderWrapper)vertexconsumer).getVertexBuilder()
                                    : vertexconsumer;
                                this.renderModelLists(pModel, pItemStack, LightTexture.MAX_BRIGHTNESS, pCombinedOverlay, pPoseStack, vertexconsumer1);
                                EmissiveTextures.endRenderEmissive();
                            }

                            EmissiveTextures.endRender();
                        }
                    }
                }
            } else if (Reflector.MinecraftForge.exists()) {
                IClientItemExtensions.of(pItemStack).getCustomRenderer().renderByItem(pItemStack, pDisplayContext, pPoseStack, pBufferSource, pCombinedLight, pCombinedOverlay);
            } else {
                this.blockEntityRenderer.renderByItem(pItemStack, pDisplayContext, pPoseStack, pBufferSource, pCombinedLight, pCombinedOverlay);
            }

            pPoseStack.popPose();
        }
    }

    private static boolean hasAnimatedTexture(ItemStack pStack) {
        return pStack.is(ItemTags.COMPASSES) || pStack.is(Items.CLOCK);
    }

    public static VertexConsumer getArmorFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pHasFoil) {
        if (Shaders.isShadowPass) {
            pHasFoil = false;
        }

        if (EmissiveTextures.isRenderEmissive()) {
            pHasFoil = false;
        }

        return pHasFoil ? VertexMultiConsumer.create(pBufferSource.getBuffer(RenderType.armorEntityGlint()), pBufferSource.getBuffer(pRenderType)) : pBufferSource.getBuffer(pRenderType);
    }

    public static VertexConsumer getCompassFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, PoseStack.Pose pPose) {
        return VertexMultiConsumer.create(
            new SheetedDecalTextureGenerator(pBufferSource.getBuffer(RenderType.glint()), pPose, 0.0078125F), pBufferSource.getBuffer(pRenderType)
        );
    }

    public static VertexConsumer getFoilBuffer(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pIsItem, boolean pGlint) {
        if (Shaders.isShadowPass) {
            pGlint = false;
        }

        if (EmissiveTextures.isRenderEmissive()) {
            pGlint = false;
        }

        if (!pGlint) {
            return pBufferSource.getBuffer(pRenderType);
        } else {
            return Minecraft.useShaderTransparency() && pRenderType == Sheets.translucentItemSheet()
                ? VertexMultiConsumer.create(pBufferSource.getBuffer(RenderType.glintTranslucent()), pBufferSource.getBuffer(pRenderType))
                : VertexMultiConsumer.create(pBufferSource.getBuffer(pIsItem ? RenderType.glint() : RenderType.entityGlint()), pBufferSource.getBuffer(pRenderType));
        }
    }

    public static VertexConsumer getFoilBufferDirect(MultiBufferSource pBufferSource, RenderType pRenderType, boolean pNoEntity, boolean pWithGlint) {
        if (Shaders.isShadowPass) {
            pWithGlint = false;
        }

        if (EmissiveTextures.isRenderEmissive()) {
            pWithGlint = false;
        }

        return pWithGlint
            ? VertexMultiConsumer.create(pBufferSource.getBuffer(pNoEntity ? RenderType.glint() : RenderType.entityGlintDirect()), pBufferSource.getBuffer(pRenderType))
            : pBufferSource.getBuffer(pRenderType);
    }

    private void renderQuadList(PoseStack pPoseStack, VertexConsumer pBuffer, List<BakedQuad> pQuads, ItemStack pItemStack, int pCombinedLight, int pCombinedOverlay) {
        boolean flag = !pItemStack.isEmpty();
        PoseStack.Pose posestack$pose = pPoseStack.last();
        boolean flag1 = EmissiveTextures.isActive();
        int i = pQuads.size();
        int j = i > 0 && Config.isCustomColors() ? CustomColors.getColorFromItemStack(pItemStack, -1, -1) : -1;

        for (int k = 0; k < i; k++) {
            BakedQuad bakedquad = pQuads.get(k);
            if (flag1) {
                bakedquad = EmissiveTextures.getEmissiveQuad(bakedquad);
                if (bakedquad == null) {
                    continue;
                }
            }

            int l = j;
            if (flag && bakedquad.isTinted()) {
                l = this.itemColors.getColor(pItemStack, bakedquad.getTintIndex());
                if (Config.isCustomColors()) {
                    l = CustomColors.getColorFromItemStack(pItemStack, bakedquad.getTintIndex(), l);
                }
            }

            float f = (float)FastColor.ARGB32.alpha(l) / 255.0F;
            float f1 = (float)FastColor.ARGB32.red(l) / 255.0F;
            float f2 = (float)FastColor.ARGB32.green(l) / 255.0F;
            float f3 = (float)FastColor.ARGB32.blue(l) / 255.0F;
            if (Reflector.ForgeHooksClient.exists()) {
                pBuffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, pCombinedLight, pCombinedOverlay, true);
            } else {
                pBuffer.putBulkData(posestack$pose, bakedquad, f1, f2, f3, f, pCombinedLight, pCombinedOverlay);
            }
        }
    }

    public BakedModel getModel(ItemStack pStack, @Nullable Level pLevel, @Nullable LivingEntity pEntity, int pSeed) {
        BakedModel bakedmodel;
        if (pStack.is(Items.TRIDENT)) {
            bakedmodel = this.itemModelShaper.getModelManager().getModel(TRIDENT_IN_HAND_MODEL);
        } else if (pStack.is(Items.SPYGLASS)) {
            bakedmodel = this.itemModelShaper.getModelManager().getModel(SPYGLASS_IN_HAND_MODEL);
        } else {
            bakedmodel = this.itemModelShaper.getItemModel(pStack);
        }

        ClientLevel clientlevel = pLevel instanceof ClientLevel ? (ClientLevel)pLevel : null;
        ItemOverrides.lastModelLocation = null;
        BakedModel bakedmodel1 = bakedmodel.getOverrides().resolve(bakedmodel, pStack, clientlevel, pEntity, pSeed);
        if (Config.isCustomItems()) {
            bakedmodel1 = CustomItems.getCustomItemModel(pStack, bakedmodel1, ItemOverrides.lastModelLocation, true);
        }

        return bakedmodel1 == null ? this.itemModelShaper.getModelManager().getMissingModel() : bakedmodel1;
    }

    public void renderStatic(
        ItemStack pStack,
        ItemDisplayContext pDisplayContext,
        int pCombinedLight,
        int pCombinedOverlay,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        @Nullable Level pLevel,
        int pSeed
    ) {
        this.renderStatic(null, pStack, pDisplayContext, false, pPoseStack, pBufferSource, pLevel, pCombinedLight, pCombinedOverlay, pSeed);
    }

    public void renderStatic(
        @Nullable LivingEntity pEntity,
        ItemStack pItemStack,
        ItemDisplayContext pDiplayContext,
        boolean pLeftHand,
        PoseStack pPoseStack,
        MultiBufferSource pBufferSource,
        @Nullable Level pLevel,
        int pCombinedLight,
        int pCombinedOverlay,
        int pSeed
    ) {
        if (!pItemStack.isEmpty()) {
            BakedModel bakedmodel = this.getModel(pItemStack, pLevel, pEntity, pSeed);
            this.render(pItemStack, pDiplayContext, pLeftHand, pPoseStack, pBufferSource, pCombinedLight, pCombinedOverlay, bakedmodel);
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager pResourceManager) {
        this.itemModelShaper.rebuildCache();
    }

    public static boolean isRenderItemGui() {
        return renderItemGui;
    }

    public static void setRenderItemGui(boolean renderItemGui) {
        ItemRenderer.renderItemGui = renderItemGui;
    }

    public BlockEntityWithoutLevelRenderer getBlockEntityRenderer() {
        return this.blockEntityRenderer;
    }
}