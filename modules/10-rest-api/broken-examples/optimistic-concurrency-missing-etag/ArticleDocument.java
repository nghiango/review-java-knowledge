package lab.restapi.broken.etagconcurrency;

import java.time.Instant;
import java.util.UUID;

public class ArticleDocument {
    private final UUID id;
    private String title;
    private String content;
    private long version;
    private Instant updatedAt;

    public ArticleDocument(UUID id, String title, String content, long version, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
