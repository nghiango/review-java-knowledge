package lab.restapi.questions;

import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;

public class Q06HttpCachingHeaders {

    public static void main(String[] args) {
        // Cache-Control header builder
        CacheControl cacheControl =
                CacheControl.maxAge(3600, TimeUnit.SECONDS).mustRevalidate().cachePublic();

        String cacheHeaderValue =
                cacheControl.getHeaderValue(); // "max-age=3600, must-revalidate, public"
        boolean isMustRevalidate = cacheHeaderValue.contains("must-revalidate"); // true
        boolean hasMaxAge = cacheHeaderValue.contains("max-age=3600"); // true

        // ETag conditional validation:
        String serverETag = "\"v1-hash-abc\"";
        String clientIfNoneMatch = "\"v1-hash-abc\"";
        boolean isNotModified =
                serverETag.equals(clientIfNoneMatch); // true (leads to HTTP 304 Not Modified)

        System.out.println(
                "Cache Header: "
                        + cacheHeaderValue); // Cache Header: max-age=3600, must-revalidate, public
        System.out.println(
                "Must revalidate: "
                        + isMustRevalidate
                        + ", Max age: "
                        + hasMaxAge); // Must revalidate: true, Max age: true
        System.out.println("304 match: " + isNotModified); // 304 match: true
    }
}
