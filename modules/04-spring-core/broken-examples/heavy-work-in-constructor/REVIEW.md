# Review: Blocking network calls and thread spawning in @PostConstruct

Review `RemoteRateClient.java` and `ExchangeRateService.java` as a production pull request.
This service maintains foreign exchange rates used by checkout and pricing microservices.

Identify every issue you can find. Consider:

- the purpose and execution context of `@PostConstruct` and constructor initialization
- impact of blocking network calls on container startup time and health probes
- unmanaged thread creation vs Spring `TaskScheduler` or `@Scheduled`
- thread safety of collections updated during periodic background refreshes
- handling external service outages during application bootstrap

Write your findings before opening `SOLUTION.md`.

Validate that the review target remains compilable with:

```bash
./gradlew :modules:04-spring-core:compileBrokenExamples
```
