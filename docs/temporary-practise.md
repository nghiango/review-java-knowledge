## Question 1

You load the same database customer in two different JPA persistence contexts:
Customer c1 = entityManager1.find(Customer.class, 123L);
Customer c2 = entityManager2.find(Customer.class, 123L);

System.out.println(c1 == c2);
System.out.println(c1.equals(c2));
Assume Customer does not override equals() or hashCode() at all.
What would you expect from these two comparisons, and why can this become a problem when working with Set<Customer> across transaction boundaries?

## Answer 1

For c1 == c2 return false, and c1.equals(c2) return false. Because == will compare references and then two objects are different, and equals compare using Object.equals() which uses == inside of it.
