package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.BookModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LecternRenderer implements BlockEntityRenderer<LecternBlockEntity> {
    private final BookModel bookModel;

    public LecternRenderer(BlockEntityRendererProvider.Context pContext) {
        this.bookModel = new BookModel(pContext.bakeLayer(ModelLayers.BOOK));
    }

    public void render(LecternBlockEntity pBlockEntity, float pPartialTick, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
        BlockState blockstate = pBlockEntity.getBlockState();
        if (blockstate.getValue(LecternBlock.HAS_BOOK)) {
            pPoseStack.pushPose();
            pPoseStack.translate(0.5F, 1.0625F, 0.5F);
            float f = blockstate.getValue(LecternBlock.FACING).getClockWise().toYRot();
            pPoseStack.mulPose(Axis.YP.rotationDegrees(-f));
            pPoseStack.mulPose(Axis.ZP.rotationDegrees(67.5F));
            pPoseStack.translate(0.0F, -0.125F, 0.0F);
            this.bookModel.setupAnim(0.0F, 0.1F, 0.9F, 1.2F);
            VertexConsumer vertexconsumer = EnchantTableRenderer.BOOK_LOCATION.buffer(pBufferSource, RenderType::entitySolid);
            this.bookModel.render(pPoseStack, vertexconsumer, pPackedLight, pPackedOverlay, -1);
            pPoseStack.popPose();
        }
    }
}