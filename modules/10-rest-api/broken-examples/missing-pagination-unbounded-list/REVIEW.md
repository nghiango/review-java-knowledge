# Code Review: Missing Pagination & Unbounded List

## Context
A product catalog REST controller was introduced to serve e-commerce catalog listings and search queries. Over time, product records have grown from thousands to millions of entries.

## Target Files
- [`CatalogItem.java`](CatalogItem.java)
- [`CatalogController.java`](CatalogController.java)

## Task
Review `CatalogController.java`. Identify memory, latency, and API contract risks associated with unbounded collections, deep offset scanning, and missing pagination response metadata.
