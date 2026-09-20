package lab.webflux.questions;

import reactor.core.publisher.Mono;

public class Q21ReactiveDistributedTransactionsSagaExample {

    record SagaStepResult(String step, boolean success, String compensationStatus) {}

    @SuppressWarnings({"UnusedVariable", "unused"})
    public static void main(String[] args) {
        // In reactive microservices using WebClient or R2DBC, traditional 2PC distributed
        // transactions are anti-patterns.
        // Distributed consistency is maintained via asynchronous reactive Saga orchestration:
        // Each step executes an asynchronous non-blocking action; failures trigger compensating
        // reactive flows.
        Mono<SagaStepResult> sagaFlow =
                Mono.just(new SagaStepResult("InventoryReserve", true, "NONE"))
                        .flatMap(
                                step1 -> {
                                    // Subsequent step fails, triggering compensation
                                    return Mono.just(
                                            new SagaStepResult(
                                                    "PaymentCharge",
                                                    false,
                                                    "COMPENSATED_INVENTORY"));
                                });

        SagaStepResult result = sagaFlow.block();
        boolean compensated = result.compensationStatus().equals("COMPENSATED_INVENTORY"); // true

        System.out.println("Reactive Saga completed step: " + result.step());
        System.out.println("Compensating action triggered: " + compensated);
    }
}
