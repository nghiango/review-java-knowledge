# Caching and Redis in Production

Production operations, memory sizing, latency diagnostics, big-key detection, and disaster avoidance for Redis and Spring Cache.

---

## 1. Redis Memory Sizing & Maxmemory Configuration

Running Redis in production without explicit memory limits risks operating system Out-Of-Memory (OOM) killer terminating the Redis process.

### Sizing Guidelines
Always set `maxmemory` to **70–80% of total host RAM**:
```text
# redis.conf
maxmemory 6gb
maxmemory-policy allkeys-lru
```

### Why Reserve 20–30% RAM?
1. **BGSAVE Fork Overhead**: When creating RDB snapshots or rewriting AOF logs, Redis forks a child process. Linux Copy-On-Write (COW) duplicates memory pages modified by active writes during the fork. If host memory is 100% committed, the fork fails with `Cannot allocate memory`.
2. **Replication Output Buffers**: In master-replica setups, high write rates accumulate in master client output buffers (`client-output-buffer-limit`) while replicating to slaves.
3. **Internal Fragmentation**: Frequent memory allocation and deallocation causes heap fragmentation (`mem_fragmentation_ratio > 1.5`).

---

## 2. Diagnosing Latency Spikes and Big Keys

Because Redis is single-threaded, any command taking more than a few milliseconds blocks all other clients.

### 1. Slow Command Logging (`SLOWLOG`)
Inspect queries that exceed execution thresholds (default 10,000 microseconds = 10ms):
```bash
redis-cli slowlog get 10
```
Common culprits:
- `KEYS *`: Scans entire keyspace in $O(N)$ time. **Banned in production** (use `SCAN` instead).
- `HGETALL` or `SMEMBERS` on collections containing >10,000 elements.
- Huge JSON payloads being serialized/deserialized synchronously.

### 2. Scanning for Big Keys (`--bigkeys`)
Find oversized strings, hashes, or sets consuming disproportionate memory:
```bash
redis-cli -h <host> -p <port> --bigkeys
```
- Strings > 100KB or collections with > 5,000 elements should be chunked, paginated, or offloaded to object storage (S3).

---

## 3. Lettuce Connection Configuration in Spring Boot

In high-throughput microservices, tune Lettuce connection pool and TCP socket timeouts in `application.yml`:

```yaml
spring:
  data:
    redis:
      host: redis-master.production.local
      port: 6379
      timeout: 2000ms          # Socket read timeout (2 seconds)
      connect-timeout: 1000ms  # Connection establishment timeout
      lettuce:
        pool:
          max-active: 32       # Max concurrent active connections for blocking commands
          max-idle: 16
          min-idle: 8
          max-wait: 1000ms
        shutdown-timeout: 200ms
```

---

## 4. Troubleshooting Production Incidents

| Incident Symptom | Root Cause | Production Remediation |
|---|---|---|
| `OOM command not allowed when used memory > 'maxmemory'` | Default `noeviction` policy + keys stored without TTL | Change policy to `allkeys-lru` or enforce mandatory TTLs on all writes |
| Sudden latency spikes to >500ms every hour | Large RDB `BGSAVE` snapshot or huge batch key expiration | Stagger background snapshotting; disable huge synchronous snapshot during peak traffic |
| Microservices failing health check on Redis disconnect | Single Redis master node failed without Sentinel failover | Migrate to Redis Sentinel or AWS ElastiCache / Redis Cluster with multi-AZ failover |
| `CROSSSLOT Keys in request don't hash to the same slot` | Multi-key transaction or Lua script touching keys across different shards | Wrap key prefix in hash tags: `{tenant:101}:user` and `{tenant:101}:orders` |

---

## 5. Production Caching Checklist

- [ ] `maxmemory` configured to 70–80% of host memory; `maxmemory-policy` explicitly set (e.g. `allkeys-lru`).
- [ ] Every cached key has an explicit finite TTL; zero immortal keys without TTL.
- [ ] Expiration TTLs include randomized jitter (+/- 10–20%) to prevent simultaneous cache avalanches.
- [ ] Single-flight locking or probabilistic early refresh enabled on hot keys (Anti-Stampede).
- [ ] Multi-tenant caches strictly namespace keys by tenant ID (`tenant:{id}:{resource}`).
- [ ] Cache penetration protected via short-lived sentinel null caching or Bloom filter screening.
- [ ] Cache mutations inside `@Transactional` methods deferred to `afterCommit` synchronization.
- [ ] Dangerous commands (`KEYS`, `FLUSHALL`, `FLUSHDB`) disabled or renamed in `redis.conf`.
- [ ] JDK serialization (`JdkSerializationRedisSerializer`) banned; clean JSON or UTF-8 string serialization used.
- [ ] Metric alerts configured for `used_memory_rss`, `mem_fragmentation_ratio`, `connected_clients`, and `instantaneous_ops_per_sec`.
