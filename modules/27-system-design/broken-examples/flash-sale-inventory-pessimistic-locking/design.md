# System Design Proposal: Flash Sale Inventory System

## 1. System Requirements & Load
- **Event**: Flash sale offering 10,000 units of a flagship smartphone.
- **Traffic**: Anticipated peak of 100,000 incoming purchase requests per second for the first 60 seconds.
- **Invariant**: Strict zero-overselling guarantee ($Stock \ge 0$).

## 2. Proposed Architecture & Workflow
```
[100,000 req/s] ---> [Spring Boot API Instances] ---> [Primary PostgreSQL DB]
```

### Proposed Inventory Deduction Service
```java
@Transactional
public boolean reserveInventory(Long productId, int quantity) {
    // 1. Acquire pessimistic row-level exclusive lock on the single product inventory row
    Inventory inventory = entityManager.createQuery(
        "SELECT i FROM Inventory i WHERE i.productId = :id", Inventory.class)
        .setParameter("id", productId)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE) // SELECT ... FOR UPDATE
        .getSingleResult();

    // 2. Check stock and decrement
    if (inventory.getAvailableStock() >= quantity) {
        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        orderRepository.save(new Order(productId, quantity, OrderStatus.CONFIRMED));
        return true;
    }
    
    return false;
}
```

## 3. Data Storage
A single `inventory` table in PostgreSQL storing `product_id` and `available_stock`.
The author argues that PostgreSQL ACID transactions and `PESSIMISTIC_WRITE` guarantee 100% data consistency without overselling.
