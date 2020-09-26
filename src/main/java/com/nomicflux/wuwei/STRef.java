package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.io.IO.io;
import static com.nomicflux.wuwei.ST.st;

/**
 * A class to work with mutable references within the {@link ST} monad
 *
 * @param <S> The hidden state parameter used to unify a sequence of ST events and prevent them from leaking references
 * @param <A> The type of the value being mutated
 */
public final class STRef<S, A> {
    private A payload;

    private STRef(A a) {
        this.payload = a;
    }

    /**
     * Used to create an STRef within an {@link ST} monad.
     * Intended for internal use - use {@link STRef#stRefCreator} instead to construct one.
     *
     * @param <S> Type parameter provided to unify the S of {@code STRef<S, A>} with the S of {@code ST<S>}
     * @param <A> Type of the value to be held in the {@code STRef}
     */
    public static class STRefCreator<S, A> {
        private final static STRefCreator<?, ?> INSTANCE = new STRefCreator<>();

        /**
         * Create an {@code STRef}.
         * This value will be mutated, so caveat emptor if an already mutable object is passed into a {@code STRef}.
         *
         * @param payload Starting value for the {@code STRef}
         * @return {@code STRef} with the payload value, within the {@link ST} monadic context
         */
        public ST<S, STRef<S, A>> createSTRef(A payload) {
            return st(new STRef<S, A>(payload));
        }
    }

    /**
     * Used to create {@code STRefs} of the given type A.
     * Hides the internal state type parameter S.
     *
     * @param <A> Type of the value to be held in the {@code STRef}
     * @return An {@link STRefCreator} which will do the work of unifying the state type parameters
     */
    @SuppressWarnings("unchecked")
    public static <A> STRefCreator<?, A> stRefCreator() {
        return (STRefCreator<?, A>) STRefCreator.INSTANCE;
    }

    /**
     * Read a value from an {@code STRef}.
     * Necessary to freeze the computation at a point in time in order to {@link ST#runST} and retrieve the value.
     *
     * @return The value of the {@code STRef} within the {@link ST} monad
     */
    public ST<S, A> readSTRef() {
        return st(payload);
    }

    /**
     * Write a value to a {@code STRef}.
     *
     * @param a New value to be held in the reference
     * @return This {@code STRef} after the new value has been written
     */
    public ST<S, STRef<S, A>> writeSTRef(A a) {
        return st(io(() -> {
            payload = a;
            return this;
        }).unsafePerformIO());
    }

    /**
     * Modify a value within a {@code STRef}.
     *
     * @param fn Function to apply to the value in the reference.
     * @return This {@code STRef} after the old value has been modified in place.
     */
    public ST<S, STRef<S, A>> modifySTRef(Fn1<A, A> fn) {
        return readSTRef().flatMap(x -> writeSTRef(fn.apply(x)));
    }
}
