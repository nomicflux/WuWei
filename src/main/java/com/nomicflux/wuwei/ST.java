package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.MonadRec;

import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Id.id;
import static com.nomicflux.wuwei.World.world;

public final class ST<S, A> implements MonadRec<A, ST<S, ?>> {
    private final Fn1<World<S>, Tuple2<World<S>, A>> stFn;

    private ST(Fn1<World<S>, Tuple2<World<S>, A>> stFn) {
        this.stFn = stFn;
    }

    // @TODO: finish implementation
    @Override
    public <B> ST<S, B> trampolineM(Fn1<? super A, ? extends MonadRec<RecursiveResult<A, B>, ST<S, ?>>> fn) {
        return new ST<S, B>(w -> {
            Tuple2<World<S>, A> apply = stFn.apply(w);
            Tuple2<World<S>, B> o = apply.into((w2, a) -> {
                ST<S, RecursiveResult<A, B>> apply1 = fn.apply(a).coerce();
                Tuple2<World<S>, RecursiveResult<A, B>> apply2 = apply1.stFn.apply(w2);
                Tuple2<World<S>, B> o1 = apply2.biMapR(rr -> rr.match(r -> {
                            B o2 = null;
                            return o2;
                        },
                        id()));
                return o1;
            });
            return o;
        });
    }

    @Override
    public <B> ST<S, B> flatMap(Fn1<? super A, ? extends Monad<B, ST<S, ?>>> f) {
        return new ST<S, B>(w -> stFn.apply(w).into((w2, a) -> f.apply(a).<ST<S, B>>coerce().stFn.apply(w2)));
    }

    @Override
    public <B> ST<S, B> pure(B b) {
        return this.discardL(st(b));
    }

    @Override
    public <B> ST<S, B> fmap(Fn1<? super A, ? extends B> fn) {
        return MonadRec.super.<B>fmap(fn).coerce();
    }

    @Override
    public <B> ST<S, B> zip(Applicative<Fn1<? super A, ? extends B>, ST<S, ?>> appFn) {
        return MonadRec.super.zip(appFn).coerce();
    }

    @Override
    public <B> Lazy<ST<S, B>> lazyZip(Lazy<? extends Applicative<Fn1<? super A, ? extends B>, ST<S, ?>>> lazyAppFn) {
        return MonadRec.super.lazyZip(lazyAppFn).fmap(MonadRec<B, ST<S, ?>>::coerce);
    }

    @Override
    public <B> ST<S, B> discardL(Applicative<B, ST<S, ?>> appB) {
        return MonadRec.super.discardL(appB).coerce();
    }

    @Override
    public <B> ST<S, A> discardR(Applicative<B, ST<S, ?>> appB) {
        return MonadRec.super.discardR(appB).coerce();
    }

    public A runST() {
        return stFn.apply(world())._2();
    }

    public static <S, A> ST<S, A> st(A payload) {
            return new ST<>(world -> tuple(world, payload));
    }
}
