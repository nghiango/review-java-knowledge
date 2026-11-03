## Equality, hashing and ordering

- `==` compares primitive values or reference identity.
- `equals` defines logical equality. It must be
reflexive, symmetric, transitive, consistent and false for null.

??? example "Example"

    ```java
    String s1 = "hello";
    String s2 = "hello";
    System.out.println(s1 == s2); // true (string pool)
    System.out.println(s1.equals(s2)); // true

    String s3 = new String("hello");
    String s4 = new String("hello");
    System.out.println(s3 == s4); // false (different objects)
    System.out.println(s3.equals(s4)); // true (same content)

    String s5 = "hello";
    String s6 = new String("hello");
    System.out.println(s5 == s6); // false (different objects)
    System.out.println(s5.equals(s6)); // true (same content)
    ```

- Equal objects must have equal hash codes; unequal objects may collide. Equality/hash state used as a HashMap key must not change while stored.
- `Comparable` defines a type's natural order; `Comparator` defines external orders. A sorted set uses
comparison equality (`compare == 0`) for uniqueness, so inconsistency with `equals` can surprise
callers even though Java permits it.

??? example "Example"

    ```java
    public class Person implements Comparable<Person> {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Person person = (Person) obj;
            return age == person.age && Objects.equals(name, person.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age);
        }

        @Override
        public int compareTo(Person other) {
            int nameCompare = name.compareTo(other.name);
            if (nameCompare != 0) return nameCompare;
            return Integer.compare(age, other.age);
        }
    }
    ```

### The contract between `equals` and `hashCode`

- If two objects are equal according to `equals()`, they must have the same hash code.
- If two objects are not equal according to `equals()`, they may or may not have the same hash code.
- If two objects have the same hash code, they are not necessarily equal. Hash collision are allowed.
- If an object's equality-related state doesn't change, its hash code should remain consistent during execution.

![Equals and HashCode Contract](images/equals-hashcode-contract.png)

### How HashMap uses equals and hashCode

![HashMap lookup](images/hashmap-lookup.png)

In a typical HashMap lookup:

1. The key's `hashCode()` is called to determine the bucket index.
2. If multiple keys hash to the same bucket (hash collision), the map uses `equals()` to find the exact key.

The constructor of a HashMap is an array of buckets, where each bucket is a linked list or tree.

From java 8, when a bucket reachs at least 8 elements and array size is equals or greater than 64, the bucket is converted to a tree.

![How HashMap works](images/how-hashmap-works.png)

### Model interview answer

??? question "Sample answer"
    In Java, ==, equals(), and hashCode() serve different purposes.

    The == operator compares values for primitive types and reference identity for objects.

    The equals() method is used to check logical equality. By default, it behaves like reference comparison, but classes such as String override it to compare their contents.

    The hashCode() method generates an integer hash value used by hash-based collections such as HashMap and HashSet.

    The important contract is that if two objects are equal according to equals(), they must return the same hash code. However, two objects with the same hash code are not necessarily equal because hash collisions are possible.

    Therefore, whenever I override equals(), I also override hashCode() using the same equality-defining fields. Otherwise, hash-based collections may behave incorrectly.

    I also avoid modifying fields involved in equality while an object is stored as a key in a HashMap.
