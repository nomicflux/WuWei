package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.adt.choice.Choice2;
import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.adt.choice.Choice2.a;
import static com.jnape.palatable.lambda.adt.choice.Choice2.b;

public class STRefModifier<A> {
    private final Choice2<Fn1<A, A>, A> modification;

    STRefModifier(Choice2<Fn1<A, A>, A> modification) {
        this.modification = modification;
    }

    public static <A> STRefModifier<A> modifier(Fn1<A, A> fn) {
        return new STRefModifier<>(a(fn));
    }

    public static <A> STRefModifier<A> writer(A a) {
        return new STRefModifier<>(b(a));
    }

    public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> run() {
        return modification.match(
                fn -> s -> s.modifySTRef(fn),
                a -> s -> s.writeSTRef(a)
        );
    }

    public STRefModifier<A> and(STRefModifier<A> next) {
        return next
                .modification
                .match(
                        fn2 -> modification.match(
                                fn1 -> new STRefModifier<A>(a(fn1.fmap(fn2))),
                                b -> new STRefModifier<>(b(fn2.apply(b)))
                        ),
                        b -> new STRefModifier<A>(b(b))
                );
    }
}
