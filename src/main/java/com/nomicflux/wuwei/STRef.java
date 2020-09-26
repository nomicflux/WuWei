package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.io.IO.io;
import static com.nomicflux.wuwei.ST.st;

public final class STRef<S, A> {
    private A payload;

    private STRef(A a) {
        this.payload = a;
    }

    public static class STRefCreator<S, A> {
        private final static STRefCreator<?, ?> INSTANCE = new STRefCreator<>();

        public ST<S, STRef<S, A>> createSTRef(A payload) {
            return st(new STRef<S, A>(payload));
        }
    }

    @SuppressWarnings("unchecked")
    public static <A> STRefCreator<?, A> stRefCreator() {
        return (STRefCreator<?, A>) STRefCreator.INSTANCE;
    }

    public ST<S, A> readSTRef() {
        return st(payload);
    }

    public ST<S, STRef<S, A>> writeSTRef(A a) {
        return st(io(() -> {
            payload = a;
            return this;
        }).unsafePerformIO());
    }

    public ST<S, STRef<S, A>> modifySTRef(Fn1<A, A> fn) {
        return readSTRef().flatMap(x -> writeSTRef(fn.apply(x)));
    }
}
