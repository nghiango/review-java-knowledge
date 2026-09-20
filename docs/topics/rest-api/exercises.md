# REST API Exercises

Hands-on exercises to practice RESTful API architecture, idempotency control, RFC 9457 Problem Details, and keyset cursor pagination.

## Exercise 1: Implement an Idempotency Interceptor Filter

Implement an HTTP filter or `HandlerInterceptor` that intercepts requests containing an `Idempotency-Key` header, queries an in-memory cache, and returns cached responses when duplicate keys are encountered.

### Requirements
- Create `IdempotencyFilter` extending `OncePerRequestFilter`.
- Intercept mutating HTTP methods (`POST`, `PUT`, `PATCH`).
- If `Idempotency-Key` is present and previously cached, short-circuit request execution and write the cached status code and payload directly to `HttpServletResponse`.
- Add header `Idempotency-Replayed: true` on cached responses.

??? question "Reveal solution"
    ```java
    @Component
    public class IdempotencyFilter extends OncePerRequestFilter {

        private final ConcurrentHashMap<String, CachedResponse> responseStore = new ConcurrentHashMap<>();

        public record CachedResponse(int status, String body, String contentType) {}

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {

            String idempotencyKey = request.getHeader("Idempotency-Key");
            String method = request.getMethod();

            if (idempotencyKey == null || "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
                filterChain.doFilter(request, response);
                return;
            }

            CachedResponse cached = responseStore.get(idempotencyKey);
            if (cached != null) {
                response.setStatus(cached.status());
                response.setContentType(cached.contentType());
                response.setHeader("Idempotency-Replayed", "true");
                response.getWriter().write(cached.body());
                return;
            }

            ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);
            filterChain.doFilter(request, responseWrapper);

            int status = responseWrapper.getStatus();
            if (status >= 200 && status < 300) {
                String body = new String(responseWrapper.getContentAsByteArray(), StandardCharsets.UTF_8);
                responseStore.put(idempotencyKey, new CachedResponse(status, body, responseWrapper.getContentType()));
            }

            responseWrapper.copyBodyToResponse();
        }
    }
    ```

---

## Exercise 2: Keyset Cursor Pagination Implementation

Implement keyset (cursor-based) pagination for an activity feed without using `OFFSET`.

### Requirements
- Implement `findFeedAfter(UUID cursor, int limit)` returning a bounded list of items.
- Return the next cursor token to the client.
- Guarantee $O(1)$ query complexity regardless of how deep the user scrolls.

??? question "Reveal solution"
    ```java
    public record FeedItem(UUID id, Instant createdAt, String message) {}
    public record CursorPage<T>(List<T> items, UUID nextCursor, boolean hasMore) {}

    @Service
    public class ActivityFeedService {
        private final List<FeedItem> allItems = new ArrayList<>();

        public CursorPage<FeedItem> getFeed(UUID lastSeenId, int size) {
            int safeSize = Math.min(Math.max(1, size), 50);

            int startIndex = 0;
            if (lastSeenId != null) {
                for (int i = 0; i < allItems.size(); i++) {
                    if (allItems.get(i).id().equals(lastSeenId)) {
                        startIndex = i + 1;
                        break;
                    }
                }
            }

            int endIndex = Math.min(startIndex + safeSize, allItems.size());
            List<FeedItem> pageItems = allItems.subList(startIndex, endIndex);
            boolean hasMore = endIndex < allItems.size();
            UUID nextCursor = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1).id();

            return new CursorPage<>(pageItems, nextCursor, hasMore);
        }
    }
    ```
