package com.nomicflux.wuwei;

import com.jnape.palatable.lambda.adt.choice.Choice2;
import com.jnape.palatable.lambda.functions.Fn1;

import static com.jnape.palatable.lambda.adt.choice.Choice2.a;
import static com.jnape.palatable.lambda.adt.choice.Choice2.b;

/**
 * Utility class for creating composable units of work on {@link STRef}s.
 *
 * @param <A>  Type of the value in the reference which will be mutated
 */
public class STRefModifier<A> {
    private final Choice2<Fn1<A, A>, A> modification;

    private STRefModifier(Choice2<Fn1<A, A>, A> modification) {
        this.modification = modification;
    }

    /**
     * Create a modification step in an {@link STRef} pipeline.
     *
     * @param fn   Function used to modify reference value
     * @param <A>  Type of the value in the reference
     * @return     {@code STRefModifier} which modifies an {@link STRef} with {@code fn}
     */
    public static <A> STRefModifier<A> modifier(Fn1<A, A> fn) {
        return new STRefModifier<>(a(fn));
    }

    /**
     * Create a writing step in an {@link STRef} pipeline.
     *
     * @param a    Value to be written into a reference
     * @param <A>  Type of the value in the reference
     * @return     {@code STRefModifier} which writes an {@code a} ta an {@link STRef}
     */
    public static <A> STRefModifier<A> writer(A a) {
        return new STRefModifier<>(b(a));
    }

    /**
     * Run a series of {@code STRefModifier}s within an {@link ST} context.
     * Calling this method will expose the state type parameter S, so it can only be used in a chain of {@link ST#flatMap}s.
     *
     * @param <S>  Type parameter used to unify {@link ST} steps
     * @return     Function used within a {@link ST#flatMap} to alter a {@link STRef}
     */
    public <S> Fn1<STRef<S, A>, ST<S, STRef<S, A>>> run() {
        return modification.match(
                fn -> s -> s.modifySTRef(fn),
                a -> s -> s.writeSTRef(a)
        );
    }

    /**
     * Compose this {@code STRefModifier} with another.
     * A writer will drop all previous {@code STRefModifiers} as it overwrites all previous work, for lawful uses of a {@link STRef}.
     *
     * @param next  The next {@code STRefModifier} to be used after {@code this} one
     * @return      A {@code STRefModifier} consisting of the chained modifiers
     */
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
