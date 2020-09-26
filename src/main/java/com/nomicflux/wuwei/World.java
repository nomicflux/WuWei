package com.nomicflux.wuwei;

/**
 * A phantom type. It is used as a concrete value for the type S.
 *
 * @param <S> The type used for type unification, but without any particular value
 */
public final class World<S> {
    private final static World<?> INSTANCE = new World<>();

    private World() {
    }

    /**
     * Create an instance of a {@code World} for a given type {@code S}.
     *
     * @param <S>  Type to be represented by {@code World} in this case
     * @return     {@code World} as typed to S
     */
    @SuppressWarnings("unchecked")
    public static <S> World<S> world() {
        return (World<S>) INSTANCE;
    }
}
