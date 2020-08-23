package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.recursion.RecursiveResult;
import com.jnape.palatable.lambda.functor.Applicative;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.io.IO;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monad.MonadRec;

import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.functions.builtin.fn2.Into.into;
import static com.jnape.palatable.lambda.functions.recursion.Trampoline.trampoline;
import static com.jnape.palatable.lambda.io.IO.io;
import static com.nomicflux.wuwei.World.world;

public final class ST<S, A> implements MonadRec<A, ST<S, ?>> {
    private final Fn1<World<S>, Tuple2<World<S>, A>> stFn;

    private ST(Fn1<World<S>, Tuple2<World<S>, A>> stFn) {
        this.stFn = stFn;
    }

    @Override
    public <B> ST<S, B> trampolineM(Fn1<? super A, ? extends MonadRec<RecursiveResult<A, B>, ST<S, ?>>> fn) {
        return new ST<S, B>(stFn.fmap(trampoline(into((w, a) -> fn.apply(a)
                .<ST<S, RecursiveResult<A, B>>>coerce().stFn
                .fmap(into((w2, aOrB) -> aOrB
                        .biMap(a2 -> tuple(w2, a2),
                                b2 -> tuple(w2, b2)))).apply(w)))));
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

    public IO<A> toIO() {
        return io(this::runST);
    }

    public A runST() {
        return stFn.apply(world())._2();
    }

    public static <S, A> ST<S, A> st(A payload) {
        return new ST<>(world -> tuple(world, payload));
    }
}
