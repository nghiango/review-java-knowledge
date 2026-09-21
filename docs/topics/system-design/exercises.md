# System Design Hands-On Exercises

Comprehensive architectural blueprints and step-by-step engineering exercises to practice designing scalable, resilient, and fault-tolerant distributed systems.

---

## Exercise 1: Design a Distributed URL Shortener (Bitly-Scale)

### Objective
Design a horizontally scalable URL shortening service capable of handling 100 million new URLs per month and 10 billion redirects per month with sub-10ms redirect latency.

### Functional & Non-Functional Requirements
- **Functional**:
  - `POST /api/v1/urls`: Accepts a long URL and optional custom alias; returns a 7-character short URL (`https://tiny.ly/7bX9q1Z`).
  - `GET /{shortCode}`: Redirects the user to the original long URL with minimum latency.
  - Expiration: URLs expire after a user-configured TTL (default 2 years).
  - Analytics: Track total clicks, country of origin, and referrers asynchronously.
- **Non-Functional**:
  - High availability ($99.99\%$). Read latency is prioritized over write latency.
  - Read-to-write ratio: $100:1$ ($10\text{B reads} / 100\text{M writes}$).
  - Short links must be unpredictable and immune to sequence enumeration scraping.

### Step-by-Step Architectural Solution

??? question "View solution"
    #### 1. Capacity & Storage Calculations
    - **Write QPS**: $\frac{100\text{M}}{30 \times 86,400} \approx 40\text{ writes/sec}$.
    - **Read QPS**: $40 \times 100 \approx 4,000\text{ reads/sec}$ (Peak: $10,000\text{ reads/sec}$).
    - **Storage**: 7-character Base62 string + long URL ($500\text{ bytes}$) + metadata $\approx 1\text{ KB}$ per entry.
      $$100\text{M} \times 1\text{ KB} = 100\text{ GB/month} \implies 1.2\text{ TB/year}$$
    - **Cache**: 80/20 rule: Cache top 20% of daily active URLs in Redis:
      $$10\text{B / 30} = 333\text{M requests/day} \times 20\% \times 1\text{ KB} \approx 66\text{ GB RAM}$$

    #### 2. Short Code Generation Strategy: Base62 vs Hashing
    - **Option A (Hashing with MD5/SHA-256)**: Hashing the long URL produces a 128-bit hash. Taking the first 7 characters requires collision detection in the database, leading to high write latencies.
    - **Option B (Base62 Encoding of 64-bit Integer ID - Recommended)**:
      - A 7-character string using characters $[0\text{-}9, a\text{-}z, A\text{-}Z]$ yields $62^7 \approx 3.52\text{ trillion}$ unique combinations.
      - Convert a unique 64-bit integer ID directly into a Base62 string:
        $$\text{ID } 125 \implies \text{"cb"}$$
    
    #### 3. Distributed ID Generation (Range Allocator)
    - To prevent a single database auto-increment bottleneck or sequence enumeration:
      - Use a central coordinator (e.g., ZooKeeper or central Redis) that hands out ranges of 1,000,000 IDs to each application server instance (Server A gets $1\text{--}1,000,000$; Server B gets $1,000,001\text{--}2,000,000$).
      - Each server generates unique IDs locally in memory without distributed locks.
      - Add a random shuffle/feistel cipher layer to the integer before Base62 encoding to eliminate predictable sequential enumeration.

    #### 4. High-Performance Redirect Flow
    ```mermaid
    sequenceDiagram
        autonumber
        actor User as Client Browser
        participant CDN as Cloudflare Edge CDN
        participant GW as API Gateway
        participant Cache as Redis Cluster
        participant DB as PostgreSQL (Read Replicas)
        participant Kafka as Analytics Kafka Topic

        User->>CDN: GET /7bX9q1Z
        alt CDN Cache Hit
            CDN-->>User: 301 / 302 Redirect to Long URL
        else CDN Cache Miss
            CDN->>GW: Route request
            GW->>Cache: GET url:7bX9q1Z
            alt Redis Hit
                Cache-->>GW: Original URL
            else Redis Miss
                GW->>DB: SELECT long_url FROM urls WHERE code = '7bX9q1Z'
                DB-->>GW: Original URL
                GW->>Cache: SET url:7bX9q1Z with TTL
            end
            GW-->>CDN: Return Long URL
            CDN-->>User: 302 Found (Location: https://example.com/...)
            GW-)Kafka: Produce ClickEvent (asynchronous)
        end
    ```

    #### 5. HTTP 301 vs 302 Redirect Trade-off
    - **301 Moved Permanently**: Browser caches the redirect locally. Subsequent clicks bypass the URL shortener entirely. Reduces server load to near zero, but destroys real-time click analytics.
    - **302 Found (Temporary Redirect)**: Browser always queries the URL shortener. Enables accurate analytics and tracking at the cost of slightly higher server traffic. In production systems, **HTTP 302** is preferred for analytics integrity.

---

## Exercise 2: Design a Seat Reservation & Ticketing System with Hold Timers

### Objective
Design an event ticketing platform (e.g., Ticketmaster) where users can temporarily hold seats for 10 minutes during checkout, ensuring seats are never double-booked and automatically expire if payment is not completed.

### Functional & Non-Functional Requirements
- **Functional**:
  - View real-time seating map with seat statuses: `AVAILABLE`, `HELD`, `BOOKED`.
  - Hold seat: When a user selects a seat, hold it for exactly 10 minutes.
  - Complete checkout: Process payment and transition seat to `BOOKED`.
  - Auto-release: If 10 minutes expire without payment, release seat to `AVAILABLE`.
- **Non-Functional**:
  - Zero double-booking: Strictly impossible for two users to hold or book the same seat.
  - High concurrency: 20,000 fans selecting seats simultaneously during venue release.

### Step-by-Step Architectural Solution

??? question "View solution"
    #### 1. State Machine
    ```mermaid
    stateDiagram-v2
        [*] --> AVAILABLE
        AVAILABLE --> HELD: User selects seat (10-min TTL)
        HELD --> BOOKED: Payment Succeeded
        HELD --> AVAILABLE: 10-Min Timer Expired OR User Aborted
        BOOKED --> [*]
    ```

    #### 2. Concurrency Control with Redis Atomic Operations
    Using a relational database with pessimistic row locks (`FOR UPDATE`) on popular venue seats creates massive deadlocks and connection pool exhaustion. Instead, use Redis as the primary reservation barrier:
    
    ```lua
    -- hold_seat.lua: Keys: seat:{venueId}:{seatId}, Arg1: userId, Arg2: holdTTLSeconds (600)
    local seatKey = KEYS[1]
    local userId = ARGV[1]
    local ttl = tonumber(ARGV[2])

    -- Check if seat is already held or booked
    local status = redis.call('HGET', seatKey, 'status')
    if status == nil or status == 'AVAILABLE' then
        redis.call('HSET', seatKey, 'status', 'HELD', 'heldBy', userId)
        redis.call('EXPIRE', seatKey, ttl)
        return 1 -- Hold Acquired
    else
        return 0 -- Seat already held or booked
    end
    ```

    #### 3. Hold Expiration Strategies
    - **Anti-Pattern (Redis Key Expiration Notification)**: Relying solely on `__keyevent@0__:expired` pub/sub is dangerous because Redis pub/sub is "fire-and-forget" with no delivery guarantees.
    - **Production-Grade Solution (Delayed Message Queue)**:
      - When the seat is held, publish a delayed message to RabbitMQ (Dead Letter Exchange TTL) or Kafka with a scheduled timestamp $T + 10\text{ minutes}$.
      - When the hold consumer receives the message after 10 minutes, it queries the database: if the seat order is still in `PENDING_PAYMENT` state, it marks the order `EXPIRED` and atomically resets the Redis seat status to `AVAILABLE`.

---

## Exercise 3: Design an End-to-End Idempotency Layer for Payment APIs

### Objective
Design a production-grade idempotency filter and distributed locking mechanism in Spring Boot that prevents duplicate billing from network retries, browser double-clicks, and webhook replays.

### Requirements
- Every mutating request (`POST /api/v1/payments`) requires an `Idempotency-Key` HTTP header.
- Concurrent requests with the same key must return `HTTP 409 Conflict` or block safely.
- Subsequent requests with the same key must return the exact cached response body, status code, and headers without re-executing business logic.

### Step-by-Step Architectural Solution

??? question "View solution"
    #### 1. Idempotency Key Lifecycle & Storage Schema
    Store idempotency records in PostgreSQL with a Redis fast path:
    ```sql
    CREATE TABLE idempotency_keys (
        key VARCHAR(128) PRIMARY KEY,
        user_id BIGINT NOT NULL,
        request_hash VARCHAR(64) NOT NULL, -- SHA-256 of request payload to detect payload mutation
        status VARCHAR(32) NOT NULL,        -- 'PROCESSING', 'COMPLETED', 'FAILED'
        response_code INT,
        response_body JSONB,
        created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
        expires_at TIMESTAMPTZ NOT NULL
    );
    CREATE INDEX idx_idempotency_expiry ON idempotency_keys (expires_at);
    ```

    #### 2. Workflow Sequence
    ```mermaid
    sequenceDiagram
        autonumber
        actor Client
        participant Filter as IdempotencyFilter
        participant Redis as Redis Lock
        participant DB as Idempotency DB Table
        participant Core as Payment Service

        Client->>Filter: POST /payments (Idempotency-Key: K1)
        Filter->>Redis: SET lock:K1 NX EX 30
        alt Lock Failed (Concurrent Duplicate Request)
            Filter-->>Client: 409 Conflict (Request already in progress)
        else Lock Acquired
            Filter->>DB: SELECT * FROM idempotency_keys WHERE key = 'K1'
            alt Key Exists & Status == 'COMPLETED'
                Filter->>Redis: DEL lock:K1
                Filter-->>Client: Return cached status code & response_body
            else Key Does Not Exist
                Filter->>DB: INSERT INTO idempotency_keys (key, status) VALUES ('K1', 'PROCESSING')
                Filter->>Core: Execute Payment & Third-Party Gateway Call
                Core-->>Filter: Payment Result (201 Created, {txId: ...})
                Filter->>DB: UPDATE idempotency_keys SET status = 'COMPLETED', response_code = 201, response_body = ... WHERE key = 'K1'
                Filter->>Redis: DEL lock:K1
                Filter-->>Client: 201 Created (with original payload)
            end
        end
    ```

    #### 3. Payload Mutation Validation
    If a client sends an existing `Idempotency-Key` with a *different* request payload (e.g., different amount or currency), the system must reject the request with `HTTP 422 Unprocessable Entity` or `HTTP 400 Bad Request` rather than silently returning the cached response of the earlier different request.
