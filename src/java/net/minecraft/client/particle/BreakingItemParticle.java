package net.minecraft.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class BreakingItemParticle extends TextureSheetParticle {
    private final float uo;
    private final float vo;

    BreakingItemParticle(
        ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, ItemStack pStack
    ) {
        this(pLevel, pX, pY, pZ, pStack);
        this.xd *= 0.1F;
        this.yd *= 0.1F;
        this.zd *= 0.1F;
        this.xd += pXSpeed;
        this.yd += pYSpeed;
        this.zd += pZSpeed;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.TERRAIN_SHEET;
    }

    protected BreakingItemParticle(ClientLevel pLevel, double pX, double pY, double pZ, ItemStack pStack) {
        super(pLevel, pX, pY, pZ, 0.0, 0.0, 0.0);
        this.setSprite(Minecraft.getInstance().getItemRenderer().getModel(pStack, pLevel, null, 0).getParticleIcon());
        this.gravity = 1.0F;
        this.quadSize /= 2.0F;
        this.uo = this.random.nextFloat() * 3.0F;
        this.vo = this.random.nextFloat() * 3.0F;
    }

    @Override
    protected float getU0() {
        return this.sprite.getU((this.uo + 1.0F) / 4.0F);
    }

    @Override
    protected float getU1() {
        return this.sprite.getU(this.uo / 4.0F);
    }

    @Override
    protected float getV0() {
        return this.sprite.getV(this.vo / 4.0F);
    }

    @Override
    protected float getV1() {
        return this.sprite.getV((this.vo + 1.0F) / 4.0F);
    }

    public static class CobwebProvider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType p_329960_,
            ClientLevel p_334942_,
            double p_332141_,
            double p_335808_,
            double p_331451_,
            double p_330404_,
            double p_335788_,
            double p_329792_
        ) {
            return new BreakingItemParticle(p_334942_, p_332141_, p_335808_, p_331451_, new ItemStack(Items.COBWEB));
        }
    }

    public static class Provider implements ParticleProvider<ItemParticleOption> {
        public Particle createParticle(
            ItemParticleOption pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return new BreakingItemParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, pType.getItem());
        }
    }

    public static class SlimeProvider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return new BreakingItemParticle(pLevel, pX, pY, pZ, new ItemStack(Items.SLIME_BALL));
        }
    }

    public static class SnowballProvider implements ParticleProvider<SimpleParticleType> {
        public Particle createParticle(
            SimpleParticleType pType,
            ClientLevel pLevel,
            double pX,
            double pY,
            double pZ,
            double pXSpeed,
            double pYSpeed,
            double pZSpeed
        ) {
            return new BreakingItemParticle(pLevel, pX, pY, pZ, new ItemStack(Items.SNOWBALL));
        }
    }
}