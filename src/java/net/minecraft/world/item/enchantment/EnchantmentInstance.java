package net.minecraft.world.item.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.util.random.WeightedEntry;

public class EnchantmentInstance extends WeightedEntry.IntrusiveBase {
    public final Holder<Enchantment> enchantment;
    public final int level;

    public EnchantmentInstance(Holder<Enchantment> pEnchantment, int pLevel) {
        super(pEnchantment.value().getWeight());
        this.enchantment = pEnchantment;
        this.level = pLevel;
    }
}