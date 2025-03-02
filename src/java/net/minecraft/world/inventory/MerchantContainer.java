package net.minecraft.world.inventory;

import javax.annotation.Nullable;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public class MerchantContainer implements Container {
    private final Merchant merchant;
    private final NonNullList<ItemStack> itemStacks = NonNullList.withSize(3, ItemStack.EMPTY);
    @Nullable
    private MerchantOffer activeOffer;
    private int selectionHint;
    private int futureXp;

    public MerchantContainer(Merchant pMerchant) {
        this.merchant = pMerchant;
    }

    @Override
    public int getContainerSize() {
        return this.itemStacks.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int pIndex) {
        return this.itemStacks.get(pIndex);
    }

    @Override
    public ItemStack removeItem(int pIndex, int pCount) {
        ItemStack itemstack = this.itemStacks.get(pIndex);
        if (pIndex == 2 && !itemstack.isEmpty()) {
            return ContainerHelper.removeItem(this.itemStacks, pIndex, itemstack.getCount());
        } else {
            ItemStack itemstack1 = ContainerHelper.removeItem(this.itemStacks, pIndex, pCount);
            if (!itemstack1.isEmpty() && this.isPaymentSlot(pIndex)) {
                this.updateSellItem();
            }

            return itemstack1;
        }
    }

    private boolean isPaymentSlot(int pSlot) {
        return pSlot == 0 || pSlot == 1;
    }

    @Override
    public ItemStack removeItemNoUpdate(int pIndex) {
        return ContainerHelper.takeItem(this.itemStacks, pIndex);
    }

    @Override
    public void setItem(int pIndex, ItemStack pStack) {
        this.itemStacks.set(pIndex, pStack);
        pStack.limitSize(this.getMaxStackSize(pStack));
        if (this.isPaymentSlot(pIndex)) {
            this.updateSellItem();
        }
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return this.merchant.getTradingPlayer() == pPlayer;
    }

    @Override
    public void setChanged() {
        this.updateSellItem();
    }

    public void updateSellItem() {
        this.activeOffer = null;
        ItemStack itemstack;
        ItemStack itemstack1;
        if (this.itemStacks.get(0).isEmpty()) {
            itemstack = this.itemStacks.get(1);
            itemstack1 = ItemStack.EMPTY;
        } else {
            itemstack = this.itemStacks.get(0);
            itemstack1 = this.itemStacks.get(1);
        }

        if (itemstack.isEmpty()) {
            this.setItem(2, ItemStack.EMPTY);
            this.futureXp = 0;
        } else {
            MerchantOffers merchantoffers = this.merchant.getOffers();
            if (!merchantoffers.isEmpty()) {
                MerchantOffer merchantoffer = merchantoffers.getRecipeFor(itemstack, itemstack1, this.selectionHint);
                if (merchantoffer == null || merchantoffer.isOutOfStock()) {
                    this.activeOffer = merchantoffer;
                    merchantoffer = merchantoffers.getRecipeFor(itemstack1, itemstack, this.selectionHint);
                }

                if (merchantoffer != null && !merchantoffer.isOutOfStock()) {
                    this.activeOffer = merchantoffer;
                    this.setItem(2, merchantoffer.assemble());
                    this.futureXp = merchantoffer.getXp();
                } else {
                    this.setItem(2, ItemStack.EMPTY);
                    this.futureXp = 0;
                }
            }

            this.merchant.notifyTradeUpdated(this.getItem(2));
        }
    }

    @Nullable
    public MerchantOffer getActiveOffer() {
        return this.activeOffer;
    }

    public void setSelectionHint(int pCurrentRecipeIndex) {
        this.selectionHint = pCurrentRecipeIndex;
        this.updateSellItem();
    }

    @Override
    public void clearContent() {
        this.itemStacks.clear();
    }

    public int getFutureXp() {
        return this.futureXp;
    }
}