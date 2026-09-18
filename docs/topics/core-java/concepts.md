# Core Java Concepts

## Object design and SOLID

Encapsulation protects invariants, not merely fields. Abstraction exposes a stable contract;
polymorphism lets callers depend on that contract. Prefer composition when a relationship is
"uses/has" or when subtype equality would be ambiguous. Inheritance fits genuine substitutability.

SOLID is diagnostic vocabulary, not a target score: SRP identifies reasons to change; OCP protects
stable policy with extension points; LSP demands substitutability; ISP avoids forcing unused
capabilities; DIP points policy toward abstractions. Too many tiny interfaces can be as costly as a
god class.

## Equality, hashing and ordering

`==` compares primitive values or reference identity. `equals` defines logical equality. It must be
reflexive, symmetric, transitive, consistent and false for null. Equal objects must have equal hash
codes; unequal objects may collide. Equality/hash state used as a HashMap key must not change while
stored.

`Comparable` defines a type's natural order; `Comparator` defines external orders. A sorted set uses
comparison equality (`compare == 0`) for uniqueness, so inconsistency with `equals` can surprise
callers even though Java permits it.

## Immutability and Java 21 data types

An immutable object has stable observable state: final references are insufficient if they point to
mutable collections. Copy mutable inputs and outputs. Records provide final components and value
semantics but are only shallowly immutable. Enums model a closed set of instances; sealed classes
model a closed hierarchy with variant-specific state. Java 21 pattern matching makes exhaustive
`switch` over sealed hierarchies practical.

## Strings, values and modifiers

String is immutable and may share interned literals. Use StringBuilder for iterative construction;
modern `+` expressions may be optimized, but concatenation in a loop repeatedly creates values.
Java is always pass-by-value: an object reference is itself a copied value, so a method can mutate
the referenced object but cannot replace the caller's variable. `final` prevents reassignment or
override depending on context; `static` belongs to the class, not an instance.

`var` changes local syntax, not static typing. Use it when the initializer makes the type obvious.
Use `java.time` immutable types and explicit zones; avoid legacy mutable date APIs.

## Exceptions and resources

Checked exceptions make a recoverable condition part of the signature; unchecked exceptions often
represent contract violations or failures callers cannot locally recover from. The boundary matters
more than taxonomy: catch only when the layer can add context, recover or translate. Preserve the
cause.

Try-with-resources closes in reverse declaration order. If body and close both fail, the body failure
is primary and close failures are suppressed. Resource ownership must be explicit.

## Optional

Optional is primarily a return type for expected absence. It is not a universal null replacement:
fields and parameters often create double absence, serialization friction and noisy calls. Prefer
`map`, `flatMap`, `filter`, `orElseGet` and contextual `orElseThrow`; avoid unproved `get()`.

## Collections

Choose by contract: List preserves position/duplicates; Set enforces uniqueness; Map associates
keys; Queue/Deque model processing order. ArrayList gives O(1) indexed access and amortized append
but O(n) middle insertion. LinkedList rarely wins due to traversal and locality costs. Hash
collections offer expected O(1) lookup under stable hashing; tree collections offer O(log n) sorted
operations. Sequenced collections expose first/last/reversed order consistently.

Fail-fast iterators detect structural modification on a best-effort basis; they are bug detectors,
not concurrency controls. ConcurrentHashMap coordinates concurrent access but still requires stable
keys and atomic compound operations (`compute`, `merge`) where appropriate.

## Generics and PECS

Generics provide compile-time type safety but most type parameters are erased. Invariance means
`List<Integer>` is not a subtype of `List<Number>`. A producer is `? extends T`; a consumer is
`? super T`. Wildcards express use-site flexibility; a type parameter is preferable when multiple
positions must share the same unknown type.

## Streams

A stream describes a pipeline; it does not store data. Intermediate operations are lazy and a
terminal operation drives traversal. Stateless operations handle each element independently;
stateful operations such as `sorted` and `distinct` may buffer or create barriers. Short-circuiting
can avoid full traversal.

Keep pipelines non-interfering and stateless. Collect through framework collectors rather than
mutating external containers. Parallel streams are useful only for sufficiently large, splittable,
CPU-bound, side-effect-free work after measurement; they are a poor default for blocking I/O or
request-path work sharing the common pool.

## Related

- [Internals](internals.md)
- [Code review](code-review.md)
- [Production](production.md)
