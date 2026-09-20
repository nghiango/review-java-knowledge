package lab.springmvc.questions;

import java.util.concurrent.TimeUnit;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;

public class Q16HttpCachingHeadersExample {

    public static void main(String[] args) {
        // Cache-Control headers inform browsers and reverse proxies of cacheability rules
        CacheControl cacheControl =
                CacheControl.maxAge(1, TimeUnit.HOURS).noTransform().mustRevalidate();

        ResponseEntity<String> response =
                ResponseEntity.ok()
                        .cacheControl(cacheControl)
                        .eTag("\"v12345\"")
                        .body("Cached static content");

        boolean hasEtag = response.getHeaders().getETag() != null; // true
        String headerValue =
                response.getHeaders()
                        .getCacheControl(); // "max-age=3600, must-revalidate, no-transform"

        System.out.println("ETag: " + hasEtag + ", Cache-Control header: " + headerValue);
    }
}
