package net.minecraft.resources;

import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ExtraCodecs;

public class HolderSetCodec<E> implements Codec<HolderSet<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;
    private final Codec<Holder<E>> elementCodec;
    private final Codec<List<Holder<E>>> homogenousListCodec;
    private final Codec<Either<TagKey<E>, List<Holder<E>>>> registryAwareCodec;

    private static <E> Codec<List<Holder<E>>> homogenousList(Codec<Holder<E>> pHolderCodec, boolean pDisallowInline) {
        Codec<List<Holder<E>>> codec = pHolderCodec.listOf().validate(ExtraCodecs.ensureHomogenous(Holder::kind));
        return pDisallowInline
            ? codec
            : Codec.either(codec, pHolderCodec)
                .xmap(
                    p_206664_ -> p_206664_.map(p_206694_ -> p_206694_, List::of),
                    p_206684_ -> p_206684_.size() == 1 ? Either.right(p_206684_.get(0)) : Either.left((List<Holder<E>>)p_206684_)
                );
    }

    public static <E> Codec<HolderSet<E>> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<Holder<E>> pHolderCodec, boolean pDisallowInline) {
        return new HolderSetCodec<>(pRegistryKey, pHolderCodec, pDisallowInline);
    }

    private HolderSetCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<Holder<E>> pElementCodec, boolean pDisallowInline) {
        this.registryKey = pRegistryKey;
        this.elementCodec = pElementCodec;
        this.homogenousListCodec = homogenousList(pElementCodec, pDisallowInline);
        this.registryAwareCodec = Codec.either(TagKey.hashedCodec(pRegistryKey), this.homogenousListCodec);
    }

    @Override
    public <T> DataResult<Pair<HolderSet<E>, T>> decode(DynamicOps<T> pOps, T pInput) {
        if (pOps instanceof RegistryOps<T> registryops) {
            Optional<HolderGetter<E>> optional = registryops.getter(this.registryKey);
            if (optional.isPresent()) {
                HolderGetter<E> holdergetter = optional.get();
                return this.registryAwareCodec
                    .decode(pOps, pInput)
                    .flatMap(
                        p_326147_ -> {
                            DataResult<HolderSet<E>> dataresult = p_326147_.getFirst()
                                .map(
                                    p_326145_ -> lookupTag(holdergetter, (TagKey<E>)p_326145_),
                                    p_326140_ -> DataResult.success(HolderSet.direct((List<? extends Holder<E>>)p_326140_))
                                );
                            return dataresult.map(p_326149_ -> Pair.of((HolderSet<E>)p_326149_, (T)p_326147_.getSecond()));
                        }
                    );
            }
        }

        return this.decodeWithoutRegistry(pOps, pInput);
    }

    private static <E> DataResult<HolderSet<E>> lookupTag(HolderGetter<E> pInput, TagKey<E> pTagKey) {
        return (DataResult)pInput.get(pTagKey)
            .map(DataResult::success)
            .orElseGet(() -> DataResult.error(() -> "Missing tag: '" + pTagKey.location() + "' in '" + pTagKey.registry().location() + "'"));
    }

    public <T> DataResult<T> encode(HolderSet<E> pInput, DynamicOps<T> pOps, T pPrefix) {
        if (pOps instanceof RegistryOps<T> registryops) {
            Optional<HolderOwner<E>> optional = registryops.owner(this.registryKey);
            if (optional.isPresent()) {
                if (!pInput.canSerializeIn(optional.get())) {
                    return DataResult.error(() -> "HolderSet " + pInput + " is not valid in current registry set");
                }

                return this.registryAwareCodec.encode(pInput.unwrap().mapRight(List::copyOf), pOps, pPrefix);
            }
        }

        return this.encodeWithoutRegistry(pInput, pOps, pPrefix);
    }

    private <T> DataResult<Pair<HolderSet<E>, T>> decodeWithoutRegistry(DynamicOps<T> pOps, T pInput) {
        return this.elementCodec.listOf().decode(pOps, pInput).flatMap(p_206666_ -> {
            List<Holder.Direct<E>> list = new ArrayList<>();

            for (Holder<E> holder : p_206666_.getFirst()) {
                if (!(holder instanceof Holder.Direct<E> direct)) {
                    return DataResult.error(() -> "Can't decode element " + holder + " without registry");
                }

                list.add(direct);
            }

            return DataResult.success(new Pair<>(HolderSet.direct(list), p_206666_.getSecond()));
        });
    }

    private <T> DataResult<T> encodeWithoutRegistry(HolderSet<E> pInput, DynamicOps<T> pOps, T pPrefix) {
        return this.homogenousListCodec.encode(pInput.stream().toList(), pOps, pPrefix);
    }
}