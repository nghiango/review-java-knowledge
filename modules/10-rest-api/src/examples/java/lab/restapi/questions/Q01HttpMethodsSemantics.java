package lab.restapi.questions;

import java.util.Objects;
import org.springframework.http.HttpMethod;

public class Q01HttpMethodsSemantics {

    public static void main(String[] args) {
        // Safe methods: Do not modify server state (read-only)
        boolean isGetSafe = isSafeMethod(HttpMethod.GET); // true
        boolean isHeadSafe = isSafeMethod(HttpMethod.HEAD); // true
        boolean isPostSafe = isSafeMethod(HttpMethod.POST); // false
        boolean isPutSafe = isSafeMethod(HttpMethod.PUT); // false
        boolean isDeleteSafe = isSafeMethod(HttpMethod.DELETE); // false

        // Idempotent methods: N > 0 identical requests produce the same side-effect on server state
        boolean isGetIdempotent = isIdempotentMethod(HttpMethod.GET); // true
        boolean isPutIdempotent = isIdempotentMethod(HttpMethod.PUT); // true
        boolean isDeleteIdempotent = isIdempotentMethod(HttpMethod.DELETE); // true
        boolean isPostIdempotent = isIdempotentMethod(HttpMethod.POST); // false
        boolean isPatchIdempotent = isIdempotentMethod(HttpMethod.PATCH); // false

        System.out.println(
                "GET safe="
                        + isGetSafe
                        + ", idempotent="
                        + isGetIdempotent); // GET safe=true, idempotent=true
        System.out.println("HEAD safe=" + isHeadSafe); // HEAD safe=true
        System.out.println(
                "POST safe="
                        + isPostSafe
                        + ", idempotent="
                        + isPostIdempotent); // POST safe=false, idempotent=false
        System.out.println(
                "PUT safe="
                        + isPutSafe
                        + ", idempotent="
                        + isPutIdempotent); // PUT safe=false, idempotent=true
        System.out.println(
                "DELETE safe="
                        + isDeleteSafe
                        + ", idempotent="
                        + isDeleteIdempotent); // DELETE safe=false, idempotent=true
        System.out.println("PATCH idempotent=" + isPatchIdempotent); // PATCH idempotent=false
    }

    public static boolean isSafeMethod(HttpMethod method) {
        return Objects.equals(method, HttpMethod.GET)
                || Objects.equals(method, HttpMethod.HEAD)
                || Objects.equals(method, HttpMethod.OPTIONS);
    }

    public static boolean isIdempotentMethod(HttpMethod method) {
        return Objects.equals(method, HttpMethod.GET)
                || Objects.equals(method, HttpMethod.HEAD)
                || Objects.equals(method, HttpMethod.PUT)
                || Objects.equals(method, HttpMethod.DELETE);
    }
}
