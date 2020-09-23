package com.nomicflux.wuwei;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

public class STRefTest {

    @Test
    public void readsWhatIsCreated() {
        Integer expected = 0;
        Integer res = STRef.<Integer>stRefCreator().createSTRef(expected).flatMap(STRef::readSTRef).runST();
        assertThat(res, equalTo(expected));
    }

    @Test
    public void readsWhatIsWritten() {
        Integer expected = 0;
        Integer res = STRef.<Integer>stRefCreator().createSTRef(expected - 10)
                .flatMap(s -> s.writeSTRef(expected))
                .flatMap(STRef::readSTRef)
                .runST();
        assertThat(res, equalTo(expected));
    }

    @Test
    public void readsAfterModification() {
        Integer expectedAlmost = 1;
        Integer res = STRef.<Integer>stRefCreator().createSTRef(expectedAlmost)
                .flatMap(s -> s.modifySTRef(x -> x * 2))
                .flatMap(STRef::readSTRef)
                .runST();
        assertThat(res, equalTo(expectedAlmost * 2));
    }

    @Test
    public void modifiesAfterWrite() {
        Integer expectedAlmost = 1;
        Integer res = STRef.<Integer>stRefCreator().createSTRef(expectedAlmost - 10)
                .flatMap(s -> s.writeSTRef(expectedAlmost))
                .flatMap(s -> s.modifySTRef(x -> x * 2))
                .flatMap(STRef::readSTRef)
                .runST();
        assertThat(res, equalTo(expectedAlmost * 2));
    }

    @Test
    public void modifierPipelineCompiles() {
        Integer expectedAlmost = 0;

        STRef.STRefModifier<Integer> set = STRef.writer(expectedAlmost);
        STRef.STRefModifier<Integer> inc = STRef.modifier(x -> x + 1);
        STRef.STRefModifier<Integer> setAndInc = set.and(inc);

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

        STRef.STRefModifier<Integer> inc = STRef.modifier(x -> {
            cheatingEffect.set(true);
            return x + 1;
        });
        STRef.STRefModifier<Integer> set = STRef.writer(expected);
        STRef.STRefModifier<Integer> incAndSet = inc.and(set);

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

        STRef.STRefModifier<AtomicInteger> inc = STRef.modifier(x -> {
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