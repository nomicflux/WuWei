package com.nomicflux.wuwei;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

public class STRefModifierTest {
    @Test
    public void modifierPipelineCompiles() {
        Integer expectedAlmost = 0;

        STRefModifier<Integer> set = STRefModifier.writer(expectedAlmost);
        STRefModifier<Integer> inc = STRefModifier.modifier(x -> x + 1);
        STRefModifier<Integer> setAndInc = set.and(inc);

        Integer res = STRef.<Integer>stRefCreator().createSTRef(expectedAlmost - 10)
                .flatMap(setAndInc.run())
                .flatMap(STRef::readSTRef)
                .runST();
        assertThat(res, equalTo(expectedAlmost + 1));
    }

    @Test
    public void writerModifierDropsPreviousChanges() {
        Integer expected = 0;

        AtomicBoolean cheatingEffect = new AtomicBoolean(false);

        STRefModifier<Integer> inc = STRefModifier.modifier(x -> {
            cheatingEffect.set(true);
            return x + 1;
        });
        STRefModifier<Integer> set = STRefModifier.writer(expected);
        STRefModifier<Integer> incAndSet = inc.and(set);

        Integer res = STRef.<Integer>stRefCreator().createSTRef(expected - 10)
                .flatMap(incAndSet.run())
                .flatMap(STRef::readSTRef)
                .runST();
        assertThat(res, equalTo(expected));
        assertFalse(cheatingEffect.get());
    }

    @Test
    public void modifiesInPlace() {
        int original = 0;
        AtomicInteger base = new AtomicInteger(original);

        STRefModifier<AtomicInteger> inc = STRefModifier.modifier(x -> {
            x.incrementAndGet();
            return x;
        });

        AtomicInteger res = STRef.<AtomicInteger>stRefCreator().createSTRef(base)
                .flatMap(inc.run())
                .flatMap(STRef::readSTRef)
                .runST();

        assertSame(res, base);
        assertThat(res.get(), equalTo(original + 1));
    }
}