## Immutability and Java 21 data types

An immutable object has stable observable state: final references are insufficient if they point to
mutable collections. Copy mutable inputs and outputs. Records provide final components and value
semantics but are only shallowly immutable. Enums model a closed set of instances; sealed classes
model a closed hierarchy with variant-specific state. Java 21 pattern matching makes exhaustive
`switch` over sealed hierarchies practical.
