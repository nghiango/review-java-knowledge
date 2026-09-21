# Code Review Target: Virtual Thread Context Propagation & Leaks

## Overview
A microservice receives tenant-scoped requests. `TenantContextHolder` uses an `InheritableThreadLocal` to propagate tenant identifiers down to helper routines and spawned background tasks.

## Code Under Review
Review `TenantContextHolder.java`.

## Questions for the Reviewer
1. What happens when millions of virtual threads access `InheritableThreadLocal`?
2. Is context cleanup guaranteed if `task.run()` throws an unchecked exception?
3. In Java 25, how does `ScopedValue` provide both memory efficiency and guaranteed lifecycle boundaries over `ThreadLocal`?
