package net.minecraft.client.resources.model;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Util;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import org.slf4j.Logger;

public class ModelManager implements PreparableReloadListener, AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, ResourceLocation> VANILLA_ATLASES = Map.of(
        Sheets.BANNER_SHEET,
        ResourceLocation.withDefaultNamespace("banner_patterns"),
        Sheets.BED_SHEET,
        ResourceLocation.withDefaultNamespace("beds"),
        Sheets.CHEST_SHEET,
        ResourceLocation.withDefaultNamespace("chests"),
        Sheets.SHIELD_SHEET,
        ResourceLocation.withDefaultNamespace("shield_patterns"),
        Sheets.SIGN_SHEET,
        ResourceLocation.withDefaultNamespace("signs"),
        Sheets.SHULKER_SHEET,
        ResourceLocation.withDefaultNamespace("shulker_boxes"),
        Sheets.ARMOR_TRIMS_SHEET,
        ResourceLocation.withDefaultNamespace("armor_trims"),
        Sheets.DECORATED_POT_SHEET,
        ResourceLocation.withDefaultNamespace("decorated_pot"),
        TextureAtlas.LOCATION_BLOCKS,
        ResourceLocation.withDefaultNamespace("blocks")
    );
    private Map<ModelResourceLocation, BakedModel> bakedRegistry;
    private final AtlasSet atlases;
    private final BlockModelShaper blockModelShaper;
    private final BlockColors blockColors;
    private int maxMipmapLevels;
    private BakedModel missingModel;
    private Object2IntMap<BlockState> modelGroups;

    public ModelManager(TextureManager pTextureManager, BlockColors pBlockColors, int pMaxMipmapLevels) {
        this.blockColors = pBlockColors;
        this.maxMipmapLevels = pMaxMipmapLevels;
        this.blockModelShaper = new BlockModelShaper(this);
        this.atlases = new AtlasSet(VANILLA_ATLASES, pTextureManager);
    }

    public BakedModel getModel(ModelResourceLocation pModelLocation) {
        return this.bakedRegistry.getOrDefault(pModelLocation, this.missingModel);
    }

    public BakedModel getMissingModel() {
        return this.missingModel;
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    @Override
    public final CompletableFuture<Void> reload(
        PreparableReloadListener.PreparationBarrier pPreparationBarrier,
        ResourceManager pResourceManager,
        ProfilerFiller pPreparationsProfiler,
        ProfilerFiller pReloadProfiler,
        Executor pBackgroundExecutor,
        Executor pGameExecutor
    ) {
        pPreparationsProfiler.startTick();
        CompletableFuture<Map<ResourceLocation, BlockModel>> completablefuture = loadBlockModels(pResourceManager, pBackgroundExecutor);
        CompletableFuture<Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>> completablefuture1 = loadBlockStates(pResourceManager, pBackgroundExecutor);
        CompletableFuture<ModelBakery> completablefuture2 = completablefuture.thenCombineAsync(
            completablefuture1,
            (p_251201_, p_251281_) -> new ModelBakery(
                    this.blockColors,
                    pPreparationsProfiler,
                    (Map<ResourceLocation, BlockModel>)p_251201_,
                    (Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>)p_251281_
                ),
            pBackgroundExecutor
        );
        Map<ResourceLocation, CompletableFuture<AtlasSet.StitchResult>> map = this.atlases.scheduleLoad(pResourceManager, this.maxMipmapLevels, pBackgroundExecutor);
        return CompletableFuture.allOf(Stream.concat(map.values().stream(), Stream.of(completablefuture2)).toArray(CompletableFuture[]::new))
            .thenApplyAsync(
                p_248624_ -> this.loadModels(
                        pPreparationsProfiler,
                        map.entrySet().stream().collect(Collectors.toMap(Entry::getKey, p_248988_ -> p_248988_.getValue().join())),
                        completablefuture2.join()
                    ),
                pBackgroundExecutor
            )
            .thenCompose(p_252255_ -> p_252255_.readyForUpload.thenApply(p_251581_ -> (ModelManager.ReloadState)p_252255_))
            .thenCompose(pPreparationBarrier::wait)
            .thenAcceptAsync(p_252252_ -> this.apply(p_252252_, pReloadProfiler), pGameExecutor);
    }

    private static CompletableFuture<Map<ResourceLocation, BlockModel>> loadBlockModels(ResourceManager pResourceManager, Executor pExecutor) {
        return CompletableFuture.<Map<ResourceLocation, Resource>>supplyAsync(() -> ModelBakery.MODEL_LISTER.listMatchingResources(pResourceManager), pExecutor)
            .thenCompose(
                p_250597_ -> {
                    List<CompletableFuture<Pair<ResourceLocation, BlockModel>>> list = new ArrayList<>(p_250597_.size());

                    for (Entry<ResourceLocation, Resource> entry : p_250597_.entrySet()) {
                        list.add(CompletableFuture.supplyAsync(() -> {
                            try {
                                Pair pair;
                                try (Reader reader = entry.getValue().openAsReader()) {
                                    pair = Pair.of(entry.getKey(), BlockModel.fromStream(reader));
                                }

                                return pair;
                            } catch (Exception exception) {
                                LOGGER.error("Failed to load model {}", entry.getKey(), exception);
                                return null;
                            }
                        }, pExecutor));
                    }

                    return Util.sequence(list)
                        .thenApply(
                            p_250813_ -> p_250813_.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond))
                        );
                }
            );
    }

    private static CompletableFuture<Map<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>> loadBlockStates(ResourceManager pResourceManager, Executor pExecutor) {
        return CompletableFuture.<Map<ResourceLocation, List<Resource>>>supplyAsync(() -> BlockStateModelLoader.BLOCKSTATE_LISTER.listMatchingResourceStacks(pResourceManager), pExecutor)
            .thenCompose(
                p_250744_ -> {
                    List<CompletableFuture<Pair<ResourceLocation, List<BlockStateModelLoader.LoadedJson>>>> list = new ArrayList<>(p_250744_.size());

                    for (Entry<ResourceLocation, List<Resource>> entry : p_250744_.entrySet()) {
                        list.add(CompletableFuture.supplyAsync(() -> {
                            List<Resource> list1 = entry.getValue();
                            List<BlockStateModelLoader.LoadedJson> list2 = new ArrayList<>(list1.size());

                            for (Resource resource : list1) {
                                try (Reader reader = resource.openAsReader()) {
                                    JsonObject jsonobject = GsonHelper.parse(reader);
                                    list2.add(new BlockStateModelLoader.LoadedJson(resource.sourcePackId(), jsonobject));
                                } catch (Exception exception) {
                                    LOGGER.error("Failed to load blockstate {} from pack {}", entry.getKey(), resource.sourcePackId(), exception);
                                }
                            }

                            return Pair.of(entry.getKey(), list2);
                        }, pExecutor));
                    }

                    return Util.sequence(list)
                        .thenApply(
                            p_248966_ -> p_248966_.stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond))
                        );
                }
            );
    }

    private ModelManager.ReloadState loadModels(ProfilerFiller pProfilerFiller, Map<ResourceLocation, AtlasSet.StitchResult> pAtlasPreparations, ModelBakery pModelBakery) {
        pProfilerFiller.push("load");
        pProfilerFiller.popPush("baking");
        Multimap<ModelResourceLocation, Material> multimap = HashMultimap.create();
        pModelBakery.bakeModels((p_343412_, p_251262_) -> {
            AtlasSet.StitchResult atlasset$stitchresult = pAtlasPreparations.get(p_251262_.atlasLocation());
            TextureAtlasSprite textureatlassprite = atlasset$stitchresult.getSprite(p_251262_.texture());
            if (textureatlassprite != null) {
                return textureatlassprite;
            } else {
                multimap.put(p_343412_, p_251262_);
                return atlasset$stitchresult.missing();
            }
        });
        multimap.asMap()
            .forEach(
                (p_344983_, p_252017_) -> LOGGER.warn(
                        "Missing textures in model {}:\n{}",
                        p_344983_,
                        p_252017_.stream()
                            .sorted(Material.COMPARATOR)
                            .map(p_325574_ -> "    " + p_325574_.atlasLocation() + ":" + p_325574_.texture())
                            .collect(Collectors.joining("\n"))
                    )
            );
        pProfilerFiller.popPush("dispatch");
        Map<ModelResourceLocation, BakedModel> map = pModelBakery.getBakedTopLevelModels();
        BakedModel bakedmodel = map.get(ModelBakery.MISSING_MODEL_VARIANT);
        Map<BlockState, BakedModel> map1 = new IdentityHashMap<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            block.getStateDefinition().getPossibleStates().forEach(p_250633_ -> {
                ResourceLocation resourcelocation = p_250633_.getBlock().builtInRegistryHolder().key().location();
                BakedModel bakedmodel1 = map.getOrDefault(BlockModelShaper.stateToModelLocation(resourcelocation, p_250633_), bakedmodel);
                map1.put(p_250633_, bakedmodel1);
            });
        }

        CompletableFuture<Void> completablefuture = CompletableFuture.allOf(
            pAtlasPreparations.values().stream().map(AtlasSet.StitchResult::readyForUpload).toArray(CompletableFuture[]::new)
        );
        pProfilerFiller.pop();
        pProfilerFiller.endTick();
        return new ModelManager.ReloadState(pModelBakery, bakedmodel, map1, pAtlasPreparations, completablefuture);
    }

    private void apply(ModelManager.ReloadState pReloadState, ProfilerFiller pProfiler) {
        pProfiler.startTick();
        pProfiler.push("upload");
        pReloadState.atlasPreparations.values().forEach(AtlasSet.StitchResult::upload);
        ModelBakery modelbakery = pReloadState.modelBakery;
        this.bakedRegistry = modelbakery.getBakedTopLevelModels();
        this.modelGroups = modelbakery.getModelGroups();
        this.missingModel = pReloadState.missingModel;
        pProfiler.popPush("cache");
        this.blockModelShaper.replaceCache(pReloadState.modelCache);
        pProfiler.pop();
        pProfiler.endTick();
    }

    public boolean requiresRender(BlockState pOldState, BlockState pNewState) {
        if (pOldState == pNewState) {
            return false;
        } else {
            int i = this.modelGroups.getInt(pOldState);
            if (i != -1) {
                int j = this.modelGroups.getInt(pNewState);
                if (i == j) {
                    FluidState fluidstate = pOldState.getFluidState();
                    FluidState fluidstate1 = pNewState.getFluidState();
                    return fluidstate != fluidstate1;
                }
            }

            return true;
        }
    }

    public TextureAtlas getAtlas(ResourceLocation pLocation) {
        return this.atlases.getAtlas(pLocation);
    }

    @Override
    public void close() {
        this.atlases.close();
    }

    public void updateMaxMipLevel(int pLevel) {
        this.maxMipmapLevels = pLevel;
    }

    static record ReloadState(
        ModelBakery modelBakery,
        BakedModel missingModel,
        Map<BlockState, BakedModel> modelCache,
        Map<ResourceLocation, AtlasSet.StitchResult> atlasPreparations,
        CompletableFuture<Void> readyForUpload
    ) {
    }
}