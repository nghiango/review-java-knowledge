# Caching and Redis Internals

Deep dive into Spring's cache proxy mechanics, Redis event loop architecture, Lettuce Netty multiplexing, and distributed lock consensus.

---

## 1. Spring CacheInterceptor & AOP Proxy Mechanics

Spring declarative caching is implemented via an AOP method interception pipeline driven by `CacheInterceptor`:

```mermaid
sequenceDiagram
    autonumber
    participant Caller
    participant Proxy as Spring AOP Proxy (CGLIB)
    participant Interceptor as CacheInterceptor
    participant CacheMgr as CacheManager (RedisCacheManager)
    participant Target as Target Business Bean
    participant Redis as Redis Server

    Caller->>Proxy: invoke getProduct(1L)
    Proxy->>Interceptor: invoke(MethodInvocation)
    Interceptor->>Interceptor: Evaluate key SpEL (#id -> 1L)
    Interceptor->>CacheMgr: getCache("products")
    CacheMgr->>Redis: GET products::1
    alt Cache Hit
        Redis-->>CacheMgr: Serialized Payload
        CacheMgr-->>Interceptor: Product(1, "Laptop", ...)
        Interceptor-->>Caller: Return Cached Object
    else Cache Miss
        Redis-->>CacheMgr: nil
        Interceptor->>Target: proceed() (Target DB Query)
        Target-->>Interceptor: Fresh Product from DB
        Interceptor->>CacheMgr: put("products::1", Product)
        CacheMgr->>Redis: SET products::1 <json> EX <ttl>
        Interceptor-->>Caller: Return Fresh Object
    end
```

### Key Internal Classes
- `CacheAspectSupport`: Base class managing cache operation metadata, key generation, and condition evaluation.
- `CacheOperationSource`: Parses annotations (`@Cacheable`, `@CachePut`, `@CacheEvict`) into runtime `CacheOperation` definitions.
- `KeyGenerator`: Evaluates SpEL expressions or defaults to `SimpleKeyGenerator`.

---

## 2. Redis Single-Threaded Event Loop (Reactor Pattern)

A common question is why Redis delivers >100,000 operations per second despite executing commands on a single main execution thread:

```mermaid
flowchart TD
    subgraph "OS Kernel Network Layer"
        Socket1["Client Socket 1 (Connected)"]
        Socket2["Client Socket 2 (Read Ready)"]
        Socket3["Client Socket 3 (Write Ready)"]
    end

    Multiplexer["I/O Multiplexer (epoll / kqueue / select)"]

    subgraph "Redis Server Process"
        EventLoop["Non-Blocking Event Loop (ae.c)"]
        FileEventHandler["File Event Dispatcher"]
        CommandHandler["Main Command Processor (Single Thread)<br/>- String, Hash, ZSet mutations in RAM<br/>- O(1) in-memory operations<br/>- Zero thread context-switching"]
    end

    Socket1 --> Multiplexer
    Socket2 --> Multiplexer
    Socket3 --> Multiplexer
    Multiplexer --> EventLoop
    EventLoop --> FileEventHandler
    FileEventHandler --> CommandHandler
```

### Why Redis is Fast
1. **In-Memory Storage**: All primary data resides directly in physical RAM. Memory access latency is measured in tens of nanoseconds, bypassing mechanical or SSD storage bottlenecks.
2. **Non-Blocking I/O Multiplexing**: Redis uses OS primitives (`epoll` on Linux, `kqueue` on macOS) to monitor thousands of client socket connections concurrently without spawning dedicated operating system threads.
3. **Zero Lock Contention**: Because core commands execute sequentially on a single thread, data structure mutations require zero mutex locks, condition variables, or synchronized blocks, eliminating lock contention and thread context-switching overhead.
4. **I/O Threading (Redis 6.0+)**: In Redis 6.0+, network socket read/write parsing is offloaded to background I/O threads, while the core command execution logic remains strictly single-threaded.

---

## 3. Client Driver Architecture: Lettuce vs Jedis

```mermaid
flowchart LR
    subgraph "Jedis Driver Architecture"
        J_T1["Thread 1"] -->|Acquire| J_Pool["Jedis Connection Pool"]
        J_T2["Thread 2"] -->|Acquire| J_Pool
        J_Pool -->|Dedicated Socket 1| S1["Redis Node"]
        J_Pool -->|Dedicated Socket 2| S2["Redis Node"]
    end

    subgraph "Lettuce Driver Architecture"
        L_T1["Thread 1"] -->|Non-blocking Channel| Netty["Netty EventLoop (Pipelined Multiplexing)"]
        L_T2["Thread 2"] -->|Non-blocking Channel| Netty
        L_T3["Thread N"] -->|Non-blocking Channel| Netty
        Netty -->|Single Shared Socket| SharedSocket["Redis Node"]
    end
```

### Lettuce (Spring Boot Default)
- Built on **Netty** asynchronous event-driven I/O.
- Shares a single, thread-safe TCP connection across hundreds of concurrent threads.
- Automatically pipelines requests across the shared connection, dramatically reducing socket overhead.
- Under Java 21 Virtual Threads, virtual threads calling synchronous Lettuce methods yield cleanly without pinning carrier threads.

### Jedis
- Synchronous, blocking client.
- Requires dedicated connection pooling (`GenericObjectPool`).
- Each concurrent worker thread must acquire a dedicated physical connection socket.
- Under high concurrent load (e.g. 10,000 virtual threads), connection pool exhaustion causes thread queuing and latency degradation.

---

## 4. Redis Cluster Hash Slot Partitioning

Redis Cluster implements shared-nothing horizontal sharding using **16,384 logical Hash Slots**:

$$\text{Hash Slot} = \text{CRC16}(\text{key}) \pmod{16384}$$

```mermaid
flowchart TD
    Key["Key: {user:101}:orders"]
    Extractor["Extract Hash Tag: 'user:101'"]
    HashAlgo["CRC16('user:101') % 16384"]
    SlotNode["Slot 7421 -> Redis Node 2"]

    Key --> Extractor
    Extractor --> HashAlgo
    HashAlgo --> SlotNode
```

### Hash Tags (`{...}`)
- Multi-key operations (transactions, pipelines, Lua scripts) require all keys to reside on the same cluster node.
- Wrapping a substring in braces `{...}` forces Redis to compute the hash slot using **only the text inside the braces**.
- Example: `{user:101}:profile` and `{user:101}:orders` hash to the exact same slot, allowing atomic multi-key transactions across them without triggering `CROSSSLOT Keys in request don't hash to the same slot`.

---

## 5. Distributed Lock Consensus: Redlock & Fencing Tokens

The naive distributed lock (`SET key value NX PX`) is safe for soft concurrency control, but in safety-critical systems, process pauses create split-brain hazards:

```mermaid
sequenceDiagram
    autonumber
    participant ClientA as Worker A
    participant Redis as Redis Lock Store
    participant DB as Storage / Database
    participant ClientB as Worker B

    ClientA->>Redis: SET lock:res A NX PX 5000 (Acquired)
    ClientA->>ClientA: Long JVM GC Pause (7000ms)
    Note over Redis: 5000ms elapsed: Lock expires!
    ClientB->>Redis: SET lock:res B NX PX 5000 (Acquired)
    ClientB->>DB: Write Data (Token 1)
    ClientA->>ClientA: GC Pause ends
    ClientA->>DB: Overwrite Data! (Split-Brain Hazard!)
```

### The Solution: Fencing Tokens
To prevent a paused worker from executing writes after its lease has expired, the lock manager issues a **monotonically increasing fencing token** (e.g. token 31, 32, 33). The primary storage layer rejects any write that presents a token lower than the highest token previously committed.
