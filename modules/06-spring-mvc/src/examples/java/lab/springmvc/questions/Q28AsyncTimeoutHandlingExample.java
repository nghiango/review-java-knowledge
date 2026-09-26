package lab.springmvc.questions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

@SuppressWarnings("unused")
public final class Q28AsyncTimeoutHandlingExample {
    private Q28AsyncTimeoutHandlingExample() {}

    public static DeferredResult<ResponseEntity<String>> processWithTimeout() {
        // 5000ms timeout configured on the DeferredResult:
        DeferredResult<ResponseEntity<String>> result = new DeferredResult<>(5000L);

        // onTimeout callback invoked if the asynchronous thread does not complete before 5000ms:
        result.onTimeout(
                () -> {
                    result.setErrorResult(
                            ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                                    .body("{\"error\":\"Request timed out after 5000ms\"}"));
                });

        // onError callback handles uncaught exceptions from worker threads:
        result.onError(
                (Throwable t) -> {
                    result.setErrorResult(
                            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                    .body("{\"error\":\"Internal worker error\"}"));
                });

        return result;
    }

    public static void main(String[] args) {
        DeferredResult<ResponseEntity<String>> deferred = processWithTimeout();
        boolean hasTimeout = deferred.isSetOrExpired(); // false initially
    }
}
