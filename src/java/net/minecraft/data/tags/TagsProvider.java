package net.minecraft.data.tags;

import com.google.common.collect.Maps;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagBuilder;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;
import net.minecraft.tags.TagKey;

public abstract class TagsProvider<T> implements DataProvider {
    protected final PackOutput.PathProvider pathProvider;
    private final CompletableFuture<HolderLookup.Provider> lookupProvider;
    private final CompletableFuture<Void> contentsDone = new CompletableFuture<>();
    private final CompletableFuture<TagsProvider.TagLookup<T>> parentProvider;
    protected final ResourceKey<? extends Registry<T>> registryKey;
    private final Map<ResourceLocation, TagBuilder> builders = Maps.newLinkedHashMap();

    protected TagsProvider(PackOutput pOutput, ResourceKey<? extends Registry<T>> pRegistryKey, CompletableFuture<HolderLookup.Provider> pLookupProvider) {
        this(pOutput, pRegistryKey, pLookupProvider, CompletableFuture.completedFuture(TagsProvider.TagLookup.empty()));
    }

    protected TagsProvider(
        PackOutput pOutput,
        ResourceKey<? extends Registry<T>> pRegistryKey,
        CompletableFuture<HolderLookup.Provider> pLookupProvider,
        CompletableFuture<TagsProvider.TagLookup<T>> pParentProvider
    ) {
        this.pathProvider = pOutput.createRegistryTagsPathProvider(pRegistryKey);
        this.registryKey = pRegistryKey;
        this.parentProvider = pParentProvider;
        this.lookupProvider = pLookupProvider;
    }

    @Override
    public final String getName() {
        return "Tags for " + this.registryKey.location();
    }

    protected abstract void addTags(HolderLookup.Provider pProvider);

    @Override
    public CompletableFuture<?> run(CachedOutput pOutput) {
        record CombinedData<T>(HolderLookup.Provider contents, TagsProvider.TagLookup<T> parent) {
        }

        return this.createContentsProvider()
            .thenApply(p_275895_ -> {
                this.contentsDone.complete(null);
                return (HolderLookup.Provider)p_275895_;
            })
            .thenCombineAsync(this.parentProvider, (p_274778_, p_274779_) -> new CombinedData<>(p_274778_, (TagsProvider.TagLookup<T>)p_274779_), Util.backgroundExecutor())
            .thenCompose(
                p_325926_ -> {
                    HolderLookup.RegistryLookup<T> registrylookup = p_325926_.contents.lookupOrThrow(this.registryKey);
                    Predicate<ResourceLocation> predicate = p_255496_ -> registrylookup.get(ResourceKey.create(this.registryKey, p_255496_)).isPresent();
                    Predicate<ResourceLocation> predicate1 = p_274776_ -> this.builders.containsKey(p_274776_)
                            || p_325926_.parent.contains(TagKey.create(this.registryKey, p_274776_));
                    return CompletableFuture.allOf(
                        this.builders
                            .entrySet()
                            .stream()
                            .map(
                                p_325931_ -> {
                                    ResourceLocation resourcelocation = p_325931_.getKey();
                                    TagBuilder tagbuilder = p_325931_.getValue();
                                    List<TagEntry> list = tagbuilder.build();
                                    List<TagEntry> list1 = list.stream().filter(p_274771_ -> !p_274771_.verifyIfPresent(predicate, predicate1)).toList();
                                    if (!list1.isEmpty()) {
                                        throw new IllegalArgumentException(
                                            String.format(
                                                Locale.ROOT,
                                                "Couldn't define tag %s as it is missing following references: %s",
                                                resourcelocation,
                                                list1.stream().map(Objects::toString).collect(Collectors.joining(","))
                                            )
                                        );
                                    } else {
                                        Path path = this.pathProvider.json(resourcelocation);
                                        return DataProvider.saveStable(pOutput, p_325926_.contents, TagFile.CODEC, new TagFile(list, false), path);
                                    }
                                }
                            )
                            .toArray(CompletableFuture[]::new)
                    );
                }
            );
    }

    protected TagsProvider.TagAppender<T> tag(TagKey<T> pTag) {
        TagBuilder tagbuilder = this.getOrCreateRawBuilder(pTag);
        return new TagsProvider.TagAppender<>(tagbuilder);
    }

    protected TagBuilder getOrCreateRawBuilder(TagKey<T> pTag) {
        return this.builders.computeIfAbsent(pTag.location(), p_236442_ -> TagBuilder.create());
    }

    public CompletableFuture<TagsProvider.TagLookup<T>> contentsGetter() {
        return this.contentsDone.thenApply(p_276016_ -> p_274772_ -> Optional.ofNullable(this.builders.get(p_274772_.location())));
    }

    protected CompletableFuture<HolderLookup.Provider> createContentsProvider() {
        return this.lookupProvider.thenApply(p_274768_ -> {
            this.builders.clear();
            this.addTags(p_274768_);
            return (HolderLookup.Provider)p_274768_;
        });
    }

    protected static class TagAppender<T> {
        private final TagBuilder builder;

        protected TagAppender(TagBuilder pBuilder) {
            this.builder = pBuilder;
        }

        public final TagsProvider.TagAppender<T> add(ResourceKey<T> pKey) {
            this.builder.addElement(pKey.location());
            return this;
        }

        @SafeVarargs
        public final TagsProvider.TagAppender<T> add(ResourceKey<T>... pKeys) {
            for (ResourceKey<T> resourcekey : pKeys) {
                this.builder.addElement(resourcekey.location());
            }

            return this;
        }

        public final TagsProvider.TagAppender<T> addAll(List<ResourceKey<T>> pKeys) {
            for (ResourceKey<T> resourcekey : pKeys) {
                this.builder.addElement(resourcekey.location());
            }

            return this;
        }

        public TagsProvider.TagAppender<T> addOptional(ResourceLocation pLocation) {
            this.builder.addOptionalElement(pLocation);
            return this;
        }

        public TagsProvider.TagAppender<T> addTag(TagKey<T> pTag) {
            this.builder.addTag(pTag.location());
            return this;
        }

        public TagsProvider.TagAppender<T> addOptionalTag(ResourceLocation pLocation) {
            this.builder.addOptionalTag(pLocation);
            return this;
        }
    }

    @FunctionalInterface
    public interface TagLookup<T> extends Function<TagKey<T>, Optional<TagBuilder>> {
        static <T> TagsProvider.TagLookup<T> empty() {
            return p_275247_ -> Optional.empty();
        }

        default boolean contains(TagKey<T> pKey) {
            return this.apply(pKey).isPresent();
        }
    }
}