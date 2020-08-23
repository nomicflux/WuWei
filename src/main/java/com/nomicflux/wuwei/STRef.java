package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.io.IO;

import static com.jnape.palatable.lambda.io.IO.io;
import static com.nomicflux.wuwei.ST.st;

public final class STRef<S, A> {
    private A payload;

    private STRef(A a) {
        this.payload = a;
    }

    private static class STRefCreator<S, A> {
        private final static STRefCreator<?, ?> INSTANCE = new STRefCreator<>();

        private ST<S, STRef<S, A>> createSTRef(A payload) {
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

    public static class STRefWriter<A> {
        public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> write(A a) {
            return s -> s.writeSTRef(a);
        }
    }

    public static <A> STRefWriter<A> writer() {
        return new STRefWriter<>();
    }

    public ST<S, STRef<S, A>> modifySTRef(Fn1<A, A> fn) {
        return readSTRef().flatMap(x -> writeSTRef(fn.apply(x)));
    }

    public static class STRefModifier<A> {
        public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> modify(Fn1<A, A> fn) {
            return s -> s.modifySTRef(fn);
        }
    }

    public static <A> STRefModifier<A> modifier() {
        return new STRefModifier<>();
    }

    public static void main(String[] args) {
        Integer i = STRef.<Integer>stRefCreator().createSTRef(0)
                .flatMap(STRef.<Integer>modifier().modify(x -> x + 1))
                .flatMap(STRef.<Integer>writer().write(10))
                .flatMap(s -> s.modifySTRef(x -> x * 2))
                .flatMap(STRef::readSTRef)
                .runST();

        System.out.println(i);
    }
}
