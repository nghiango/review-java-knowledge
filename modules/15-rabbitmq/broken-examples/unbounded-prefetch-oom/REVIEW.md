# Code Review — Unbounded Prefetch Buffer Exhausting JVM Heap

## Context

A video processing pipeline consumes transcoding tasks from RabbitMQ queue `video.transcoding.jobs`. Each transcoding job is computationally expensive and memory-intensive, requiring 30–60 seconds of CPU rendering.

Review `VideoTranscodingConsumer.java` for prefetch mechanics, memory utilization, worker dispatch fairness, and crash vulnerability.

## What to look for

- The AMQP prefetch count setting on the listener container factory (`factory.setPrefetchCount(0)`)
- How RabbitMQ pushes messages to consumers when prefetch is unbounded
- Memory footprint when a surge of hundreds of transcoding jobs is queued
- Distribution of workload across multiple competing consumer instances
