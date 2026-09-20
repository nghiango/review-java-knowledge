# Solution: Missing Method Security & Parameter-Based Authorization

## Annotated Code

### `DocumentTransferService.java`
```java
package lab.springsecurity.broken.methodsecurity;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class DocumentTransferService {

    private final Map<UUID, DocumentRecord> store = new ConcurrentHashMap<>();

    public DocumentTransferService() {
        UUID doc1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        store.put(doc1, new DocumentRecord(doc1, "alice", "Financial Report 2026", "Confidential quarterly figures"));
    }

    // Security issue: Missing method-level authorization (@PreAuthorize). Any internal bean, scheduler, or unvalidated controller can invoke this method.
    // Security issue: Business logic trusts caller-supplied 'requesterUsername' parameter rather than validating against SecurityContextHolder.getContext().getAuthentication().
    public DocumentRecord updateDocument(UUID id, String requesterUsername, String newContent) {
        DocumentRecord record = store.get(id);
        if (record == null) {
            throw new IllegalArgumentException("Document not found");
        }

        DocumentRecord updated = new DocumentRecord(record.id(), record.owner(), record.title(), newContent);
        store.put(id, updated);
        return updated;
    }

    public DocumentRecord getDocument(UUID id) {
        return store.get(id);
    }

    public record DocumentRecord(UUID id, String owner, String title, String content) {}
}
```

## Issue Catalogue

| Issue | Category | Severity | Description |
|---|---|---|---|
| Missing Method Security Authorization | Security | Major | Relying exclusively on URL-based filter security leaves backend service methods vulnerable when invoked by other components, message consumers, or misconfigured routes. Enabling `@EnableMethodSecurity` and applying `@PreAuthorize` provides essential defense-in-depth (CWE-285). |
| Caller-Supplied Identity Spoofing | Security | Critical | Accepting the requester's username as an untrusted parameter (`String username`) allows attackers to impersonate document owners without credentials (CWE-290). Identity must be extracted exclusively from the authenticated `SecurityContext`. |

## Correct Implementation Reference
- [`DocumentService.java`](../../src/main/java/lab/springsecurity/methodsecurity/DocumentService.java)
- [`MethodSecurityConfig.java`](../../src/main/java/lab/springsecurity/methodsecurity/MethodSecurityConfig.java)
