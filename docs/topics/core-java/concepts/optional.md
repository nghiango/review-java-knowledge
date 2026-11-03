## Optional

Optional is primarily a return type for expected absence. It is not a universal null replacement:
fields and parameters often create double absence, serialization friction and noisy calls. Prefer
`map`, `flatMap`, `filter`, `orElseGet` and contextual `orElseThrow`; avoid unproved `get()`.
