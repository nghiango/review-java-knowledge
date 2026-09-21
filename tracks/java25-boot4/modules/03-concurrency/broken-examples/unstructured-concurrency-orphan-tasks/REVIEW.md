# Code Review Target: Unstructured Concurrency & Orphan Task Leaks

## Overview
`OrderFulfillmentService` fans out order fulfillment into parallel inventory reservation and payment processing steps using `CompletableFuture.supplyAsync()` backed by virtual threads.

## Code Under Review
Review `OrderFulfillmentService.java`.

## Questions for the Reviewer
1. What happens when `reserveInventory()` fails immediately? Does `paymentFuture` get cancelled?
2. How does `CompletableFuture.allOf()` handle exception propagation and cancellation of sibling threads?
3. How does Java 25 Structured Concurrency (`StructuredTaskScope.ShutdownOnFailure`) resolve orphan task execution?
