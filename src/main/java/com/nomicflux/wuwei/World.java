package com.nomicflux.wuwei;

public final class World<S> {
    private final static World<?> INSTANCE = new World<>();

    private World() {
    }

    @SuppressWarnings("unchecked")
    public static <S> World<S> world() {
        return (World<S>) INSTANCE;
    }
}
