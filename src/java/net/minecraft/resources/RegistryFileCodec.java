package net.minecraft.resources;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Lifecycle;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;

public final class RegistryFileCodec<E> implements Codec<Holder<E>> {
    private final ResourceKey<? extends Registry<E>> registryKey;
    private final Codec<E> elementCodec;
    private final boolean allowInline;

    public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
        return create(pRegistryKey, pElementCodec, true);
    }

    public static <E> RegistryFileCodec<E> create(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pAllowInline) {
        return new RegistryFileCodec<>(pRegistryKey, pElementCodec, pAllowInline);
    }

    private RegistryFileCodec(ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, boolean pAllowInline) {
        this.registryKey = pRegistryKey;
        this.elementCodec = pElementCodec;
        this.allowInline = pAllowInline;
    }

    public <T> DataResult<T> encode(Holder<E> pInput, DynamicOps<T> pOps, T pPrefix) {
        if (pOps instanceof RegistryOps<?> registryops) {
            Optional<HolderOwner<E>> optional = registryops.owner(this.registryKey);
            if (optional.isPresent()) {
                if (!pInput.canSerializeIn(optional.get())) {
                    return DataResult.error(() -> "Element " + pInput + " is not valid in current registry set");
                }

                return pInput.unwrap()
                    .map(
                        p_206714_ -> ResourceLocation.CODEC.encode(p_206714_.location(), pOps, pPrefix),
                        p_206710_ -> this.elementCodec.encode((E)p_206710_, pOps, pPrefix)
                    );
            }
        }

        return this.elementCodec.encode(pInput.value(), pOps, pPrefix);
    }

    @Override
    public <T> DataResult<Pair<Holder<E>, T>> decode(DynamicOps<T> pOps, T pInput) {
        if (pOps instanceof RegistryOps<?> registryops) {
            Optional<HolderGetter<E>> optional = registryops.getter(this.registryKey);
            if (optional.isEmpty()) {
                return DataResult.error(() -> "Registry does not exist: " + this.registryKey);
            } else {
                HolderGetter<E> holdergetter = optional.get();
                DataResult<Pair<ResourceLocation, T>> dataresult = ResourceLocation.CODEC.decode(pOps, pInput);
                if (dataresult.result().isEmpty()) {
                    return !this.allowInline
                        ? DataResult.error(() -> "Inline definitions not allowed here")
                        : this.elementCodec.decode(pOps, pInput).map(p_206720_ -> p_206720_.mapFirst(Holder::direct));
                } else {
                    Pair<ResourceLocation, T> pair = dataresult.result().get();
                    ResourceKey<E> resourcekey = ResourceKey.create(this.registryKey, pair.getFirst());
                    return holdergetter.get(resourcekey)
                        .map(DataResult::success)
                        .orElseGet(() -> DataResult.error(() -> "Failed to get element " + resourcekey))
                        .<Pair<Holder<E>, T>>map(p_255658_ -> Pair.of(p_255658_, pair.getSecond()))
                        .setLifecycle(Lifecycle.stable());
                }
            }
        } else {
            return this.elementCodec.decode(pOps, pInput).map(p_214212_ -> p_214212_.mapFirst(Holder::direct));
        }
    }

    @Override
    public String toString() {
        return "RegistryFileCodec[" + this.registryKey + " " + this.elementCodec + "]";
    }
}