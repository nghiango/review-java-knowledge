## Streams

A stream describes a pipeline; it does not store data. Intermediate operations are lazy and a
terminal operation drives traversal. Stateless operations handle each element independently;
stateful operations such as `sorted` and `distinct` may buffer or create barriers. Short-circuiting
can avoid full traversal.

Keep pipelines non-interfering and stateless. Collect through framework collectors rather than
mutating external containers. Parallel streams are useful only for sufficiently large, splittable,
CPU-bound, side-effect-free work after measurement; they are a poor default for blocking I/O or
request-path work sharing the common pool.
