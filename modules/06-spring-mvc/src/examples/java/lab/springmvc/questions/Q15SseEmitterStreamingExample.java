package lab.springmvc.questions;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public class Q15SseEmitterStreamingExample {

    public static void main(String[] args) throws Exception {
        // SseEmitter allows streaming asynchronous push events to HTTP clients using
        // text/event-stream
        SseEmitter emitter = new SseEmitter(10000L);

        boolean isTimeoutPositive = emitter.getTimeout() > 0; // true
        emitter.send(SseEmitter.event().name("price-update").data("AAPL: $185.20"));
        emitter.complete();

        System.out.println("SSE Emitter completed stream: " + isTimeoutPositive);
    }
}
