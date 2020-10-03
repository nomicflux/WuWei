package io.ataraxic.nomicflux.wuwei;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
}