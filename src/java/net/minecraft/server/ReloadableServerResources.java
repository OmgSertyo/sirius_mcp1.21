package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.LayeredRegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleReloadInstance;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.TagManager;
import net.minecraft.util.Unit;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.slf4j.Logger;

public class ReloadableServerResources {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final CompletableFuture<Unit> DATA_RELOAD_INITIAL_TASK = CompletableFuture.completedFuture(Unit.INSTANCE);
    private final ReloadableServerRegistries.Holder fullRegistryHolder;
    private final ReloadableServerResources.ConfigurableRegistryLookup registryLookup;
    private final Commands commands;
    private final RecipeManager recipes;
    private final TagManager tagManager;
    private final ServerAdvancementManager advancements;
    private final ServerFunctionLibrary functionLibrary;

    private ReloadableServerResources(RegistryAccess.Frozen pRegistryAccess, FeatureFlagSet pEnabledFeatures, Commands.CommandSelection pCommandSelection, int pFunctionCompilationLevel) {
        this.fullRegistryHolder = new ReloadableServerRegistries.Holder(pRegistryAccess);
        this.registryLookup = new ReloadableServerResources.ConfigurableRegistryLookup(pRegistryAccess);
        this.registryLookup.missingTagAccessPolicy(ReloadableServerResources.MissingTagAccessPolicy.CREATE_NEW);
        this.recipes = new RecipeManager(this.registryLookup);
        this.tagManager = new TagManager(pRegistryAccess);
        this.commands = new Commands(pCommandSelection, CommandBuildContext.simple(this.registryLookup, pEnabledFeatures));
        this.advancements = new ServerAdvancementManager(this.registryLookup);
        this.functionLibrary = new ServerFunctionLibrary(pFunctionCompilationLevel, this.commands.getDispatcher());
    }

    public ServerFunctionLibrary getFunctionLibrary() {
        return this.functionLibrary;
    }

    public ReloadableServerRegistries.Holder fullRegistries() {
        return this.fullRegistryHolder;
    }

    public RecipeManager getRecipeManager() {
        return this.recipes;
    }

    public Commands getCommands() {
        return this.commands;
    }

    public ServerAdvancementManager getAdvancements() {
        return this.advancements;
    }

    public List<PreparableReloadListener> listeners() {
        return List.of(this.tagManager, this.recipes, this.functionLibrary, this.advancements);
    }

    public static CompletableFuture<ReloadableServerResources> loadResources(
        ResourceManager pResourceManager,
        LayeredRegistryAccess<RegistryLayer> pRegistries,
        FeatureFlagSet pEnabledFeatures,
        Commands.CommandSelection pCommandSelection,
        int pFunctionCompilationLevel,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        return ReloadableServerRegistries.reload(pRegistries, pResourceManager, pBackgroundExecutor)
            .thenCompose(
                p_326196_ -> {
                    ReloadableServerResources reloadableserverresources = new ReloadableServerResources(p_326196_.compositeAccess(), pEnabledFeatures, pCommandSelection, pFunctionCompilationLevel);
                    return SimpleReloadInstance.create(
                            pResourceManager, reloadableserverresources.listeners(), pBackgroundExecutor, pGameExecutor, DATA_RELOAD_INITIAL_TASK, LOGGER.isDebugEnabled()
                        )
                        .done()
                        .whenComplete(
                            (p_326199_, p_326200_) -> reloadableserverresources.registryLookup.missingTagAccessPolicy(ReloadableServerResources.MissingTagAccessPolicy.FAIL)
                        )
                        .thenApply(p_214306_ -> reloadableserverresources);
                }
            );
    }

    public void updateRegistryTags() {
        this.tagManager.getResult().forEach(p_326197_ -> updateRegistryTags(this.fullRegistryHolder.get(), (TagManager.LoadResult<?>)p_326197_));
        AbstractFurnaceBlockEntity.invalidateCache();
        Blocks.rebuildCache();
    }

    private static <T> void updateRegistryTags(RegistryAccess pRegistryAccess, TagManager.LoadResult<T> pLoadResult) {
        ResourceKey<? extends Registry<T>> resourcekey = pLoadResult.key();
        Map<TagKey<T>, List<Holder<T>>> map = pLoadResult.tags()
            .entrySet()
            .stream()
            .collect(
                Collectors.toUnmodifiableMap(p_214303_ -> TagKey.create(resourcekey, p_214303_.getKey()), p_214312_ -> List.copyOf(p_214312_.getValue()))
            );
        pRegistryAccess.registryOrThrow(resourcekey).bindTags(map);
    }

    static class ConfigurableRegistryLookup implements HolderLookup.Provider {
        private final RegistryAccess registryAccess;
        ReloadableServerResources.MissingTagAccessPolicy missingTagAccessPolicy = ReloadableServerResources.MissingTagAccessPolicy.FAIL;

        ConfigurableRegistryLookup(RegistryAccess pRegistryAccess) {
            this.registryAccess = pRegistryAccess;
        }

        public void missingTagAccessPolicy(ReloadableServerResources.MissingTagAccessPolicy pMissingTagAccessPolicy) {
            this.missingTagAccessPolicy = pMissingTagAccessPolicy;
        }

        @Override
        public Stream<ResourceKey<? extends Registry<?>>> listRegistries() {
            return this.registryAccess.listRegistries();
        }

        @Override
        public <T> Optional<HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> pRegistryKey) {
            return this.registryAccess.registry(pRegistryKey).map(p_335756_ -> this.createDispatchedLookup(p_335756_.asLookup(), p_335756_.asTagAddingLookup()));
        }

        private <T> HolderLookup.RegistryLookup<T> createDispatchedLookup(final HolderLookup.RegistryLookup<T> pLookup, final HolderLookup.RegistryLookup<T> pTagAddingLookup) {
            return new HolderLookup.RegistryLookup.Delegate<T>() {
                @Override
                public HolderLookup.RegistryLookup<T> parent() {
                    return switch (ConfigurableRegistryLookup.this.missingTagAccessPolicy) {
                        case CREATE_NEW -> pTagAddingLookup;
                        case FAIL -> pLookup;
                    };
                }
            };
        }
    }

    static enum MissingTagAccessPolicy {
        CREATE_NEW,
        FAIL;
    }
}