# Code Review: Eager Fetching Anti-Pattern

## Background
`CompanyDirectoryService` lists simple department names. In production, profiling revealed that executing `listDepartmentNames()` triggered multiple large SQL left outer joins across `departments`, `employees`, and `projects`, resulting in massive memory allocation and Slow Query logs.

## Questions to Consider
1. What is the impact of placing `fetch = FetchType.EAGER` on collection relationships?
2. Can `FetchType.EAGER` be overridden or disabled at query time in JPQL?
3. Why should `FetchType.LAZY` be the universal default across all entity associations?
