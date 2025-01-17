package net.minecraft.stats;

import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public class StatType<T> implements Iterable<Stat<T>> {
    private final Registry<T> registry;
    private final Map<T, Stat<T>> map = new IdentityHashMap<>();
    private final Component displayName;
    private final StreamCodec<RegistryFriendlyByteBuf, Stat<T>> streamCodec;

    public StatType(Registry<T> pRegistry, Component pDisplayName) {
        this.registry = pRegistry;
        this.displayName = pDisplayName;
        this.streamCodec = ByteBufCodecs.registry(pRegistry.key()).map(this::get, Stat::getValue);
    }

    public StreamCodec<RegistryFriendlyByteBuf, Stat<T>> streamCodec() {
        return this.streamCodec;
    }

    public boolean contains(T pValue) {
        return this.map.containsKey(pValue);
    }

    public Stat<T> get(T pValue, StatFormatter pFormatter) {
        return this.map.computeIfAbsent(pValue, p_12896_ -> new Stat<>(this, (T)p_12896_, pFormatter));
    }

    public Registry<T> getRegistry() {
        return this.registry;
    }

    @Override
    public Iterator<Stat<T>> iterator() {
        return this.map.values().iterator();
    }

    public Stat<T> get(T p_12903_) {
        return this.get(p_12903_, StatFormatter.DEFAULT);
    }

    public Component getDisplayName() {
        return this.displayName;
    }
}