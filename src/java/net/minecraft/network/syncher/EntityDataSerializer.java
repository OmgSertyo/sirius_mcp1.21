package net.minecraft.network.syncher;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface EntityDataSerializer<T> {
    StreamCodec<? super RegistryFriendlyByteBuf, T> codec();

    default EntityDataAccessor<T> createAccessor(int pId) {
        return new EntityDataAccessor<>(pId, this);
    }

    T copy(T pValue);

    static <T> EntityDataSerializer<T> forValueType(StreamCodec<? super RegistryFriendlyByteBuf, T> p_332495_) {
        return (ForValueType<T>)() -> p_332495_;
    }

    public interface ForValueType<T> extends EntityDataSerializer<T> {
        @Override
        default T copy(T p_238112_) {
            return p_238112_;
        }
    }
}