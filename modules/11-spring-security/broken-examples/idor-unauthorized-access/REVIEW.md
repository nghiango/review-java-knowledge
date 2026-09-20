# Code Review: Insecure Direct Object Reference (IDOR / BOLA)

## Context
A billing microservice exposes customer invoice details via REST endpoints. Each invoice belongs to a specific customer account.

## Target Files
- [`Invoice.java`](Invoice.java)
- [`InvoiceController.java`](InvoiceController.java)

## Task
Review `InvoiceController.java`. Identify Broken Object Level Authorization (BOLA / IDOR) vulnerabilities and lack of ownership validation against the authenticated principal.
