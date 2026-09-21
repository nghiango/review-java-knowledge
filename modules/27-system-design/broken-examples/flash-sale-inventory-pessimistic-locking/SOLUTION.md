# Solution: Flash Sale Inventory System

## Annotated Target Review

```markdown
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
    # Scalability issue: Serializing 100,000 req/sec through an exclusive row-level database lock (FOR UPDATE) collapses database throughput.
    # Performance issue: Thousands of concurrent threads queue in PostgreSQL lock manager, driving DB CPU to 100% on context switching and spinning.
    Inventory inventory = entityManager.createQuery(
        "SELECT i FROM Inventory i WHERE i.productId = :id", Inventory.class)
        .setParameter("id", productId)
        .setLockMode(LockModeType.PESSIMISTIC_WRITE) // SELECT ... FOR UPDATE
        .getSingleResult();

    # Performance issue: Synchronously creating order rows within the same locked transaction multiplies lock hold duration.
    if (inventory.getAvailableStock() >= quantity) {
        inventory.setAvailableStock(inventory.getAvailableStock() - quantity);
        orderRepository.save(new Order(productId, quantity, OrderStatus.CONFIRMED));
        return true;
    }
    
    return false;
}
```

## 3. Data Storage
# Resilience issue: Direct coupling of extreme flash traffic directly to relational storage without an in-memory caching tier or message queue buffer guarantees catastrophic database failure.
A single `inventory` table in PostgreSQL storing `product_id` and `available_stock`.
The author argues that PostgreSQL ACID transactions and `PESSIMISTIC_WRITE` guarantee 100% data consistency without overselling.
```

---

## Discovered Issues

### 1. The Hot-Row Lock Manager Collapse
While `SELECT ... FOR UPDATE` guarantees consistency in theory, in practice:
- Relational databases (PostgreSQL/MySQL) are designed to handle concurrent transactions accessing **different rows**.
- When 100,000 transactions target the **exact same row** in a single second, the database engine can only process them **serially, one transaction at a time**.
- If each transaction takes only $2\text{ms}$ (lock, fetch, update, insert order, commit), the maximum theoretical throughput is $1000\text{ms} / 2\text{ms} = 500\text{ transactions/sec}$.
- The remaining 99,500 requests per second queue up in the PostgreSQL lock table and connection queues.

### 2. Cascading Connection Exhaustion & Node Crash
Within $< 2\text{seconds}$ of the sale launch:
- All HikariCP pools across all application instances are 100% full waiting for the hot row lock.
- Application threads block, causing HTTP request queues in Tomcat / Netty to fill up and reject incoming requests with HTTP 503 Service Unavailable.
- PostgreSQL CPU spikes to 100% purely on kernel context switching, spinlock contention, and lock manager hashtable lookups.
- Unrelated microservices sharing the same database instance experience complete connection timeout failures.

---

## Correct Implementation

See [`correct/design.md`](correct/design.md) for the multi-tier production flash sale architecture featuring **Redis in-memory inventory pre-allocation with Lua scripting**, **Kafka asynchronous order buffering**, and **database background persistence with optimistic updates**.
