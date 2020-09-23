# WuWei - Functional Mutability

Relies on [Lamba](https://github.com/palatable/lambda/).

This library provides the `ST` monad along with `STRefs` to provide delimited scopes for safe mutations in Java.

Unlike the `IO` monad:
1. There is an expectation for a clear start and finish to mutations within the `ST` monad. Once complete, the resulting
   value is frozen and can be used safely as an immutable object.
2. The sole effect within an `ST` monad is the mutation of a provided object. The addition of other sorts of `IO` effects (database work,
   logging statements, etc.) is considered a breach of the contract and may be dropped in optimizations.

## Examples 

### Using normal monadic composition:

    Integer res = STRef.<Integer>stRefCreator().createSTRef(-10)
        .flatMap(ref -> ref.writeSTRef(1))
        .flatMap(ref -> ref.modifySTRef(value -> value * 2))
        .flatMap(STRef::readSTRef)
        .runST();
    assertThat(res, equalTo(2));

### Abstracting out effects:

    STRef.STRefModifier<Integer> set = STRef.writer(0);
    STRef.STRefModifier<Integer> inc = STRef.modifier(value -> value + 1);
    STRef.STRefModifier<Integer> setAndInc = set.and(inc);

    Integer res = STRef.<Integer>stRefCreator().createSTRef(-10)
        .flatMap(setAndInc.modify())
        .flatMap(STRef::readSTRef)
        .runST();
    assertThat(res, equalTo(1));
