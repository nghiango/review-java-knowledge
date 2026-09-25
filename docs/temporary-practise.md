## Question 1

Question 1 — Spring Data JPA: @Transactional, Lazy Loading & OSIV
Consider:
```
@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public Order getOrder(Long id) {
        return orderRepository.findById(id)
            .orElseThrow();
    }
}
```
Entity:
```
@Entity
public class Order {

    @Id
    private Long id;

    @OneToMany(
        mappedBy = "order",
        fetch = FetchType.LAZY
    )
    private List<OrderItem> items;
}
```
Controller:
```
@GetMapping("/orders/{id}")
public OrderResponse getOrder(@PathVariable Long id) {
    Order order = orderService.getOrder(id);

    return new OrderResponse(
        order.getId(),
        order.getItems().stream()
            .map(OrderItem::getName)
            .toList()
    );
}
```
Production has:
spring:
  jpa:
    open-in-view: false
Everything compiles, but the endpoint fails at runtime when accessing order.getItems().
Explain:
1. Why does it fail?
2. Would adding @Transactional to OrderService.getOrder() fix it? Why or why not?
3. Give me at least two production-quality solutions, and explain which one you would prefer for a read API like this.

## Answer 1

With OSIV disabled, the entity is detached before the controller accesses the lazy collection, so Hibernate cannot initialize items. Adding @Transactional only around getOrder() doesn't help because the transaction ends before the controller accesses items. I would perform entity access and DTO mapping inside a transactional service and explicitly fetch the required relationship using JOIN FETCH or EntityGraph. For a read-only API with a fixed response shape, I would also consider DTO projection to avoid loading unnecessary entity state.

Note: Persistence context lifetime is different with connection lifetime. OSIV just terminates the persistence context.

## Question 2

```
@GetMapping("/orders")
public Page<OrderResponse> getOrders(Pageable pageable) {
    ...
}
```
Each response needs the order plus all its items.
A developer writes:
```
@Query("""
    SELECT o
    FROM Order o
    JOIN FETCH o.items
""")
Page<Order> findAllWithItems(Pageable pageable);
```

There are 100 orders, each with 10 items, and the requested page size is 20 orders.
Why is combining JOIN FETCH on a @OneToMany collection with pagination problematic?
How would you redesign this query so that pagination remains correct and you still avoid N+1?

## Answer 2

Pagination should operate on Orders, but a to-many fetch join multiplies each Order into multiple SQL rows. Therefore applying limit/offset directly to that joined result doesn't cleanly represent 20 distinct Orders. I would first page the Order IDs, then fetch those Orders and their items with a second IN query and preserve the original ordering. Alternatively, batch fetching can avoid N+1 while keeping the parent query pageable.

Note: Fetch one order and get list items of it, which doesn't consider as N + 1
