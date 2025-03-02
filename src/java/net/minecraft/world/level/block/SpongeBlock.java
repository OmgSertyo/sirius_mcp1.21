package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public class SpongeBlock extends Block {
    public static final MapCodec<SpongeBlock> CODEC = simpleCodec(SpongeBlock::new);
    public static final int MAX_DEPTH = 6;
    public static final int MAX_COUNT = 64;
    private static final Direction[] ALL_DIRECTIONS = Direction.values();

    @Override
    public MapCodec<SpongeBlock> codec() {
        return CODEC;
    }

    protected SpongeBlock(BlockBehaviour.Properties p_56796_) {
        super(p_56796_);
    }

    @Override
    protected void onPlace(BlockState pState, Level pLevel, BlockPos pPos, BlockState pOldState, boolean pIsMoving) {
        if (!pOldState.is(pState.getBlock())) {
            this.tryAbsorbWater(pLevel, pPos);
        }
    }

    @Override
    protected void neighborChanged(BlockState pState, Level pLevel, BlockPos pPos, Block pBlock, BlockPos pFromPos, boolean pIsMoving) {
        this.tryAbsorbWater(pLevel, pPos);
        super.neighborChanged(pState, pLevel, pPos, pBlock, pFromPos, pIsMoving);
    }

    protected void tryAbsorbWater(Level pLevel, BlockPos pPos) {
        if (this.removeWaterBreadthFirstSearch(pLevel, pPos)) {
            pLevel.setBlock(pPos, Blocks.WET_SPONGE.defaultBlockState(), 2);
            pLevel.playSound(null, pPos, SoundEvents.SPONGE_ABSORB, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    private boolean removeWaterBreadthFirstSearch(Level pLevel, BlockPos pPos) {
        return BlockPos.breadthFirstTraversal(
                pPos,
                6,
                65,
                (p_277519_, p_277492_) -> {
                    for (Direction direction : ALL_DIRECTIONS) {
                        p_277492_.accept(p_277519_.relative(direction));
                    }
                },
                p_296944_ -> {
                    if (p_296944_.equals(pPos)) {
                        return true;
                    } else {
                        BlockState blockstate = pLevel.getBlockState(p_296944_);
                        FluidState fluidstate = pLevel.getFluidState(p_296944_);
                        if (!fluidstate.is(FluidTags.WATER)) {
                            return false;
                        } else {
                            if (blockstate.getBlock() instanceof BucketPickup bucketpickup
                                && !bucketpickup.pickupBlock(null, pLevel, p_296944_, blockstate).isEmpty()) {
                                return true;
                            }

                            if (blockstate.getBlock() instanceof LiquidBlock) {
                                pLevel.setBlock(p_296944_, Blocks.AIR.defaultBlockState(), 3);
                            } else {
                                if (!blockstate.is(Blocks.KELP)
                                    && !blockstate.is(Blocks.KELP_PLANT)
                                    && !blockstate.is(Blocks.SEAGRASS)
                                    && !blockstate.is(Blocks.TALL_SEAGRASS)) {
                                    return false;
                                }

                                BlockEntity blockentity = blockstate.hasBlockEntity() ? pLevel.getBlockEntity(p_296944_) : null;
                                dropResources(blockstate, pLevel, p_296944_, blockentity);
                                pLevel.setBlock(p_296944_, Blocks.AIR.defaultBlockState(), 3);
                            }

                            return true;
                        }
                    }
                }
            )
            > 1;
    }
}