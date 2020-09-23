package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.adt.choice.Choice2;
import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.adt.choice.Choice2.a;
import static com.jnape.palatable.lambda.adt.choice.Choice2.b;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
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

    public static class STRefModifier<A> {
        private final Choice2<Fn1<A, A>, A> modification;

        private STRefModifier(Choice2<Fn1<A, A>, A> modification) {
            this.modification = modification;
        }

        public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> modify() {
            return modification.match(
                    fn -> s -> s.modifySTRef(fn),
                    a -> s -> s.writeSTRef(a)
            );
        }

        public STRefModifier<A> and(STRefModifier<A> next) {
            return next.modification
                    .match(
                            fn2 -> modification.match(
                                    fn1 -> new STRefModifier<A>(a(fn1.fmap(fn2))),
                                    constantly(new STRefModifier<A>(a(fn2::apply)))
                            ),
                            b -> new STRefModifier<A>(b(b))
                    );
        }
    }

    public static <A> STRefModifier<A> modifier(Fn1<A, A> fn) {
        return new STRefModifier<>(a(fn));
    }

    public static <A> STRefModifier<A> writer(A a) {
        return new STRefModifier<>(b(a));
    }
}
