package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functor.Functor;

import static com.jnape.palatable.lambda.io.IO.io;
import static com.nomicflux.wuwei.ST.st;

public final class STRef<S, A> implements Functor<A, STRef<S, ?>> {
    private A payload;

    private STRef(A a) {
        this.payload = a;
    }

    @Override
    public <B> STRef<S, B> fmap(Fn1<? super A, ? extends B> fn) {
        return new STRef<>(fn.apply(payload));
    }

    private static class STSTRef<S, A> {
        private final static STSTRef<?, ?> INSTANCE = new STSTRef<>();

        private ST<S, STRef<S, A>> createSTRef(A payload) {
            return st(new STRef<S, A>(payload));
        }
    }

    private static class STRefProducer {
        private final static STRefProducer INSTANCE = new STRefProducer();

        @SuppressWarnings("unchecked")
        private <A> STSTRef<?, A> prepare() {
            return (STSTRef<?, A>) STSTRef.INSTANCE;
        }
    }

    public static STRefProducer stRefProducer() {
        return STRefProducer.INSTANCE;
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
        public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> writer(A a) {
            return s -> s.writeSTRef(a);
        }
    }

    public ST<S, STRef<S, A>> modifySTRef(Fn1<A, A> fn) {
        return readSTRef().flatMap(x -> writeSTRef(fn.apply(x)));
    }

    public static <A> STRefWriter<A> writer() {
        return new STRefWriter<>();
    }

    public static class STRefModifier<A> {
        public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> modifier(Fn1<A, A> fn) {
            return s -> s.modifySTRef(fn);
        }
    }

    public static <A> STRefModifier<A> modifier() {
        return new STRefModifier<>();
    }

    public static void main(String[] args) {
        Integer i = stRefProducer().<Integer>prepare().createSTRef(0)
                .flatMap(STRef.<Integer>modifier().modifier(x -> x + 1))
                .flatMap(STRef.<Integer>writer().writer(10))
                .flatMap(STRef.<Integer>modifier().modifier(x -> x * 2))
                .flatMap(STRef::readSTRef)
                .runST();

        System.out.println(i);
    }
}
