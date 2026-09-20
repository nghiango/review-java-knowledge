# Code Review — Retry Storm Missing Jitter

## Context

A cross-border payments application converts foreign exchange rates via an external Forex provider. During peak volume, thousands of worker threads query currency rates concurrently. The developer implemented exponential backoff ($500\text{ms} \times 2^{k-1}$) to relieve pressure on failure.

Review `CurrencyRateService.java` for distributed synchronization, retry storm amplification, and thundering herds.

## What to look for

- Deterministic vs stochastic backoff intervals
- Synchronization of thousands of concurrent failed callers
- Downstream overload behavior during recovery phases
- Circuit breaking and failure propagation
