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

/**
 * {@link MonadRec} which allows for actions within a limited scope, to be chained together with monadic or applicative actions
 * and ended with {@link ST#runST}.
 *
 * Intended for internal use to other classes. {@code ST} on its own does not provide any effect other than passing the
 * state type parameter S through a chain of computations.
 *
 * @param <S>  Type parameter used to unify a chain of {@code ST} actions. This should always be hidden from the end user.
 * @param <A>  Type of the value being passed along
 */
public final class ST<S, A> implements MonadRec<A, ST<S, ?>> {
    private final Fn1<World<S>, Tuple2<World<S>, A>> stFn;

    private ST(Fn1<World<S>, Tuple2<World<S>, A>> stFn) {
        this.stFn = stFn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, B> trampolineM(Fn1<? super A, ? extends MonadRec<RecursiveResult<A, B>, ST<S, ?>>> fn) {
        return new ST<S, B>(stFn.fmap(trampoline(into((w, a) -> fn.apply(a)
                .<ST<S, RecursiveResult<A, B>>>coerce().stFn
                .fmap(into((w2, aOrB) -> aOrB
                        .biMap(a2 -> tuple(w2, a2),
                                b2 -> tuple(w2, b2)))).apply(w)))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, B> flatMap(Fn1<? super A, ? extends Monad<B, ST<S, ?>>> f) {
        return new ST<S, B>(w -> this.stFn.apply(w).into((w2, a) -> f.apply(a).<ST<S, B>>coerce().stFn.apply(w2)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, B> pure(B b) {
        return this.discardL(st(b));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, B> fmap(Fn1<? super A, ? extends B> fn) {
        return new ST<S, B>(w -> stFn.apply(w).fmap(fn));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, B> zip(Applicative<Fn1<? super A, ? extends B>, ST<S, ?>> appFn) {
        return MonadRec.super.zip(appFn).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> Lazy<ST<S, B>> lazyZip(Lazy<? extends Applicative<Fn1<? super A, ? extends B>, ST<S, ?>>> lazyAppFn) {
        return MonadRec.super.lazyZip(lazyAppFn).fmap(MonadRec<B, ST<S, ?>>::coerce);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, B> discardL(Applicative<B, ST<S, ?>> appB) {
        return MonadRec.super.discardL(appB).coerce();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <B> ST<S, A> discardR(Applicative<B, ST<S, ?>> appB) {
        return MonadRec.super.discardR(appB).coerce();
    }

    /**
     * Convert an {@code ST} computation to one in {@link IO}.
     *
     * @return An {@link IO} action which performs the same effect as the original {@code ST} one
     */
    public IO<A> toIO() {
        return io(this::runST);
    }

    /**
     * End the chain of {@code ST} computations and extract a non-monadic value.
     *
     * @return The value passed through the series of {@code ST} actions
     */
    public A runST() {
        return stFn.apply(world())._2();
    }

    /**
     * Create an {@code ST} monad from a value. Same as {@link ST#pure} except static.
     *
     * @param payload  Value entered into the {@code ST} monad
     * @param <S>      Type parameter used to unify {@code ST} chain
     * @param <A>      Type of the value passed into the {@code ST} monad
     * @return         {@code payload} within an {@code ST} monad
     */
    public static <S, A> ST<S, A> st(A payload) {
        return new ST<>(world -> tuple(world, payload));
    }
}
