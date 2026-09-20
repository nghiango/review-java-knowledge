# Review: metric line encoder

Review `MetricLineEncoder.java` as code called once per emitted metric. Consider allocation rate,
determinism, escaping, logging volume, validation, and thread safety. Write findings before opening
`SOLUTION.md`.

Reproduce with one million finite iterations under JFR using the `profile` settings; compare
allocation samples and GC pauses with the correct encoder.
