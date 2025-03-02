package net.minecraft.world.entity.ai.behavior.declarative;

import com.mojang.datafixers.kinds.App;
import com.mojang.datafixers.kinds.Applicative;
import com.mojang.datafixers.kinds.Const;
import com.mojang.datafixers.kinds.IdF;
import com.mojang.datafixers.kinds.K1;
import com.mojang.datafixers.kinds.OptionalBox;
import com.mojang.datafixers.util.Function3;
import com.mojang.datafixers.util.Function4;
import com.mojang.datafixers.util.Unit;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;

public class BehaviorBuilder<E extends LivingEntity, M> implements App<BehaviorBuilder.Mu<E>, M> {
    private final BehaviorBuilder.TriggerWithResult<E, M> trigger;

    public static <E extends LivingEntity, M> BehaviorBuilder<E, M> unbox(App<BehaviorBuilder.Mu<E>, M> pApp) {
        return (BehaviorBuilder<E, M>)pApp;
    }

    public static <E extends LivingEntity> BehaviorBuilder.Instance<E> instance() {
        return new BehaviorBuilder.Instance<>();
    }

    public static <E extends LivingEntity> OneShot<E> create(
        Function<BehaviorBuilder.Instance<E>, ? extends App<BehaviorBuilder.Mu<E>, Trigger<E>>> pInitializer
    ) {
        final BehaviorBuilder.TriggerWithResult<E, Trigger<E>> triggerwithresult = get(
            (App<BehaviorBuilder.Mu<E>, Trigger<E>>)pInitializer.apply(instance())
        );
        return new OneShot<E>() {
            @Override
            public boolean trigger(ServerLevel p_259385_, E p_260003_, long p_259194_) {
                Trigger<E> trigger = triggerwithresult.tryTrigger(p_259385_, p_260003_, p_259194_);
                return trigger == null ? false : trigger.trigger(p_259385_, p_260003_, p_259194_);
            }

            @Override
            public String debugString() {
                return "OneShot[" + triggerwithresult.debugString() + "]";
            }

            @Override
            public String toString() {
                return this.debugString();
            }
        };
    }

    public static <E extends LivingEntity> OneShot<E> sequence(Trigger<? super E> pPredicateTrigger, Trigger<? super E> pTrigger) {
        return create(p_259495_ -> p_259495_.group(p_259495_.ifTriggered(pPredicateTrigger)).apply(p_259495_, p_260322_ -> pTrigger::trigger));
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> pPredicate, OneShot<? super E> pTrigger) {
        return sequence(triggerIf(pPredicate), pTrigger);
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(Predicate<E> pPredicate) {
        return create(p_260353_ -> p_260353_.point((p_259280_, p_259428_, p_259845_) -> pPredicate.test(p_259428_)));
    }

    public static <E extends LivingEntity> OneShot<E> triggerIf(BiPredicate<ServerLevel, E> pPredicate) {
        return create(p_260191_ -> p_260191_.point((p_259079_, p_259093_, p_260140_) -> pPredicate.test(p_259079_, p_259093_)));
    }

    static <E extends LivingEntity, M> BehaviorBuilder.TriggerWithResult<E, M> get(App<BehaviorBuilder.Mu<E>, M> pApp) {
        return unbox(pApp).trigger;
    }

    BehaviorBuilder(BehaviorBuilder.TriggerWithResult<E, M> pTrigger) {
        this.trigger = pTrigger;
    }

    static <E extends LivingEntity, M> BehaviorBuilder<E, M> create(BehaviorBuilder.TriggerWithResult<E, M> pTrigger) {
        return new BehaviorBuilder<>(pTrigger);
    }

    static final class Constant<E extends LivingEntity, A> extends BehaviorBuilder<E, A> {
        Constant(A pValue) {
            this(pValue, () -> "C[" + pValue + "]");
        }

        Constant(final A pValue, final Supplier<String> pName) {
            super(new BehaviorBuilder.TriggerWithResult<E, A>() {
                @Override
                public A tryTrigger(ServerLevel p_259561_, E p_259467_, long p_259297_) {
                    return pValue;
                }

                @Override
                public String debugString() {
                    return pName.get();
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }
    }

    public static final class Instance<E extends LivingEntity> implements Applicative<BehaviorBuilder.Mu<E>, BehaviorBuilder.Instance.Mu<E>> {
        public <Value> Optional<Value> tryGet(MemoryAccessor<OptionalBox.Mu, Value> pMemory) {
            return OptionalBox.unbox(pMemory.value());
        }

        public <Value> Value get(MemoryAccessor<IdF.Mu, Value> pMemory) {
            return IdF.get(pMemory.value());
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<OptionalBox.Mu, Value>> registered(MemoryModuleType<Value> pMemoryType) {
            return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Registered<>(pMemoryType));
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<IdF.Mu, Value>> present(MemoryModuleType<Value> pMemoryType) {
            return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Present<>(pMemoryType));
        }

        public <Value> BehaviorBuilder<E, MemoryAccessor<Const.Mu<Unit>, Value>> absent(MemoryModuleType<Value> pMemoryType) {
            return new BehaviorBuilder.PureMemory<>(new MemoryCondition.Absent<>(pMemoryType));
        }

        public BehaviorBuilder<E, Unit> ifTriggered(Trigger<? super E> pTrigger) {
            return new BehaviorBuilder.TriggerWrapper<>(pTrigger);
        }

        public <A> BehaviorBuilder<E, A> point(A pValue) {
            return new BehaviorBuilder.Constant<>(pValue);
        }

        public <A> BehaviorBuilder<E, A> point(Supplier<String> pName, A pValue) {
            return new BehaviorBuilder.Constant<>(pValue, pName);
        }

        @Override
        public <A, R> Function<App<BehaviorBuilder.Mu<E>, A>, App<BehaviorBuilder.Mu<E>, R>> lift1(App<BehaviorBuilder.Mu<E>, Function<A, R>> pBehavior) {
            return p_259751_ -> {
                final BehaviorBuilder.TriggerWithResult<E, A> triggerwithresult = BehaviorBuilder.get(p_259751_);
                final BehaviorBuilder.TriggerWithResult<E, Function<A, R>> triggerwithresult1 = BehaviorBuilder.get(pBehavior);
                return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                    @Override
                    public R tryTrigger(ServerLevel p_259603_, E p_260233_, long p_259654_) {
                        A a = (A)triggerwithresult.tryTrigger(p_259603_, p_260233_, p_259654_);
                        if (a == null) {
                            return null;
                        } else {
                            Function<A, R> function = (Function<A, R>)triggerwithresult1.tryTrigger(p_259603_, p_260233_, p_259654_);
                            return (R)(function == null ? null : function.apply(a));
                        }
                    }

                    @Override
                    public String debugString() {
                        return triggerwithresult1.debugString() + " * " + triggerwithresult.debugString();
                    }

                    @Override
                    public String toString() {
                        return this.debugString();
                    }
                });
            };
        }

        public <T, R> BehaviorBuilder<E, R> map(final Function<? super T, ? extends R> pMapper, App<BehaviorBuilder.Mu<E>, T> pBehavior) {
            final BehaviorBuilder.TriggerWithResult<E, T> triggerwithresult = BehaviorBuilder.get(pBehavior);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                @Override
                public R tryTrigger(ServerLevel p_259755_, E p_259656_, long p_259300_) {
                    T t = triggerwithresult.tryTrigger(p_259755_, p_259656_, p_259300_);
                    return (R)(t == null ? null : pMapper.apply(t));
                }

                @Override
                public String debugString() {
                    return triggerwithresult.debugString() + ".map[" + pMapper + "]";
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <A, B, R> BehaviorBuilder<E, R> ap2(
            App<BehaviorBuilder.Mu<E>, BiFunction<A, B, R>> pMapper, App<BehaviorBuilder.Mu<E>, A> pBehavior1, App<BehaviorBuilder.Mu<E>, B> pBehavior2
        ) {
            final BehaviorBuilder.TriggerWithResult<E, A> triggerwithresult = BehaviorBuilder.get(pBehavior1);
            final BehaviorBuilder.TriggerWithResult<E, B> triggerwithresult1 = BehaviorBuilder.get(pBehavior2);
            final BehaviorBuilder.TriggerWithResult<E, BiFunction<A, B, R>> triggerwithresult2 = BehaviorBuilder.get(pMapper);
            return BehaviorBuilder.create(new BehaviorBuilder.TriggerWithResult<E, R>() {
                @Override
                public R tryTrigger(ServerLevel p_259274_, E p_259817_, long p_259820_) {
                    A a = triggerwithresult.tryTrigger(p_259274_, p_259817_, p_259820_);
                    if (a == null) {
                        return null;
                    } else {
                        B b = triggerwithresult1.tryTrigger(p_259274_, p_259817_, p_259820_);
                        if (b == null) {
                            return null;
                        } else {
                            BiFunction<A, B, R> bifunction = triggerwithresult2.tryTrigger(p_259274_, p_259817_, p_259820_);
                            return bifunction == null ? null : bifunction.apply(a, b);
                        }
                    }
                }

                @Override
                public String debugString() {
                    return triggerwithresult2.debugString() + " * " + triggerwithresult.debugString() + " * " + triggerwithresult1.debugString();
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }

        public <T1, T2, T3, R> BehaviorBuilder<E, R> ap3(
            App<BehaviorBuilder.Mu<E>, Function3<T1, T2, T3, R>> pMapper,
            App<BehaviorBuilder.Mu<E>, T1> pBehavior1,
            App<BehaviorBuilder.Mu<E>, T2> pBehavior2,
            App<BehaviorBuilder.Mu<E>, T3> pBehavior3
        ) {
            final BehaviorBuilder.TriggerWithResult<E, T1> triggerwithresult = BehaviorBuilder.get(pBehavior1);
            final BehaviorBuilder.TriggerWithResult<E, T2> triggerwithresult1 = BehaviorBuilder.get(pBehavior2);
            final BehaviorBuilder.TriggerWithResult<E, T3> triggerwithresult2 = BehaviorBuilder.get(pBehavior3);
            final BehaviorBuilder.TriggerWithResult<E, Function3<T1, T2, T3, R>> triggerwithresult3 = BehaviorBuilder.get(pMapper);
            return BehaviorBuilder.create(
                new BehaviorBuilder.TriggerWithResult<E, R>() {
                    @Override
                    public R tryTrigger(ServerLevel p_259096_, E p_260221_, long p_259035_) {
                        T1 t1 = triggerwithresult.tryTrigger(p_259096_, p_260221_, p_259035_);
                        if (t1 == null) {
                            return null;
                        } else {
                            T2 t2 = triggerwithresult1.tryTrigger(p_259096_, p_260221_, p_259035_);
                            if (t2 == null) {
                                return null;
                            } else {
                                T3 t3 = triggerwithresult2.tryTrigger(p_259096_, p_260221_, p_259035_);
                                if (t3 == null) {
                                    return null;
                                } else {
                                    Function3<T1, T2, T3, R> function3 = triggerwithresult3.tryTrigger(p_259096_, p_260221_, p_259035_);
                                    return function3 == null ? null : function3.apply(t1, t2, t3);
                                }
                            }
                        }
                    }

                    @Override
                    public String debugString() {
                        return triggerwithresult3.debugString()
                            + " * "
                            + triggerwithresult.debugString()
                            + " * "
                            + triggerwithresult1.debugString()
                            + " * "
                            + triggerwithresult2.debugString();
                    }

                    @Override
                    public String toString() {
                        return this.debugString();
                    }
                }
            );
        }

        public <T1, T2, T3, T4, R> BehaviorBuilder<E, R> ap4(
            App<BehaviorBuilder.Mu<E>, Function4<T1, T2, T3, T4, R>> pMapper,
            App<BehaviorBuilder.Mu<E>, T1> pBehavior1,
            App<BehaviorBuilder.Mu<E>, T2> pBehavior2,
            App<BehaviorBuilder.Mu<E>, T3> pBehavior3,
            App<BehaviorBuilder.Mu<E>, T4> pBehavior4
        ) {
            final BehaviorBuilder.TriggerWithResult<E, T1> triggerwithresult = BehaviorBuilder.get(pBehavior1);
            final BehaviorBuilder.TriggerWithResult<E, T2> triggerwithresult1 = BehaviorBuilder.get(pBehavior2);
            final BehaviorBuilder.TriggerWithResult<E, T3> triggerwithresult2 = BehaviorBuilder.get(pBehavior3);
            final BehaviorBuilder.TriggerWithResult<E, T4> triggerwithresult3 = BehaviorBuilder.get(pBehavior4);
            final BehaviorBuilder.TriggerWithResult<E, Function4<T1, T2, T3, T4, R>> triggerwithresult4 = BehaviorBuilder.get(pMapper);
            return BehaviorBuilder.create(
                new BehaviorBuilder.TriggerWithResult<E, R>() {
                    @Override
                    public R tryTrigger(ServerLevel p_259537_, E p_259581_, long p_259423_) {
                        T1 t1 = triggerwithresult.tryTrigger(p_259537_, p_259581_, p_259423_);
                        if (t1 == null) {
                            return null;
                        } else {
                            T2 t2 = triggerwithresult1.tryTrigger(p_259537_, p_259581_, p_259423_);
                            if (t2 == null) {
                                return null;
                            } else {
                                T3 t3 = triggerwithresult2.tryTrigger(p_259537_, p_259581_, p_259423_);
                                if (t3 == null) {
                                    return null;
                                } else {
                                    T4 t4 = triggerwithresult3.tryTrigger(p_259537_, p_259581_, p_259423_);
                                    if (t4 == null) {
                                        return null;
                                    } else {
                                        Function4<T1, T2, T3, T4, R> function4 = triggerwithresult4.tryTrigger(p_259537_, p_259581_, p_259423_);
                                        return function4 == null ? null : function4.apply(t1, t2, t3, t4);
                                    }
                                }
                            }
                        }
                    }

                    @Override
                    public String debugString() {
                        return triggerwithresult4.debugString()
                            + " * "
                            + triggerwithresult.debugString()
                            + " * "
                            + triggerwithresult1.debugString()
                            + " * "
                            + triggerwithresult2.debugString()
                            + " * "
                            + triggerwithresult3.debugString();
                    }

                    @Override
                    public String toString() {
                        return this.debugString();
                    }
                }
            );
        }

        static final class Mu<E extends LivingEntity> implements Applicative.Mu {
            private Mu() {
            }
        }
    }

    public static final class Mu<E extends LivingEntity> implements K1 {
    }

    static final class PureMemory<E extends LivingEntity, F extends K1, Value> extends BehaviorBuilder<E, MemoryAccessor<F, Value>> {
        PureMemory(final MemoryCondition<F, Value> pMemoryCondition) {
            super(new BehaviorBuilder.TriggerWithResult<E, MemoryAccessor<F, Value>>() {
                public MemoryAccessor<F, Value> tryTrigger(ServerLevel p_259899_, E p_259558_, long p_259793_) {
                    Brain<?> brain = p_259558_.getBrain();
                    Optional<Value> optional = brain.getMemoryInternal(pMemoryCondition.memory());
                    return optional == null ? null : pMemoryCondition.createAccessor(brain, optional);
                }

                @Override
                public String debugString() {
                    return "M[" + pMemoryCondition + "]";
                }

                @Override
                public String toString() {
                    return this.debugString();
                }
            });
        }
    }

    interface TriggerWithResult<E extends LivingEntity, R> {
        @Nullable
        R tryTrigger(ServerLevel pLevel, E pEntity, long pGameTime);

        String debugString();
    }

    static final class TriggerWrapper<E extends LivingEntity> extends BehaviorBuilder<E, Unit> {
        TriggerWrapper(final Trigger<? super E> pTrigger) {
            super(new BehaviorBuilder.TriggerWithResult<E, Unit>() {
                @Nullable
                public Unit tryTrigger(ServerLevel p_259397_, E p_260169_, long p_259155_) {
                    return pTrigger.trigger(p_259397_, p_260169_, p_259155_) ? Unit.INSTANCE : null;
                }

                @Override
                public String debugString() {
                    return "T[" + pTrigger + "]";
                }
            });
        }
    }
}