package net.minecraft.world.item;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.ItemLike;

public class CreativeModeTab {
    static final ResourceLocation DEFAULT_BACKGROUND = createTextureLocation("items");
    private final Component displayName;
    ResourceLocation backgroundTexture = DEFAULT_BACKGROUND;
    boolean canScroll = true;
    boolean showTitle = true;
    boolean alignedRight = false;
    private final CreativeModeTab.Row row;
    private final int column;
    private final CreativeModeTab.Type type;
    @Nullable
    private ItemStack iconItemStack;
    private Collection<ItemStack> displayItems = ItemStackLinkedSet.createTypeAndComponentsSet();
    private Set<ItemStack> displayItemsSearchTab = ItemStackLinkedSet.createTypeAndComponentsSet();
    private final Supplier<ItemStack> iconGenerator;
    private final CreativeModeTab.DisplayItemsGenerator displayItemsGenerator;

    CreativeModeTab(
        CreativeModeTab.Row pRow,
        int pColumn,
        CreativeModeTab.Type pType,
        Component pDisplayName,
        Supplier<ItemStack> pIconGenerator,
        CreativeModeTab.DisplayItemsGenerator pDisplayItemGenerator
    ) {
        this.row = pRow;
        this.column = pColumn;
        this.displayName = pDisplayName;
        this.iconGenerator = pIconGenerator;
        this.displayItemsGenerator = pDisplayItemGenerator;
        this.type = pType;
    }

    public static ResourceLocation createTextureLocation(String pName) {
        return ResourceLocation.withDefaultNamespace("textures/gui/container/creative_inventory/tab_" + pName + ".png");
    }

    public static CreativeModeTab.Builder builder(CreativeModeTab.Row pRow, int pColumn) {
        return new CreativeModeTab.Builder(pRow, pColumn);
    }

    public Component getDisplayName() {
        return this.displayName;
    }

    public ItemStack getIconItem() {
        if (this.iconItemStack == null) {
            this.iconItemStack = this.iconGenerator.get();
        }

        return this.iconItemStack;
    }

    public ResourceLocation getBackgroundTexture() {
        return this.backgroundTexture;
    }

    public boolean showTitle() {
        return this.showTitle;
    }

    public boolean canScroll() {
        return this.canScroll;
    }

    public int column() {
        return this.column;
    }

    public CreativeModeTab.Row row() {
        return this.row;
    }

    public boolean hasAnyItems() {
        return !this.displayItems.isEmpty();
    }

    public boolean shouldDisplay() {
        return this.type != CreativeModeTab.Type.CATEGORY || this.hasAnyItems();
    }

    public boolean isAlignedRight() {
        return this.alignedRight;
    }

    public CreativeModeTab.Type getType() {
        return this.type;
    }

    public void buildContents(CreativeModeTab.ItemDisplayParameters pParameters) {
        CreativeModeTab.ItemDisplayBuilder creativemodetab$itemdisplaybuilder = new CreativeModeTab.ItemDisplayBuilder(this, pParameters.enabledFeatures);
        ResourceKey<CreativeModeTab> resourcekey = BuiltInRegistries.CREATIVE_MODE_TAB
            .getResourceKey(this)
            .orElseThrow(() -> new IllegalStateException("Unregistered creative tab: " + this));
        this.displayItemsGenerator.accept(pParameters, creativemodetab$itemdisplaybuilder);
        this.displayItems = creativemodetab$itemdisplaybuilder.tabContents;
        this.displayItemsSearchTab = creativemodetab$itemdisplaybuilder.searchTabContents;
    }

    public Collection<ItemStack> getDisplayItems() {
        return this.displayItems;
    }

    public Collection<ItemStack> getSearchTabDisplayItems() {
        return this.displayItemsSearchTab;
    }

    public boolean contains(ItemStack pStack) {
        return this.displayItemsSearchTab.contains(pStack);
    }

    public static class Builder {
        private static final CreativeModeTab.DisplayItemsGenerator EMPTY_GENERATOR = (p_270422_, p_259433_) -> {
        };
        private final CreativeModeTab.Row row;
        private final int column;
        private Component displayName = Component.empty();
        private Supplier<ItemStack> iconGenerator = () -> ItemStack.EMPTY;
        private CreativeModeTab.DisplayItemsGenerator displayItemsGenerator = EMPTY_GENERATOR;
        private boolean canScroll = true;
        private boolean showTitle = true;
        private boolean alignedRight = false;
        private CreativeModeTab.Type type = CreativeModeTab.Type.CATEGORY;
        private ResourceLocation backgroundTexture = CreativeModeTab.DEFAULT_BACKGROUND;

        public Builder(CreativeModeTab.Row pRow, int pColumn) {
            this.row = pRow;
            this.column = pColumn;
        }

        public CreativeModeTab.Builder title(Component pTitle) {
            this.displayName = pTitle;
            return this;
        }

        public CreativeModeTab.Builder icon(Supplier<ItemStack> pIcon) {
            this.iconGenerator = pIcon;
            return this;
        }

        public CreativeModeTab.Builder displayItems(CreativeModeTab.DisplayItemsGenerator pDisplayItemsGenerator) {
            this.displayItemsGenerator = pDisplayItemsGenerator;
            return this;
        }

        public CreativeModeTab.Builder alignedRight() {
            this.alignedRight = true;
            return this;
        }

        public CreativeModeTab.Builder hideTitle() {
            this.showTitle = false;
            return this;
        }

        public CreativeModeTab.Builder noScrollBar() {
            this.canScroll = false;
            return this;
        }

        protected CreativeModeTab.Builder type(CreativeModeTab.Type pType) {
            this.type = pType;
            return this;
        }

        public CreativeModeTab.Builder backgroundTexture(ResourceLocation pBackgroundTexture) {
            this.backgroundTexture = pBackgroundTexture;
            return this;
        }

        public CreativeModeTab build() {
            if ((this.type == CreativeModeTab.Type.HOTBAR || this.type == CreativeModeTab.Type.INVENTORY) && this.displayItemsGenerator != EMPTY_GENERATOR) {
                throw new IllegalStateException("Special tabs can't have display items");
            } else {
                CreativeModeTab creativemodetab = new CreativeModeTab(
                    this.row, this.column, this.type, this.displayName, this.iconGenerator, this.displayItemsGenerator
                );
                creativemodetab.alignedRight = this.alignedRight;
                creativemodetab.showTitle = this.showTitle;
                creativemodetab.canScroll = this.canScroll;
                creativemodetab.backgroundTexture = this.backgroundTexture;
                return creativemodetab;
            }
        }
    }

    @FunctionalInterface
    public interface DisplayItemsGenerator {
        void accept(CreativeModeTab.ItemDisplayParameters pParameters, CreativeModeTab.Output pOutput);
    }

    static class ItemDisplayBuilder implements CreativeModeTab.Output {
        public final Collection<ItemStack> tabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
        public final Set<ItemStack> searchTabContents = ItemStackLinkedSet.createTypeAndComponentsSet();
        private final CreativeModeTab tab;
        private final FeatureFlagSet featureFlagSet;

        public ItemDisplayBuilder(CreativeModeTab pTab, FeatureFlagSet pFeatureFlagSet) {
            this.tab = pTab;
            this.featureFlagSet = pFeatureFlagSet;
        }

        @Override
        public void accept(ItemStack pStack, CreativeModeTab.TabVisibility pTabVisibility) {
            if (pStack.getCount() != 1) {
                throw new IllegalArgumentException("Stack size must be exactly 1");
            } else {
                boolean flag = this.tabContents.contains(pStack) && pTabVisibility != CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY;
                if (flag) {
                    throw new IllegalStateException(
                        "Accidentally adding the same item stack twice "
                            + pStack.getDisplayName().getString()
                            + " to a Creative Mode Tab: "
                            + this.tab.getDisplayName().getString()
                    );
                } else {
                    if (pStack.getItem().isEnabled(this.featureFlagSet)) {
                        switch (pTabVisibility) {
                            case PARENT_AND_SEARCH_TABS:
                                this.tabContents.add(pStack);
                                this.searchTabContents.add(pStack);
                                break;
                            case PARENT_TAB_ONLY:
                                this.tabContents.add(pStack);
                                break;
                            case SEARCH_TAB_ONLY:
                                this.searchTabContents.add(pStack);
                        }
                    }
                }
            }
        }
    }

    public static record ItemDisplayParameters(FeatureFlagSet enabledFeatures, boolean hasPermissions, HolderLookup.Provider holders) {
        public boolean needsUpdate(FeatureFlagSet pEnabledFeatures, boolean pHasPermissions, HolderLookup.Provider pHolders) {
            return !this.enabledFeatures.equals(pEnabledFeatures) || this.hasPermissions != pHasPermissions || this.holders != pHolders;
        }
    }

    public interface Output {
        void accept(ItemStack pStack, CreativeModeTab.TabVisibility pTabVisibility);

        default void accept(ItemStack pStack) {
            this.accept(pStack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        default void accept(ItemLike pItem, CreativeModeTab.TabVisibility pTabVisibility) {
            this.accept(new ItemStack(pItem), pTabVisibility);
        }

        default void accept(ItemLike pItem) {
            this.accept(new ItemStack(pItem), CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        default void acceptAll(Collection<ItemStack> pStacks, CreativeModeTab.TabVisibility pTabVisibility) {
            pStacks.forEach(p_252337_ -> this.accept(p_252337_, pTabVisibility));
        }

        default void acceptAll(Collection<ItemStack> pStacks) {
            this.acceptAll(pStacks, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }

    public static enum Row {
        TOP,
        BOTTOM;
    }

    protected static enum TabVisibility {
        PARENT_AND_SEARCH_TABS,
        PARENT_TAB_ONLY,
        SEARCH_TAB_ONLY;
    }

    public static enum Type {
        CATEGORY,
        INVENTORY,
        HOTBAR,
        SEARCH;
    }
}