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

```Java
Integer res = STRef.<Integer>stRefCreator()
                   .createSTRef(-10)
                   .flatMap(ref -> ref.writeSTRef(1))
                   .flatMap(ref -> ref.modifySTRef(value -> value * 2))
                   .flatMap(ref -> ref.readSTRef)
                   .runST();
assertThat(res, equalTo(2));
```

### Abstracting out effects:

```Java
STRefModifier<Integer> set = writer(1);
STRefModifier<Integer> inc = modifier(value -> value * 2);
STRefModifier<Integer> setAndInc = set.and(inc);

Integer res = STRef.<Integer>stRefCreator()
                   .createSTRef(-10)
                   .flatMap(setAndInc.run())
                   .flatMap(STRef::readSTRef)
                   .runST();
assertThat(res, equalTo(2));
```

## STRef Usage

The purpose of an `STRef` is that all of its actions take place in the `ST` monad. This involves passing an internal
type parameter through the chain of operations. If at any point one attempts to break the chain of operations and
release a mutable `STRef` into the wild, type inference will break. One must:
1. Create an STRef
2. Perform the operations on an STRef
3. Read the STRef
4. And, finally, runST to release the read value from the `ST` monad.

### Creating and Running an STRef

In order to create an STRef, use an stRefCreator:

```Java
ST<?, ? extends STRef<?, Integer>> stRef = STRef.<Integer>stRefCreator()
                                                .createSTRef(0);
```

Because of the type captures, this variable stRef is unusable in any further operations - the type inference engine
would need to unify both the capture in `ST` and in `STRef` and verify they are the same, which it can't do. To resolve this, 
read the `STRef`:

```Java
ST<?, Integer> stResult = STRef.<Integer>stRefCreator()
                               .createSTRef(10)
                               .flatMap(STRef::readSTRef);
```

At which point the result no longer would leak an STRef, and can be run:

```Java
Integer ten = integerST.runST();
```

### Performing operations with flatmaps

The two operations that be used to mutate an `STRef` are `STRef#writeSTRef` and `STRef#modifySTRef`:

```Java
Integer writtenAndModified = STRef.<Integer>stRefCreator()
                                  .createSTRef(10)
                                  .flatMap(stRef -> stRef.writeSTRef(0))
                                  .flatMap(stRef -> stRef.modifySTRef(x -> x + 1))
                                  .flatMap(STRef::readSTRef)
                                  .runST();
```

`STRef#writeSTRef` will replace the reference value. `STRef#modifySTRef` will modify the reference value in place using
the provided function.

### Performing operations with STRefModifier

As stated above, a chain of actions on an `STRef` cannot be abstracted out, as it would leak the `STRef` and fail type
checking. In order to create compositional units of work on `STRefs`, one can use an `STRefModifier` instead. To create
a write action, use `STRefModifier#writer`:

```Java
STRefModifier<Integer> writeTen = writer(10);
```

And to modify, `STRefModifier#modifier`:

```Java
STRefModifier<Integer> triple = modifier(x -> x * 3);
```

These can be combined:

```Java
STRefModifier<Integer> writeTenThenTriple = writeTen.and(triple);
```

Since the only two mutable actions allowed for an `STRef` are writing and modification, and writing will overwrite
whatever is currently in the reference whatever it is, `STRefModifier` features an optimization where a `write` will
wipe the slate clean and start from scratch, no matter how many other actions have been `added` before it.

## Performance

For lightweight types, regular lambda functions will work fine, if not better. For example, this `STRef` summation
algorithm:

```Java
Integer integer = STRef.<Integer>stRefCreator()
                       .createSTRef(0)
                       .flatMap(s -> s.modifySTRef(trampoline(a -> a > 1_000_000 ? terminate(a) : recurse(a + 1))))
                       .flatMap(STRef::readSTRef)
                       .runST();
```

runs at about the same speed as the simpler:

```Java
Integer integer = foldLeft((acc, n) -> acc + n, 0, replicate(1_000_000, 1));
```

and takes significantly longer if the trampolining is done in-place in a monadic context (~10x the time).

However, for heavier-weight objects, the `STRef` implementation is significantly faster. For example, using the
following class:

```Java
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
```

the following `STRef` implementation with a mutable object runs locally at about 180 - 200ms for a lazy collection and
20-25ms for a strict collection, after JVM optimizations:

```Java
Foo fooLazy = STRef.<Foo>stRefCreator()
                   .createSTRef(new Foo(take(10, iterate(n -> n + 1, 1)), 0))
                   .flatMap(s -> s.modifySTRef(trampoline(f -> f.n > 10_000_000 ? terminate(f) : recurse(f.incMutable()))))
                   .flatMap(STRef::readSTRef)
                   .runST();

ArrayList<Integer> m = toCollection(ArrayList::new, take(10, iterate(n -> n + 1, 1));
Foo fooStrict = STRef.<Foo>stRefCreator()
                     .createSTRef(new Foo(m, 0))
                     .flatMap(s -> s.modifySTRef(trampoline(f -> f.n > 10_000_000 ? terminate(f) : recurse(f.incMutable()))))
                     .flatMap(STRef::readSTRef)
                     .runST();
```

while the `foldLeft` implementation with immutable objects runs around 13000 - 15000ms for a lazily-constructed
  `Iterable`, and 1300 - 1500ms if the `Iterable` is forced into an `ArrayList`:

```Java
Foo fooLazy = foldLeft((acc, n) -> acc.incImmutable(), 
                       new Foo(take(10, iterate(n -> n + 1, 1)), 0),
                       replicate(10_000_000, 1));

ArrayList<Integer> m = toCollection(ArrayList::new, take(10, iterate(n -> n + 1, 1));
Foo fooStrict = foldLeft((acc, n) -> acc.incImmutable(), 
                         new Foo(m, 0),
                         replicate(10_000_000, 1));
```

