# WuWei - Functional Mutability

Relies on [Lambda](https://github.com/palatable/lambda/).

This library includes the `ST` monad along with `STRef`s to provide delimited scopes for safe mutations in Java.

Unlike the `IO` monad:
1. There is an expectation for a clear start and finish to mutations within the `ST` monad. Once complete, the resulting
   value is frozen and can be used safely as an immutable object.
2. The sole effect within an `ST` monad is the mutation of a provided object. The addition of other sorts of `IO` effects (database work,
   logging statements, etc.) is considered a breach of contract and may be dropped in optimizations.

## Examples 

### Using normal monadic composition:

    Integer res = STRef.<Integer>stRefCreator().createSTRef(-10)
        .flatMap(ref -> ref.writeSTRef(1))
        .flatMap(ref -> ref.modifySTRef(value -> value * 2))
        .flatMap(ref -> ref.readSTRef)
        .runST();
    assertThat(res, equalTo(2));

### Abstracting out effects:

    STRefModifier<Integer> set = writer(1);
    STRefModifier<Integer> inc = modifier(value -> value * 2);
    STRefModifier<Integer> setAndInc = set.and(inc);

    Integer res = STRef.<Integer>stRefCreator().createSTRef(-10)
        .flatMap(setAndInc.run())
        .flatMap(STRef::readSTRef)
        .runST();
    assertThat(res, equalTo(2));

## Performance

For lightweight types, regular lambda functions will work fine, if not better:

    Integer integer = STRef.<Integer>stRefCreator()
                           .createSTRef(0)
                           .flatMap(s -> s.modifySTRef(trampoline(a -> a > 1_000_000 ? terminate(a) : recurse(a + 1))))
                           .flatMap(STRef::readSTRef)
                           .runST();

runs at about the same speed as the simpler:

    Integer integer = foldLeft((acc, n) -> acc + n, 0, replicate(1_000_000, 1));

However, for heavier-weight objects, the `STRef` implementation is significantly faster. For example, using the
following class:

   public static class Foo {
        private final Iterable<Integer> m;
        private int n;

        public Foo(Iterable<Integer> m, int n) {
            this.m = m;
            this.n = n;
        }

        public Foo incImmutable() {
            return new Foo(m, n + foldLeft(Integer::sum, 0, m));
        }

        public Foo incMutable() {
            this.n += foldLeft(Integer::sum, 0, m);
            return this;
        }
    }

the following `STRef` implementation with a mutable object runs locally at about 180-200ms after JVM optimizations:

    Foo foo = STRef.<Foo>stRefCreator()
                   .createSTRef(new Foo(take(10, iterate(n -> n + 1, 1)), 0))
                   .flatMap(s -> s.modifySTRef(trampoline(f -> f.n > 10_000_000 ? terminate(f) : recurse(f.incMutable()))))
                   .flatMap(STRef::readSTRef)
                   .runST();

while the `foldLeft` implementation with immutable objects runs around 13000 - 15000ms:

    Foo foo = foldLeft((acc, n) -> acc.incImmutable(), 
                       new Foo(take(10, iterate(n -> n + 1, 1)), 0),
                       replicate(10_000_000, 1));

