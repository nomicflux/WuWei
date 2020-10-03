package io.ataraxic.nomicflux.wuwei;

import com.jnape.palatable.traitor.annotations.TestTraits;
import com.jnape.palatable.traitor.runners.Traits;
import org.junit.runner.RunWith;
import testsupport.traits.*;

import static io.ataraxic.nomicflux.wuwei.ST.st;
import static testsupport.traits.Equivalence.equivalence;

@RunWith(Traits.class)
public class STTest {

    @TestTraits({FunctorLaws.class,
            ApplicativeLaws.class,
            MonadLaws.class,
            MonadRecLaws.class})
    public Equivalence<ST<?, Integer>> testReader() {
        return equivalence(st(0), ST::runST);
    }
}