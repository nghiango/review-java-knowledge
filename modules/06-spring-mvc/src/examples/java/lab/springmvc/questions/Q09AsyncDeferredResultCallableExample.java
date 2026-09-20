package lab.springmvc.questions;

import org.springframework.web.context.request.async.DeferredResult;

public class Q09AsyncDeferredResultCallableExample {

    public static void main(String[] args) {
        // DeferredResult releases container worker thread while asynchronous event completes
        // out-of-band
        DeferredResult<String> deferredResult = new DeferredResult<>(3000L, "TIMEOUT");

        boolean isInitiallySet = deferredResult.isSetOrExpired(); // false
        deferredResult.setResult("ASYNC_PAYLOAD");
        boolean isFieldSet = deferredResult.isSetOrExpired(); // true
        Object result = deferredResult.getResult(); // "ASYNC_PAYLOAD"

        System.out.println(
                "Initially set: "
                        + isInitiallySet
                        + ", after result: "
                        + isFieldSet
                        + ", payload: "
                        + result);
    }
}
