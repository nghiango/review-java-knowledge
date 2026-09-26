# System Design Interview Questions

Senior Java and backend engineer interview questions covering distributed systems architecture, capacity planning, data partitioning, caching topologies, consensus, and the 8 canonical design problems.

---

<!-- --8<-- [start:basic] -->
## Basic Concepts (1–8)

### 1. How should a senior engineer structure a 45-minute system design interview, and what are the primary failure modes in communication?

What specific activities belong in each phase of the interview?

??? question "Reveal answer"
    - **Phase 1: Requirements & Scope (5–8 minutes)**:
      - *Functional Requirements (FRs)*: Agree on 3–4 core user capabilities. Explicitly define what is *out of scope*.
      - *Non-Functional Requirements (NFRs)*: Define availability SLA (e.g. 99.99%), p99 latency SLOs ($< 100\text{ms}$), read-to-write ratio, data consistency needs (strong vs eventual), and regulatory compliance (PCI-DSS, GDPR).
      - *Capacity Estimations*: Calculate Daily Active Users (DAU), average & peak QPS, ingress/egress bandwidth, and 5-year storage growth.
    - **Phase 2: High-Level Architecture (10–15 minutes)**:
      - Draw high-level component diagrams: Clients $\rightarrow$ CDN / DNS $\rightarrow$ API Gateway / Load Balancer $\rightarrow$ Application Services $\rightarrow$ Cache / Database.
      - Define API contracts (endpoints, parameters) and database schemas (core tables, primary keys).
    - **Phase 3: Deep Dives & Bottlenecks (15–20 minutes)**:
      - Deep-dive into 2–3 specific challenging areas: data partitioning/sharding keys, concurrency control, caching invalidation, message ordering, or distributed transactions.
    - **Phase 4: Resiliency & Failure Modes (5–8 minutes)**:
      - Discuss Single Points of Failure (SPOFs), network partitions, circuit breakers, rate limiting, and disaster recovery.
    - **Primary Candidate Failure Modes**:
      - Jumping into drawing boxes without clarifying requirements or scale.
      - Passive communication (waiting for the interviewer to prompt rather than leading the technical investigation).
      - Over-engineering early (e.g. adding Kafka and Cassandra for a low-throughput internal tool).

??? example "Example"
    ```text
    Standard 45-minute Interview Time Allocation:
    [00-08m] Requirements (FR/NFR) & Back-of-the-Envelope Capacity Estimations
    [08-22m] High-Level Component Architecture, API Contracts & Data Schema
    [22-38m] Detailed Deep Dives (Sharding, Concurrency, Caching Topologies)
    [38-45m] Failure Modes, Bottlenecks, Resiliency & Telemetry
    ```

---

### 2. How do you perform back-of-the-envelope capacity estimations for a system with 10 million Daily Active Users (DAU)?

Calculate the read QPS, write QPS, network bandwidth, and 5-year storage requirements assuming each user writes 2 posts per day (1 KB each) and views 20 posts per day.

??? question "Reveal answer"
    - **1. Traffic / QPS Calculations**:
      - *Seconds per day*: $24 \times 3,600 \approx 86,400\text{ seconds} \approx 10^5\text{ seconds}$ (standard estimation approximation).
      - *Write Volume*: $10\text{M users} \times 2\text{ writes} = 20\text{ million writes/day}$.
      - *Average Write QPS*: $\frac{20,000,000}{86,400} \approx 231\text{ writes/sec}$.
      - *Peak Write QPS ($2\times - 3\times$)*: $\approx 500 - 700\text{ writes/sec}$.
      - *Read Volume*: $10\text{M users} \times 20\text{ reads} = 200\text{ million reads/day}$.
      - *Average Read QPS*: $\frac{200,000,000}{86,400} \approx 2,315\text{ reads/sec}$.
      - *Peak Read QPS ($2\times - 3\times$)*: $\approx 5,000 - 7,000\text{ reads/sec}$.
    - **2. Storage Capacity Calculations**:
      - *Daily Storage*: $20\text{M writes} \times 1\text{ KB} = 20\text{ GB/day}$.
      - *Annual Storage*: $20\text{ GB} \times 365 \approx 7.3\text{ TB/year}$.
      - *5-Year Storage*: $7.3\text{ TB} \times 5 \approx 36.5\text{ TB}$.
      - Adding index overhead ($+30\%$) and replication ($3\times$ copies):
        $$\text{Total 5-Year Storage} = 36.5\text{ TB} \times 1.3 \times 3 \approx 142\text{ TB}$$
    - **3. Ingress / Egress Network Bandwidth**:
      - *Write Bandwidth (Ingress)*: $231\text{ req/s} \times 1\text{ KB} \approx 231\text{ KB/sec} \approx 1.85\text{ Mbps}$.
      - *Read Bandwidth (Egress)*: $2,315\text{ req/s} \times 1\text{ KB} \approx 2.3\text{ MB/sec} \approx 18.5\text{ Mbps}$.

??? example "Example"
    ```text
    Rule of Thumb Equivalences for System Design Interviews:
    - 1 million requests/day ≈ 12 requests/second
    - 10 million requests/day ≈ 116 requests/second
    - 100 million requests/day ≈ 1,160 requests/second
    - 1 billion requests/day ≈ 11,600 requests/second
    - 86,400 seconds/day ≈ 10^5 seconds
    ```

---

### 3. When designing a URL Shortener (TinyURL), should redirection responses use HTTP 301 Moved Permanently or HTTP 302 Found?

What are the architectural trade-offs regarding analytics tracking versus server load?

??? question "Reveal answer"
    - **HTTP 301 Moved Permanently**:
      - *Behavior*: Browsers and intermediate caching proxies cache the redirection permanently on the client side. Subsequent clicks to the short link never hit the URL shortener backend; the browser redirects directly to the long URL from local cache.
      - *Pros*: Dramatically reduces load on the URL shortener application servers and databases.
      - *Cons*: **Eliminates real-time click analytics**. Because subsequent requests bypass the backend, you cannot count total clicks, record user IP/locations, or track referrer headers.
    - **HTTP 302 Found (or HTTP 307 Temporary Redirect)**:
      - *Behavior*: Browsers do not cache the redirection. Every single click to the short link sends an HTTP request to the URL shortener service before redirecting.
      - *Pros*: Guarantees **100% accurate click analytics tracking**, real-time clickstream telemetry, and dynamic destination link redirection.
      - *Cons*: Every click consumes backend compute, network, and database/cache lookup resources.
    - **Senior Recommendation**:
      - Use **HTTP 302 / 307** if real-time click metrics and campaign tracking are core business requirements. Use a high-speed in-memory cache (Redis) to serve 302 lookups in $< 2\text{ms}$.

??? example "Example"
    ```java
    // Spring Boot Controller returning HTTP 302 Found for click analytics tracking
    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode, HttpServletRequest request) {
        String longUrl = urlShortenerService.resolveAndRecordClick(shortCode, request.getRemoteAddr(), request.getHeader("User-Agent"));
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(longUrl))
            .build();
    }
    ```

---

### 4. What architectural criteria determine whether to choose a Relational Database (SQL) versus a NoSQL database?

When is a document database like MongoDB preferable, and when is PostgreSQL mandatory?

??? question "Reveal answer"
    - **Choose Relational (PostgreSQL / MySQL / Aurora) when**:
      - *ACID Transactions*: Workloads require multi-table, multi-row atomicity and consistency guarantees (financial ledgers, payment processing, inventory reservation).
      - *Structured Data with Complex Joins*: Relational schemas where entities have strong foreign-key relationships (Customers $\leftrightarrow$ Orders $\leftrightarrow$ Line Items).
      - *Strict Schema Enforcement*: Preventing malformed or corrupted records from being persisted.
    - **Choose Key-Value (Redis / DynamoDB) when**:
      - High-throughput lookups by primary key with predictable sub-5ms latency (session storage, shopping carts, rate-limiting tokens).
    - **Choose Document (MongoDB / Couchbase) when**:
      - Dynamic, hierarchical, semi-structured data where entities are accessed as complete self-contained documents (product catalogs with varying custom attributes per category).
    - **Choose Wide-Column / LSM-Tree (Cassandra / ScyllaDB) when**:
      - Extreme write-heavy ingestion (100,000+ writes/sec) with simple primary-key lookup patterns (IoT sensor telemetry, user activity audit logs, chat message history).

??? example "Example"
    ```sql
    -- Relational Schema: Enforcing multi-table foreign keys and ACID constraints
    CREATE TABLE orders (
        id UUID PRIMARY KEY,
        customer_id UUID NOT NULL REFERENCES customers(id),
        total_cents BIGINT NOT NULL,
        status VARCHAR(20) NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    );
    ```

---

### 5. Explain the Cache-Aside (Lazy Loading) pattern, and trace what happens during a cache miss and a cache eviction.

Why is evicting a cache key (`DEL`) on database update safer than updating the cache key (`SET`)?

??? question "Reveal answer"
    - **Cache-Aside Read Flow**:
      1. Application receives a read request for `customer:123`.
      2. Application queries the cache (Redis).
      3. If *Cache Hit*: returns cached data immediately ($< 2\text{ms}$).
      4. If *Cache Miss*: queries the primary database, populates the cache with the retrieved record (attaching a TTL), and returns data to the client.
    - **Cache-Aside Write Flow**:
      1. Application writes the updated data to the primary database.
      2. Application **evicts (deletes)** the corresponding key from the cache (`DEL customer:123`).
    - **Why Evicting is Safer than Updating Cache (`DEL` vs `SET`)**:
      - Updating the cache on write introduces a **concurrent write race condition**:
        - Thread A writes value $V1$ to DB.
        - Thread B writes value $V2$ to DB.
        - Due to network scheduling, Thread B updates cache with $V2$ first.
        - Thread A then updates cache with $V1$ second.
        - The cache permanently retains stale value $V1$, while the database holds $V2$!
      - Deleting the key on write eliminates this race: the next read simply fetches the true database state and repopulates the cache.

??? example "Example"
    ```java
    // Spring Cache abstraction implementing Cache-Aside
    @Service
    public class CustomerService {
        @Cacheable(value = "customers", key = "#id")
        public CustomerDTO getCustomer(UUID id) {
            return customerRepository.findById(id).map(CustomerDTO::fromEntity)
                .orElseThrow(() -> new NotFoundException(id));
        }

        @CacheEvict(value = "customers", key = "#customer.id()")
        @Transactional
        public void updateCustomer(CustomerUpdateCommand customer) {
            customerRepository.update(customer);
        }
    }
    ```

---

### 6. What are the limits of vertical scaling (scaling up) versus horizontal scaling (scaling out) in backend architectures?

At what layer of the application architecture is horizontal scaling trivial, and where is it difficult?

??? question "Reveal answer"
    - **Vertical Scaling (Scale Up)**:
      - Adding more CPU cores, RAM, and faster NVMe disks to an existing physical/virtual server.
      - *Pros*: Zero architectural complexity; code runs unchanged without distributed coordination or network latency.
      - *Limits*: Hard hardware ceiling (e.g. AWS `u-24tb1.112xlarge` has 448 vCPUs and 24 TB RAM); diminishing returns per dollar; single point of failure (hardware crash brings down entire service).
    - **Horizontal Scaling (Scale Out)**:
      - Adding more independent compute nodes (containers, EC2 instances) behind a load balancer.
      - *Where it is Trivial*: The **Stateless Application Tier** (Spring Boot web services). Stateless nodes store zero session state locally in memory; any node can handle any incoming request.
      - *Where it is Difficult*: The **Stateful Persistence Tier** (Relational Databases). Distributing relational data across multiple nodes requires distributed transactions, distributed joins, sharding keys, split-brain consensus, and cross-shard replication lag.

??? example "Example"
    ```mermaid
    flowchart TD
        LB["Application Load Balancer"]
        subgraph StatelessTier ["Stateless Tier (Horizontally Scaled: 2 to 100 Instances)"]
            App1["Spring Boot Task 1"]
            App2["Spring Boot Task 2"]
            AppN["Spring Boot Task N"]
        end
        LB --> App1
        LB --> App2
        LB --> AppN
        App1 --> State["Stateful Tier (Shared DB / Redis Cluster)"]
        App2 --> State
        AppN --> State
    ```

---

### 7. How do you identify and eliminate Single Points of Failure (SPOFs) across a distributed web architecture?

Analyze SPOFs across DNS, Ingress, Application, and Database tiers.

??? question "Reveal answer"
    - **1. DNS Tier**:
      - *SPOF Risk*: Single nameserver or DNS provider outage.
      - *Remediation*: Use multi-provider Anycast DNS (e.g. Route 53 paired with Cloudflare) to route around global BGP routing leaks.
    - **2. Ingress Tier**:
      - *SPOF Risk*: Single load balancer instance or availability zone outage.
      - *Remediation*: Application Load Balancer deployed across at least 3 distinct Availability Zones with automated health-check failover.
    - **3. Application Tier**:
      - *SPOF Risk*: Single container crash or host hardware lockup.
      - *Remediation*: Minimum replica count of $\ge 2$ distributed across multiple AZs with auto-healing (ECS task definition / K8s Deployment).
    - **4. Database Tier**:
      - *SPOF Risk*: Single RDS host failure.
      - *Remediation*: Multi-AZ synchronous standby deployment with automated failover in $< 120\text{s}$, or Amazon Aurora with 6-way storage replication across 3 AZs.
    - **5. External Dependencies**:
      - *SPOF Risk*: Third-party payment gateway or email provider outage hangs application threads.
      - *Remediation*: Circuit breakers (Resilience4j), timeouts on every remote call, and fallback queues.

??? example "Example"
    ```hcl
    # Eliminating SPOF via Multi-AZ Target Groups in Terraform
    resource "aws_lb" "app" {
      name               = "production-alb"
      internal           = false
      load_balancer_type = "application"
      subnets            = [aws_subnet.public_az_a.id, aws_subnet.public_az_b.id, aws_subnet.public_az_c.id]
    }
    ```

---

### 8. Compare Polling, Long Polling, WebSockets, and Server-Sent Events (SSE) for delivering real-time notifications to web and mobile clients.

Which protocol is best suited for a real-time crypto price ticker versus a customer support chat application?

??? question "Reveal answer"
    - **1. Short Polling**:
      - Client sends HTTP request every $N$ seconds. Server responds immediately with new data or empty payload.
      - *Trade-off*: Enormous waste of network bandwidth and server CPU on empty responses; high latency ($N$ seconds).
    - **2. Long Polling**:
      - Client opens an HTTP request; server holds the connection open until new data arrives or timeout (e.g. 20s) expires.
      - *Trade-off*: Good for low-frequency updates, but reconnect overhead on every message.
    - **3. Server-Sent Events (SSE)**:
      - Unidirectional persistent HTTP streaming connection (`Content-Type: text/event-stream`) from server to client.
      - *Best Suited For*: **Crypto price tickers, stock feeds, or live dashboards**. Lightweight, works natively over HTTP/2, automatic reconnection, built-in browser support via `EventSource`.
    - **4. WebSockets**:
      - Full-duplex, bidirectional persistent TCP connection established via HTTP handshake (`Upgrade: websocket`).
      - *Best Suited For*: **Customer support chat, multiplayer gaming, collaborative document editing**. Both client and server can send arbitrary messages with sub-millisecond framing overhead.

??? example "Example"
    ```java
    // Spring MVC Controller delivering real-time stock ticker updates via Server-Sent Events (SSE)
    @GetMapping(value = "/api/stocks/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamStockPrices() {
        SseEmitter emitter = new SseEmitter(180_000L); // 3-minute timeout
        stockPriceService.registerListener(emitter::send);
        emitter.onCompletion(() -> stockPriceService.removeListener(emitter));
        return emitter;
    }
    ```

---

<!-- --8<-- [end:basic] -->

---

<!-- --8<-- [start:intermediate] -->
## Intermediate System Blueprints (9–16)

### 9. Design a URL Shortener (TinyURL) handling 100 million new URLs per month with sub-10ms redirection latency.

Explain Base62 encoding, unique ID allocation, and database schema design.

??? question "Reveal answer"
    - **1. Capacity & Mathematical Estimations**:
      - 100M URLs/month $\approx 40\text{ writes/sec}$.
      - 100:1 Read-to-Write ratio $\approx 4,000\text{ reads/sec}$.
      - Storage over 5 years: $100\text{M} \times 12 \times 5 = 6\text{ billion URLs}$.
      - At 500 bytes per record, $6\text{B} \times 500\text{B} \approx 3\text{ TB}$ of storage.
    - **2. Base62 Short Code Encoding**:
      - Characters: $[a-z, A-Z, 0-9]$ (62 possible characters).
      - With a 7-character string: $62^7 \approx 3.52\text{ trillion unique URLs}$ (vastly exceeding the 6 billion required).
    - **3. Unique ID Generation Strategy**:
      - Do NOT hash long URLs with MD5/SHA256 (requires truncating and handling collisions with expensive DB lookups).
      - **Range-Based ID Generator**: A distributed coordinator (Zookeeper or Redis) hands out discrete ID ranges (e.g. Server 1 gets IDs $1,000,000 - 1,999,999$).
      - Convert the 64-bit integer ID into a Base62 string:
        $$\text{ID } 11,157 \rightarrow \text{"2TX"}$$
    - **4. Storage & Caching Layer**:
      - Store in PostgreSQL, MongoDB, or DynamoDB:
        `CREATE TABLE url_mappings (short_code VARCHAR(7) PRIMARY KEY, long_url TEXT NOT NULL, created_at TIMESTAMP);`
      - Cache the top 20% of popular URLs in Redis Cache-Aside with 7-day TTL to satisfy the 4,000 read QPS in $< 2\text{ms}$.

??? example "Example"
    ```java
    // Base62 Converter for generating 7-character short codes from 64-bit IDs
    public class Base62 {
        private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

        public static String encode(long id) {
            StringBuilder sb = new StringBuilder();
            while (id > 0) {
                sb.append(ALPHABET.charAt((int) (id % 62)));
                id /= 62;
            }
            return sb.reverse().toString();
        }
    }
    ```

---

### 10. How does Consistent Hashing with virtual nodes solve the rebalancing problem in distributed caching clusters?

What happens to keys when a cache node crashes in a cluster of 10 nodes?

??? question "Reveal answer"
    - **The Failure of Modulo Hashing**:
      - Using `hash(key) % N`: If 1 of 10 nodes crashes, $N$ changes from 10 to 9.
      - Because nearly every key's hash modulo 9 differs from its modulo 10, **almost 100% of cached keys are suddenly assigned to wrong nodes**.
      - This causes a catastrophic cluster-wide cache miss, flooding databases with 100% of production traffic.
    - **Consistent Hashing Mechanics**:
      - Nodes and keys are mapped to a $2^{32} - 1$ circular hash space.
      - When Node 5 crashes, only the keys that were specifically stored on Node 5 are affected.
      - Those keys naturally fall to the next node moving clockwise around the ring.
      - **Only $\frac{1}{N}$ (10%) of keys are invalidated**, preserving 90% of cache hits.
    - **Virtual Nodes**:
      - Assigning 100–250 virtual tokens per physical node across the ring ensures uniform key distribution and prevents hot spots.

??? example "Example"
    ```java
    // Consistent Hash Ring using TreeMap in Java
    public class ConsistentHashRing<T> {
        private final SortedMap<Integer, T> ring = new TreeMap<>();
        private final int numberOfReplicas;

        public ConsistentHashRing(int numberOfReplicas, Collection<T> nodes) {
            this.numberOfReplicas = numberOfReplicas;
            for (T node : nodes) {
                add(node);
            }
        }

        public void add(T node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                ring.put(hash(node.toString() + "-vnode-" + i), node);
            }
        }

        public T get(String key) {
            if (ring.isEmpty()) return null;
            int hash = hash(key);
            SortedMap<Integer, T> tail = ring.tailMap(hash);
            int targetHash = tail.isEmpty() ? ring.firstKey() : tail.firstKey();
            return ring.get(targetHash);
        }

        private int hash(String key) {
            return key.hashCode() & 0x7fffffff;
        }
    }
    ```

---

### 11. How do you design an enterprise Notification Engine supporting Push (APNs/FCM), SMS (Twilio), and Email (SendGrid)?

How do you enforce user preferences, rate limits per recipient, and provider circuit breaking?

??? question "Reveal answer"
    - **1. Ingestion & Decoupling**:
      - Incoming notification requests are validated at the API Gateway and published to an Amazon SQS / Kafka queue (`notification-events`).
      - This protects downstream notification workers from sudden traffic spikes.
    - **2. User Preference & Deduplication Filter**:
      - Worker reads event, checks user preferences in Redis / PostgreSQL:
        - Has the user opted out of marketing SMS?
        - Is the user currently in a "Do Not Disturb" quiet-hours window?
        - Has the user already received an identical notification in the last 15 minutes (deduplication via Redis key)?
    - **3. Channel Workers & Priority Queues**:
      - Notifications are routed into dedicated channel queues: `push-queue`, `sms-queue`, `email-queue`.
      - High-priority transactional alerts (Password Reset, 2FA OTP) use a dedicated high-priority queue with zero wait time.
    - **4. Rate Limiting & Circuit Breaking**:
      - Rate limit per recipient (e.g. maximum 3 SMS per user per hour).
      - Channel workers wrap external API calls (Twilio, SendGrid) with **Resilience4j Circuit Breakers**. If Twilio returns 503 errors, the circuit opens, failing over to a backup SMS provider (AWS SNS) immediately.

??? example "Example"
    ```mermaid
    flowchart LR
        Service["Microservices"] --> Ingest["Notification API"]
        Ingest --> SQS["SQS Priority Queues"]
        SQS --> Filter["Preference & Dedup Worker"]
        Filter --> Redis[("User Prefs & Rate Limits")]
        Filter --> Push["Push Worker (APNs/FCM)"]
        Filter --> SMS["SMS Worker (Twilio/SNS)"]
        Filter --> Email["Email Worker (SendGrid/SES)"]
    ```

---

### 12. Design a Distributed Rate Limiter supporting multiple tiers (Free vs Premium) across an auto-scaled API Gateway cluster.

Compare Token Bucket versus Sliding Window Counter implementations.

??? question "Reveal answer"
    - **Algorithm Comparison**:
      - **Token Bucket**:
        - A bucket has capacity $C$ and refills at $R$ tokens/sec. Each request consumes 1 token.
        - *Pros*: Memory efficient ($O(1)$ space); easily handles legitimate temporary bursts up to capacity $C$.
        - *Cons*: Difficult to implement accurately in distributed multi-node clusters without synchronized clocks or race conditions.
      - **Sliding Window Counter**:
        - Combines the memory efficiency of fixed window with the accuracy of sliding window logs.
        - Approximates current request count:
          $$\text{Count} = \text{Requests in Current Window} + \left(\text{Requests in Previous Window} \times (1 - \text{Overlap Percentage})\right)$$
        - *Pros*: Memory efficient (stores only 2 integers per user); smooths bursts; $< 1\text{ms}$ execution in Redis.
    - **Multi-Tier Implementation**:
      - The API key's tier (e.g. Free = 60 req/min, Enterprise = 5,000 req/min) is embedded in the JWT claims or cached in Redis.
      - The Redis key incorporates the tier or user ID: `rate_limit:user123`.
      - A Redis Lua script atomically calculates current usage against the tier limit.

??? example "Example"
    ```lua
    -- Redis Lua script executing Sliding Window Counter
    local key = KEYS[1]
    local limit = tonumber(ARGV[1])
    local current_time = tonumber(ARGV[2])
    local current_window = math.floor(current_time / 60)
    
    local current_count = tonumber(redis.call('HGET', key, current_window) or '0')
    if current_count >= limit then
        return 0 -- Throttled
    end
    
    redis.call('HINCRBY', key, current_window, 1)
    redis.call('EXPIRE', key, 120) -- Keep previous window for smoothing calculation
    return 1 -- Allowed
    ```

---

### 13. Design a Large-Scale Asynchronous File Processing Pipeline (e.g. Video Transcoding or Document OCR).

How do you upload 5 GB files without routing file bytes through your Spring Boot web microservices?

??? question "Reveal answer"
    - **The Anti-Pattern (Routing Bytes Through App Servers)**:
      - Uploading a 5 GB video through Spring Boot (`MultipartFile`) consumes server memory, saturates HTTP worker threads, and crashes if the client's cellular connection drops after 4 GB.
    - **The Production Architecture (Direct-to-S3 Upload)**:
      1. *Client Request*: Client sends file metadata (name, size, checksum) to Spring Boot: `POST /api/files/upload-ticket`.
      2. *S3 Presigned URL & Multipart Upload*:
         - Spring Boot validates user quota and calls Amazon S3: `CreateMultipartUpload`.
         - Spring Boot generates a set of **Presigned Upload URLs** (one per 100 MB chunk) and returns them to the client.
      3. *Direct Parallel Upload*:
         - The client uploads chunks directly to Amazon S3 in parallel using standard HTTP `PUT`.
         - If chunk 3 fails due to a network glitch, the client retries only chunk 3, not the entire 5 GB.
      4. *Completion Event & Async Processing*:
         - Client calls `CompleteMultipartUpload`.
         - S3 emits an `ObjectCreated` event to **Amazon EventBridge**.
         - An asynchronous distributed worker fleet (AWS ECS tasks or Lambda) picks up the transcoding job from SQS, processes the file, and stores results in a CDN-backed S3 bucket.

??? example "Example"
    ```mermaid
    sequenceDiagram
        autonumber
        participant Client as Web/Mobile Client
        participant App as Spring Boot API
        participant S3 as Amazon S3
        participant Worker as Transcoding Worker Fleet

        Client->>App: POST /api/upload-ticket (file.mp4, 5GB)
        App->>S3: CreateMultipartUpload & Generate Presigned URLs
        App-->>Client: Return Presigned URLs for Chunks 1..50
        
        Note over Client,S3: Client uploads chunks directly to S3!<br/>Zero bytes touch Spring Boot memory!
        Client->>S3: PUT Chunk 1..50 (Direct to S3)
        Client->>S3: CompleteMultipartUpload
        
        S3->>Worker: S3 Event Notification via SQS
        Worker->>S3: Transcode video into HLS streams
    ```

---

### 14. Design an E-Commerce Order Management System using the Transactional Outbox Pattern and Change Data Capture (CDC).

How does this architecture eliminate dual-write inconsistencies between PostgreSQL and Apache Kafka?

??? question "Reveal answer"
    - **The Dual-Write Hazard**:
      - If code attempts to update PostgreSQL (`orderRepository.save(order)`) and then publish to Kafka (`kafkaTemplate.send("orders", event)`):
        - If Kafka is down, the DB commit succeeded, but downstream shipping/billing never receive the event.
        - If DB rollback occurs after Kafka publish, downstream systems process an order that was never committed.
    - **The Transactional Outbox Pattern**:
      - Create an `outbox_events` table in the exact same PostgreSQL database.
      - When an order is created, the order record and the outbox event are inserted **within the exact same local ACID transaction**:
        ```sql
        BEGIN;
        INSERT INTO orders (id, total, status) VALUES (...);
        INSERT INTO outbox_events (id, aggregate_type, payload) VALUES (...);
        COMMIT;
        ```
      - Guarantees 100% atomic consistency: either both exist or neither exists.
    - **Change Data Capture (Debezium / Kafka Connect)**:
      - A Debezium connector tails the PostgreSQL **Write-Ahead Log (WAL)**.
      - As soon as the outbox record is committed, Debezium streams the event directly into Apache Kafka with exactly-once / at-least-once delivery guarantees.
      - Zero application polling overhead; zero dual-write vulnerabilities.

??? example "Example"
    ```sql
    -- Outbox Table schema in PostgreSQL
    CREATE TABLE outbox_events (
        id UUID PRIMARY KEY,
        aggregate_type VARCHAR(50) NOT NULL,
        aggregate_id VARCHAR(50) NOT NULL,
        event_type VARCHAR(50) NOT NULL,
        payload JSONB NOT NULL,
        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
    );
    ```

---

### 15. Design a Hotel / Airline Ticket Booking System handling seat contention with a 10-minute temporary reservation holding timer.

How do you prevent seat overselling while gracefully handling customer payment abandonment?

??? question "Reveal answer"
    - **Two-Phase Booking Lifecycle**:
      1. *Phase 1: Reserve (Temporary Hold)*: Seat status transitions from `AVAILABLE` to `HELD`. A 10-minute countdown starts for payment.
      2. *Phase 2: Confirm (Permanent Booking)*: Upon payment success, status transitions to `BOOKED`.
      3. *Phase 3: Expire / Release*: If payment is not completed within 10 minutes, the seat automatically returns to `AVAILABLE`.
    - **Concurrency Control on Seat Selection**:
      - Relational database row with **Optimistic Concurrency Control**:
        ```sql
        UPDATE seats 
        SET status = 'HELD', hold_expires_at = NOW() + INTERVAL '10 minutes', version = version + 1
        WHERE id = :seatId AND status = 'AVAILABLE' AND version = :version;
        ```
      - If the update returns 0 rows modified, another user claimed the seat milliseconds earlier; return HTTP 409 Conflict.
    - **Automated Hold Expiration Mechanics**:
      - Do NOT run a scheduled polling query `SELECT * FROM seats WHERE status = 'HELD' AND hold_expires_at < NOW()` every second (scans entire table).
      - **Redis TTL Key Expiration Event / Delayed Message Queue**:
        - When seat is held, set a Redis key with 10-minute TTL: `SETEX hold:seat:101 600 "user_99"`.
        - Simultaneously publish a delayed message to an SQS Delayed Queue or Kafka scheduled topic with a 10-minute delay.
        - When the timer fires, a background consumer checks if the seat was confirmed; if still `HELD`, it reverts the status to `AVAILABLE`.

??? example "Example"
    ```java
    // Optimistic locking entity in Spring Data JPA for seat reservations
    @Entity
    @Table(name = "seats")
    public class Seat {
        @Id
        private Long id;
        @Enumerated(EnumType.STRING)
        private SeatStatus status;
        private Instant holdExpiresAt;
        @Version
        private Long version;
    }
    ```

---

### 16. What is the difference between Cache Penetration, Cache Breakdown, and Cache Avalanche, and how do you protect a system against each?

Provide the exact technical mitigation pattern for each failure mode.

??? question "Reveal answer"
    - **1. Cache Penetration**:
      - *Failure*: Requests for keys that **never exist** in storage (e.g. negative IDs, random hacker scans) bypass cache and query the database every time.
      - *Mitigation*:
        1. **Bloom Filter**: In-memory probabilistic data structure positioned before the cache. If the Bloom filter says a key does not exist, return 404 immediately.
        2. **Cache Null Values**: Store a sentinel `null` or empty object in Redis with a short TTL ($30-60\text{s}$).
    - **2. Cache Breakdown (Stampede / Thundering Herd)**:
      - *Failure*: A **single, highly popular key** (e.g. Homepage Top Stories) expires while receiving 10,000 req/sec. Thousands of threads miss cache simultaneously and bombard the database.
      - *Mitigation*:
        1. **Mutex Lock (Distributed Lock)**: Only the first thread that misses the cache acquires a lock (Redisson / Spring `sync = true`) to query the DB and update the cache. Remaining threads wait.
        2. **Probabilistic Early Expiration (XFetch)**: Background worker refreshes the cache value asynchronously before its TTL expires.
    - **3. Cache Avalanche**:
      - *Failure*: **Millions of keys expire at the exact same second** because they were loaded with identical TTLs (e.g. at midnight). The database collapses under the sudden surge.
      - *Mitigation*:
        1. **TTL Jitter**: Add random variance to expiration times: `TTL = 3600 + rand(0, 300)` seconds.
        2. **Multi-Tier Caching**: Local in-memory cache (Caffeine) in front of distributed cache (Redis).

??? example "Example"
    ```java
    // Spring Cache with sync = true to prevent Cache Breakdown (Thundering Herd)
    @Service
    public class ProductCatalogService {
        @Cacheable(value = "products", key = "#id", sync = true)
        public ProductDTO getProduct(Long id) {
            return productRepository.findById(id).map(ProductDTO::from)
                .orElse(null);
        }
    }
    ```

---

### 24. How do Consistent Hashing algorithms (Ketama, Virtual Nodes) prevent catastrophic cache invalidation during node churn?

How do virtual nodes solve partition skew and nonuniform key distribution across cache clusters?

??? question "Reveal answer"
    - **Naive Modulo Hashing vs Consistent Hashing**:
      - *Naive Modulo* (`node = hash(key) % N`): Adding or removing a single node from an $N$-node cluster changes the divisor $N$. Nearly $100\%$ of all keys remap to new nodes, causing catastrophic cache misses and database collapse.
      - *Consistent Hashing*: Maps both nodes and keys to a fixed $360^\circ$ circular ring ($0$ to $2^{32}-1$). A key is assigned to the first node encountered moving clockwise. When a node is added or removed, only $\frac{K}{N}$ keys are remapped on average ($K = \text{total keys}$), preserving $> 80-90\%$ of cached data.
    - **Virtual Nodes (Vnodes) Mechanism**:
      - Without virtual nodes, physical servers hash to non-uniform positions on the ring, creating "hotspots" where one server owns a huge arc.
      - Each physical server is assigned 100–300 **Virtual Nodes** (e.g. `node1#1`, `node1#2`, `node1#3`).
      - This distributes the physical node's footprint uniformly across the ring, ensuring even memory distribution and balanced load shedding during failures.

??? example "Example"
    ```java
    // Consistent Hashing Ring with Virtual Nodes using TreeMap
    public class ConsistentHashRing<T> {
        private final NavigableMap<Integer, T> ring = new TreeMap<>();
        private final int numberOfReplicas;

        public ConsistentHashRing(int numberOfReplicas, List<T> nodes) {
            this.numberOfReplicas = numberOfReplicas;
            for (T node : nodes) {
                addNode(node);
            }
        }

        public void addNode(T node) {
            for (int i = 0; i < numberOfReplicas; i++) {
                int hash = hashFunction(node.toString() + "#" + i);
                ring.put(hash, node);
            }
        }

        public T get(String key) {
            if (ring.isEmpty()) return null;
            int hash = hashFunction(key);
            Map.Entry<Integer, T> entry = ring.ceilingEntry(hash);
            return (entry != null) ? entry.getValue() : ring.firstEntry().getValue();
        }
    }
    ```

---

### 25. How do you design an Asynchronous Job Processing System (e.g. video transcode, report generation) with backpressure and dead-letter handling?

Compare polling queues (Amazon SQS / RabbitMQ) with distributed stream processing (Apache Kafka) for long-running worker tasks.

??? question "Reveal answer"
    - **Queue-Based (SQS / RabbitMQ) vs Stream-Based (Kafka) for Long Jobs**:
      - *Kafka*: Designed for fast sequential streams. If a single video transcode job takes 20 minutes, consumer threads block, partition lag alarms fire, and subsequent messages in that partition are delayed.
      - *SQS / RabbitMQ*: Far superior for variable, long-running worker tasks. Each message is leased independently. Worker scale matches queue depth dynamically without partition lock-in.
    - **Production Architecture Components**:
      1. **Visibility Timeout & Heartbeat Extension**:
         - Set initial SQS visibility timeout to 5 minutes.
         - Running worker threads send heartbeat extensions (`ChangeMessageVisibility`) every 2 minutes while transcode progress continues.
      2. **Dead-Letter Queue (DLQ) & Bounded Retries**:
         - Set `maxReceiveCount = 3`. If a video file has corrupted frames crashing the worker, after 3 attempts it routes to the DLQ, unblocking the worker pool.
      3. **Worker Autoscaling via SQS Backlog**:
         - Scale worker pool based on `ApproximateNumberOfMessagesVisible / target_latency_seconds`.

??? example "Example"
    ```yaml
    # AWS SQS Queue with Redrive Policy (DLQ) for Long-Running Jobs
    resource "aws_sqs_queue" "job_queue" {
      name                       = "video-transcode-jobs"
      visibility_timeout_seconds = 300
      redrive_policy = jsonencode({
        deadLetterTargetArn = aws_sqs_queue.job_dlq.arn
        maxReceiveCount     = 3
      })
    }
    ```
<!-- --8<-- [end:intermediate] -->

---

<!-- --8<-- [start:senior] -->
## Senior Production Architecture (17–21, 26–28)

### 17. Design a High-Concurrency Flash Sale Inventory System capable of selling 10,000 units in 60 seconds with 100,000 requests/second peak traffic.

Why does relational row locking fail, and how do you implement in-memory pre-allocation with zero overselling?

??? question "Reveal answer"
    - **Why Relational Row Locking Collapses**:
      - Executing `SELECT stock FROM inventory WHERE item_id = 1 FOR UPDATE` forces the database to serialize 100,000 requests through a single row lock.
      - Maximum single-row write throughput on PostgreSQL is $\approx 500\text{ transactions/sec}$. The remaining 99,500 requests block in lock queues, saturating connection pools and crashing the database in $< 2\text{seconds}$.
    - **The 3-Tier Production Architecture**:
      1. *Tier 1: In-Memory Pre-Allocation (Redis)*:
         - Before the sale, initialize `SET item:101:stock 10000` in Redis.
         - Incoming requests execute an atomic Redis Lua script that checks user purchase history (`SISMEMBER`) and decrements stock (`DECRBY`).
         - Redis processes single-threaded operations in memory in $< 1\text{ms}$, effortlessly handling 100,000 req/sec. Requests arriving after stock hits 0 are immediately rejected with HTTP 410 Sold Out.
      2. *Tier 2: Asynchronous Queue Buffering (Kafka)*:
         - Successful reservations are pushed to a Kafka topic `order-reservations`. The user receives HTTP 202 Accepted: "Order Queued".
      3. *Tier 3: Rate-Limited Database Persistence*:
         - Downstream consumer workers pull from Kafka at a smooth, sustainable rate (e.g. 500 writes/sec), generating orders and updating PostgreSQL without lock contention.

??? example "Example"
    ```lua
    -- Atomic Flash Sale Redis Lua Reservation Script
    local stock_key = KEYS[1]
    local user_set_key = KEYS[2]
    local user_id = ARGV[1]
    local quantity = tonumber(ARGV[2])

    if redis.call('SISMEMBER', user_set_key, user_id) == 1 then
        return -1 -- User already purchased
    end

    local current_stock = tonumber(redis.call('GET', stock_key) or '0')
    if current_stock >= quantity then
        redis.call('DECRBY', stock_key, quantity)
        redis.call('SADD', user_set_key, user_id)
        return 1 -- Reservation success
    else
        return 0 -- Sold out
    end
    ```

---

### 18. How do you design an Immutable Double-Entry Bookkeeping Ledger for a financial fintech platform?

How do you guarantee that money cannot be created or destroyed, and how do you prevent floating-point rounding errors?

??? question "Reveal answer"
    - **1. Integer Minor Units (No Floating Point)**:
      - Never use `FLOAT` or `DOUBLE` for monetary calculations.
      - Store currency in **minor integer units** (cents for USD/EUR, integer satoshis for BTC) using `BIGINT` in SQL and `Long` or `BigDecimal` in Java.
    - **2. The Double-Entry Invariant**:
      - Money is never updated in-place; every transaction creates at least two immutable balancing ledger lines.
      - **Debits must equal Credits**:
        $$\sum \text{Debits} \equiv \sum \text{Credits}$$
      - Example: Customer deposits $100 cash:
        - Debit: `Assets:Cash` (\$100.00 / 10,000 cents)
        - Credit: `Liabilities:CustomerDeposit` (\$100.00 / 10,000 cents)
    - **3. Append-Only Immutability**:
      - Ledger tables permit only `INSERT` statements; `UPDATE` and `DELETE` operations are strictly revoked at the database user permission level.
      - Corrections are executed by inserting compensating reversing entries.
    - **4. Concurrency & Serializability**:
      - Enforce `SERIALIZABLE` isolation or account-level optimistic locking on account balance derivation to eliminate write skew.

??? example "Example"
    ```java
    // Java record representing an immutable financial ledger posting
    public record LedgerEntry(
        UUID id,
        UUID transactionId,
        UUID accountId,
        EntryType type,      // DEBIT or CREDIT
        long amountCents,    // Minor currency units (e.g. 10000 = $100.00)
        String currency,     // ISO-4217 code (USD)
        Instant timestamp
    ) {
        public LedgerEntry {
            if (amountCents <= 0) {
                throw new IllegalArgumentException("Ledger amount must be strictly positive");
            }
        }
    }
    ```

---

### 19. Compare Saga Orchestration versus Saga Choreography for multi-service distributed workflows.

When does Choreography become an unmaintainable anti-pattern ("Pinball Architecture")?

??? question "Reveal answer"
    - **Saga Choreography (Event-Driven)**:
      - *Mechanism*: Services publish domain events to a shared message broker. Other services listen to those events and execute local transactions without centralized control.
      - *Pros*: Decentralized; loose coupling; ideal for simple 2–3 step workflows.
      - *When it Becomes an Anti-Pattern ("Pinball Machine")*:
        - As workflows grow to 5+ services with conditional branching and complex compensation logic, understanding system state becomes impossible.
        - Events bounce unpredictably between services like a pinball machine.
        - Cyclic event dependencies and circular compensation loops emerge; debugging production failures requires tracing across dozens of asynchronous event topics.
    - **Saga Orchestration (Centralized State Machine)**:
      - *Mechanism*: A dedicated orchestrator service (implemented via Temporal, AWS Step Functions, or custom Spring Boot state machine) sends explicit command messages to participant services and awaits responses.
      - *Pros*:
        - **Centralized Visibility**: The complete workflow state, progress, and failure history is observable in a single state machine.
        - **Deterministic Compensations**: Rollback logic is explicitly encoded in one place.
        - Avoids circular dependency graphs.

??? example "Example"
    ```mermaid
    flowchart TD
        subgraph OrchestrationApproach ["Saga Orchestration (Recommended for Complex Workflows)"]
            Orch["Order Saga Orchestrator<br/>(State Machine)"]
            Orch -->|Command: Reserve| Inv["Inventory Service"]
            Orch -->|Command: Charge| Pay["Payment Service"]
            Orch -->|Command: Ship| Ship["Shipping Service"]
        end
    ```

---

### 20. How do you design a Multi-Tenant Database Architecture for an enterprise B2B SaaS platform?

Compare Shared Database / Shared Schema (Tenant Discriminator) versus Database-per-Tenant in terms of security, scalability, and cost.

??? question "Reveal answer"
    - **1. Shared Database, Shared Schema (Tenant Column / Discriminator)**:
      - *Architecture*: All tenants share the same database tables. Every query filters by `tenant_id` (e.g. `WHERE tenant_id = 'acme'`).
      - *Pros*: Lowest infrastructure cost; easiest schema migration management; effortless resource pooling.
      - *Cons*: **High risk of cross-tenant data leakage** if a developer forgets a `tenant_id` filter; noisy-neighbor performance impact; cannot restore backup for a single tenant.
      - *Senior Mitigation*: Enforce PostgreSQL **Row-Level Security (RLS)** or Hibernate multi-tenant filters automatically based on thread context.
    - **2. Separate Database per Tenant**:
      - *Architecture*: Every customer tenant receives an isolated relational database instance or database catalog.
      - *Pros*: Absolute physical data isolation (satisfies banking/healthcare compliance); independent backup/restore; zero noisy-neighbor performance cross-talk.
      - *Cons*: High infrastructure cost; managing schema migrations across 5,000 separate databases is an operational burden; connection pool overhead.
    - **Hybrid Tiering Model**:
      - Free / Starter tier: Shared Database with Row-Level Security.
      - Enterprise VIP tier: Dedicated Database per Tenant billed at premium pricing.

??? example "Example"
    ```sql
    -- PostgreSQL Row-Level Security (RLS) guaranteeing multi-tenant isolation
    ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

    CREATE POLICY tenant_isolation_policy ON orders
        FOR ALL
        USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
    ```

---

### 21. How do you design an active-passive cross-region disaster recovery architecture for an e-commerce platform on AWS?

Explain data replication, Route 53 health check failovers, and Recovery Point Objective (RPO) vs Recovery Time Objective (RTO).

??? question "Reveal answer"
    - **Active-Passive Architecture Overview**:
      - **Primary Region (`us-east-1`)**: Serves 100% of live production traffic.
      - **Secondary Region (`us-west-2`)**: Warm standby running minimal compute capacity with continuous asynchronous storage replication.
    - **Data Replication Mechanics**:
      - *Relational DB*: Amazon Aurora Global Database replicates storage blocks across regions in $< 1\text{second}$ without impacting primary write throughput.
      - *Object Storage*: Amazon S3 Cross-Region Replication (CRR) replicates uploaded media assets.
      - *Cache*: Redis keys are reconstructed on demand (or replicated via AWS Global Datastore).
    - **RPO and RTO Targets**:
      - **Recovery Point Objective (RPO)**: Maximum acceptable data loss duration. Under Aurora Global DB, $RPO < 1\text{second}$ (asynchronous replication lag).
      - **Recovery Time Objective (RTO)**: Time required to complete failover and restore full service. $RTO = 3 - 8\text{minutes}$ (Route 53 DNS TTL propagation + promoting secondary Aurora cluster to primary writer + scaling up ECS tasks).
    - **Route 53 DNS Failover**:
      - Route 53 health checks monitor primary ALB endpoint (`/actuator/health/readiness`).
      - If 3 consecutive checks fail, Route 53 flips DNS records to point to the secondary region ALB.

??? example "Example"
    ```mermaid
    flowchart LR
        Users(["Global Clients"]) --> R53["Route 53 DNS (Health Check Failover)"]
        
        subgraph Primary ["Primary: us-east-1 (Active)"]
            ALB1["ALB"] --> App1["ECS Tasks (100% Scale)"]
            App1 --> Aurora1[("Aurora Writer")]
        end

        subgraph Secondary ["Secondary: us-west-2 (Standby)"]
            ALB2["ALB"] --> App2["ECS Tasks (Autoscaling Standby)"]
            App2 --> Aurora2[("Aurora Global DB (Read Only)")]
        end

        R53 -->|Active Traffic| ALB1
        R53 -.->|Failover on Outage| ALB2
        Aurora1 -.->|Async Replication < 1s lag| Aurora2
    ```

---

### 26. How do you design a Global Distributed Rate Limiter operating across multi-region microservices?

Compare centralized Redis Token Bucket algorithms with local token batch leases and eventual consistency.

??? question "Reveal answer"
    - **Centralized Redis vs Local Leases Trade-off**:
      - *Centralized Multi-Region Redis*: Every HTTP request in `ap-southeast-1` queries a Redis cluster in `us-east-1` to decrement tokens. Cross-ocean latency ($150-200\text{ms}$) destroys API performance.
      - *Local Batch Leasing Architecture*:
        1. A centralized token authority grants local regional instances a "batch lease" of tokens (e.g. 5,000 tokens for the next 10 seconds).
        2. Local gateways decrement tokens locally in in-memory memory buffers (Caffeine / local Redis) with sub-millisecond latency.
        3. Background asynchronous heartbeats periodically sync consumption back to the central authority and request additional token allotments.
    - **Token Bucket Algorithm via Redis Lua**:
      - Evaluates capacity $C$, fill rate $R$, last updated timestamp $T_{\text{last}}$, and current timestamp $T_{\text{now}}$.
      - Automatically refills tokens: $\text{tokens} = \min(C, \text{tokens} + (T_{\text{now}} - T_{\text{last}}) \times R)$.

??? example "Example"
    ```lua
    -- Atomic Token Bucket implementation in Redis Lua
    local key = KEYS[1]
    local limit = tonumber(ARGV[1])
    local current = tonumber(redis.call('get', key) or "0")
    if current + 1 > limit then
        return 0 -- Throttled
    else
        redis.call("INCRBY", key, 1)
        if current == 0 then
            redis.call("EXPIRE", key, 1)
        end
        return 1 -- Allowed
    end
    ```

---

### 27. How do you design a Real-Time Collaborative Document Editing System (Google Docs / Figma)?

Compare Operational Transformation (OT) with Conflict-free Replicated Data Types (CRDTs).

??? question "Reveal answer"
    - **Operational Transformation (OT)**:
      - *Mechanism*: Edits are represented as position-based operations: `Insert(pos, char)`, `Delete(pos)`.
      - *Architecture*: Relies on a centralized server to order operations and transform concurrent client offsets so all clients converge.
      - *Pros/Cons*: Used historically by Google Docs; computationally complex transformation matrices ($N \times M$ combinations) that are notoriously hard to prove correct mathematically.
    - **Conflict-free Replicated Data Types (CRDTs)**:
      - *Mechanism*: Every character or element is assigned a globally unique, immutable, fractional identifier (e.g. LSEQ or RGA tree structures).
      - *Architecture*: Decentralized, peer-to-peer friendly. Operations commute naturally: applying edits in any order on any replica produces the exact same document state without a central server.
      - *Pros/Cons*: Used by Figma and modern collaborative tools; memory overhead for storing unique character IDs and tombstones for deleted characters.

??? example "Example"
    ```json
    // CRDT fractional indexing for concurrent text insertions
    [
      { "id": "0.1", "char": "H", "author": "user_1" },
      { "id": "0.15", "char": "i", "author": "user_2" },
      { "id": "0.2", "char": "!", "author": "user_1" }
    ]
    ```

---

### 28. How do you design an Order Matching Engine (Cryptocurrency / Stock Exchange) for sub-millisecond execution?

Explain lock-free ring buffers (LMAX Disruptor), in-memory order books, and deterministic event journaling.

??? question "Reveal answer"
    - **Why Traditional Web Architectures Fail**:
      - Relational databases, Spring Boot thread pools, and distributed network hops introduce microsecond-level jitter, lock contention, and Garbage Collection pauses that cause order matching queues to back up.
    - **High-Performance Matching Engine Blueprint**:
      1. **LMAX Disruptor Lock-Free Ring Buffer**: Single-writer principle. Orders are placed into a pre-allocated circular ring buffer sequenced by an atomic 64-bit sequence counter. A dedicated CPU-pinned single thread executes matching logic without any mutexes or context switching ($> 6\text{ million operations/sec}$).
      2. **In-Memory B-Tree / Red-Black Tree Order Books**: Bids (highest first) and Asks (lowest first) are maintained entirely in memory. Matching executes in $O(1)$ to $O(\log N)$ memory accesses.
      3. **Deterministic Sequential Journaling**: Every incoming order is appended to an append-only sequential disk log (Chronicle Queue / NVMe memory-mapped files) *before* matching. If the engine crashes, state is deterministically rebuilt by replaying the journal from the last snapshot.

??? example "Example"
    ```java
    // Pre-allocated order event in LMAX Disruptor ring buffer
    public class OrderEvent {
        private long orderId;
        private long price;
        private int quantity;
        private OrderSide side;

        public void setValues(long orderId, long price, int quantity, OrderSide side) {
            this.orderId = orderId;
            this.price = price;
            this.quantity = quantity;
            this.side = side;
        }
    }
    ```
<!-- --8<-- [end:senior] -->

---

<!-- --8<-- [start:scenarios] -->
## Production Incident Scenarios (22–23, 29–30)

### 22. Production Incident: During a flash sale event, an e-commerce platform collapses within 30 seconds of launch. The database CPU hits 100%, and all application instances exhaust their HikariCP connection pools. Investigation shows thousands of threads blocked on `SELECT ... FOR UPDATE` on the product inventory row. How do you triage the live incident and redesign the system?

Walk through the operational triage, root cause analysis, and permanent multi-tier architecture.

??? question "Reveal answer"
    - **Immediate Incident Triage**:
      1. *Emergency Ingress Shedding*: Configure API Gateway / CloudFront to return immediate HTTP 503 or static queue waiting room pages for the flash sale item, stopping the flood of requests to the database.
      2. *Kill Blocked Database Locks*: Run `pg_terminate_backend(pid)` on PostgreSQL to terminate all active `SELECT ... FOR UPDATE` backend worker connections and unblock the connection pool.
      3. *Restart Application Instances*: Reset HikariCP pools across Spring Boot instances to restore normal operations for non-flash-sale customer traffic.
    - **Root Cause Analysis**:
      - The application used pessimistic database locking (`SELECT ... FOR UPDATE`) on a single database row under 50,000 req/sec.
      - PostgreSQL processes row locks serially. Because each transaction held the lock for 5–10ms, throughput capped at $< 200\text{ req/sec}$.
      - The remaining 49,800 requests accumulated in the PostgreSQL lock table, driving CPU to 100% on context switching and locking all Hikari connections.
    - **Permanent Architectural Redesign**:
      1. **Shift Concurrency to Memory (Redis Lua)**:
         - Pre-allocate the inventory count into Redis. Use an atomic Lua script (`DECRBY`) to reserve stock in $< 1\text{ms}$.
      2. **Asynchronous Order Generation (Kafka)**:
         - Successful reservations are published to Kafka.
         - Downstream workers write orders to the database at a controlled, sustainable rate (e.g. 300 orders/sec).
      3. **Optimistic Locking with Inventory Bucketing**:
         - If database reservation is required, split the inventory into 20 discrete inventory buckets (`item_101_bucket_1` to `item_101_bucket_20`), multiplying single-row write throughput by $20\times$.

??? example "Example"
    ```java
    // Permanent fix: Pre-allocating in Redis via atomic Lua execution
    public boolean reserveFlashSaleItem(String userId, Long productId, int quantity) {
        Long result = redisTemplate.execute(
            flashSaleScript,
            List.of("stock:" + productId, "buyers:" + productId),
            userId,
            String.valueOf(quantity)
        );
        return result != null && result == 1L;
    }
    ```

---

### 23. Production Incident: A customer's internet connection drops while checking out a $1,200 flight ticket. The mobile app automatically retries the payment request 3 times. The customer is charged $3,600 on their credit card statement, triggering an urgent fraud escalation. Why did this occur and how do you architect end-to-end idempotency?

Explain the idempotency key lifecycle from mobile client through API Gateway down to the payment processor.

??? question "Reveal answer"
    - **Root Cause Analysis**:
      - The payment endpoint was **non-idempotent**: each HTTP POST generated a fresh transaction ID and issued a new authorization request to the banking gateway.
      - When the client's network dropped after 2 seconds, the server had already processed the charge and committed it, but the client never received the HTTP 200 response.
      - The client app retried the request with the same payload, which the server treated as 2 additional distinct transactions.
    - **End-to-End Idempotency Architecture**:
      1. *Client Generation of Idempotency Key*:
         - When the user taps "Pay Now", the client generates a unique UUID `Idempotency-Key` header (e.g. `Idempotency-Key: 9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d`) and stores it locally on the device.
      2. *API Gateway / Payment Service Dedup Table*:
         - Before executing any payment logic, the service checks an `idempotency_records` table:
           ```sql
           INSERT INTO idempotency_keys (key, user_id, status, created_at)
           VALUES (:key, :userId, 'STARTED', NOW())
           ON CONFLICT (key) DO NOTHING;
           ```
         - If the insert fails (key already exists), the service inspects the existing record:
           - If status is `SUCCESS`, it immediately returns the **cached previous HTTP response** without re-executing the charge.
           - If status is `STARTED`, it returns HTTP 409 Conflict: "Transaction in Progress".
      3. *Gateway Propagation*:
         - The internal payment service forwards the exact same `Idempotency-Key` to Stripe (`Idempotency-Key: 9b1deb4d...`). Stripe guarantees at the banking network layer that identical keys within 24 hours will never create duplicate credit card authorizations.

??? example "Example"
    ```java
    // Idempotent Payment Controller in Spring Boot
    @PostMapping("/api/payments/charge")
    public ResponseEntity<PaymentResponse> charge(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody @Valid ChargeRequest request) {
        
        Optional<PaymentResponse> cachedResponse = idempotencyService.findResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            return ResponseEntity.ok(cachedResponse.get());
        }

        PaymentResponse response = paymentService.executeChargeWithIdempotency(idempotencyKey, request);
        idempotencyService.saveResponse(idempotencyKey, response);
        return ResponseEntity.ok(response);
    }
    ```

---

### 29. Production Incident: A celebrity user with 10 million followers posts a message, causing a massive write fan-out stampede that exhausts Redis memory and freezes timeline feeds for millions of users. What happened and how do you resolve it?

Explain Write Fan-Out (Push) vs Read Fan-Out (Pull), hybrid timeline generation, and social network caching architectures.

??? question "Reveal answer"
    - **Incident Walkthrough**:
      - A social platform used **Write Fan-Out (Push Model)**: when user $A$ posts, a background worker pushes the post ID into the Redis timeline sorted set (`ZADD timeline:<follower_id>`) of *every* follower.
      - A pop star with 10 million followers posted a concert announcement.
      - Background workers attempted 10,000,000 Redis write operations simultaneously.
      - Redis connection queues saturated, memory spiked past max limits, and millions of regular user timeline writes were delayed by $> 30\text{ minutes}$.
    - **Root Cause Analysis**:
      - Pure Push (Write Fan-Out) exhibits $O(F)$ write complexity where $F$ is follower count. For high-degree nodes ("celebrities"), $F > 1,000,000$, creating uncontrollable write amplification.
    - **Permanent Architectural Fix (Hybrid Push-Pull Model)**:
      1. **Follower Threshold Partitioning**:
         - Standard users ($< 25,000$ followers): Use **Push Model**. When they post, fan-out workers write to followers' Redis timelines ($O(F)$ is trivial).
         - Celebrity users ($\ge 25,000$ followers): Use **Pull Model**. When a celebrity posts, their post is written *only once* to their personal celebrity outbox timeline (`celebrity_posts:<user_id>`). Zero fan-out writes are performed!
      2. **Read-Time Dynamic Timeline Merge**:
         - When a follower opens their feed, the service reads the follower's personal push timeline from Redis, fetches the recent posts from the 5 celebrities they follow, and executes a $K$-way merge sort in memory ($< 5\text{ms}$).

??? example "Example"
    ```java
    // Hybrid timeline feed merger combining local push timeline with celebrity pull outboxes
    public List<PostDTO> getFeed(Long userId) {
        List<PostDTO> standardFeed = redisFeedStore.getTimeline(userId, 0, 50);
        List<Long> followedCelebrities = celebrityFollowStore.getCelebritiesFor(userId);
        
        List<PostDTO> celebrityPosts = celebrityPostStore.getRecentPosts(followedCelebrities, 20);
        
        // Merge and sort in memory by timestamp
        return Stream.concat(standardFeed.stream(), celebrityPosts.stream())
            .sorted(Comparator.comparing(PostDTO::createdAt).reversed())
            .limit(50)
            .toList();
    }
    ```

---

### 30. Production Incident: A multi-region deployment of a distributed shopping cart cluster experiences cross-region split-brain, causing conflicting item quantities and lost cart updates. What happened and how do you resolve it?

Explain vector clocks, Last-Write-Wins (LWW) clock skew vulnerabilities, and conflict resolution policies.

??? question "Reveal answer"
    - **Incident Walkthrough**:
      - A shopping cart service deployed active-active across `us-east-1` and `eu-west-1` using asynchronous multi-master replication with Last-Write-Wins (LWW) conflict resolution based on system timestamps.
      - A transatlantic fiber cut caused a 4-minute network partition between the regions.
      - A traveling user updated their cart in Europe (added Item $X$ at 14:00:01 UTC) and immediately switched mobile networks, updating their cart via US servers (added Item $Y$ at 14:00:02 UTC according to US system time).
      - Due to NTP server clock skew (US clock was lagging by 3 seconds), the US timestamp was recorded as 13:59:59 UTC.
      - When the network partition healed, the LWW resolver compared timestamps, chose the European update, and completely discarded the US update, causing Item $Y$ to vanish from the user's cart.
    - **Root Cause Analysis**:
      - Physical wall-clock timestamps are non-monotonic across distributed servers due to NTP drift, leap seconds, and relativistic clock skew. LWW silently drops valid concurrent updates.
    - **Permanent Architectural Fix**:
      1. **Adopt Vector Clocks / Lamport Version Trees**:
         - Instead of relying on physical clock timestamps, track causal relationships using Vector Clocks: $V = [(\text{Region}_A, c_1), (\text{Region}_B, c_2)]$.
      2. **Deterministic Conflict Resolution (CRDTs)**:
         - Model the shopping cart as an Observed-Remove Set (OR-Set) or Positive-Negative Counter (PN-Counter).
         - When regions reconcile, cart contents are mathematically merged via set union: $\text{Cart}_{\text{final}} = \text{Cart}_{\text{US}} \cup \text{Cart}_{\text{EU}}$, preserving all items added in both regions.

??? example "Example"
    ```json
    // Reconciled CRDT Shopping Cart state merging concurrent additions
    {
      "cart_id": "cart_888",
      "items": {
        "SKU_LAPTOP": { "qty": 1, "added_by": "us-east-1", "epoch": 101 },
        "SKU_MOUSE": { "qty": 1, "added_by": "eu-west-1", "epoch": 102 }
      },
      "vector_clock": {
        "us-east-1": 4,
        "eu-west-1": 3
      }
    }
    ```
<!-- --8<-- [end:scenarios] -->
