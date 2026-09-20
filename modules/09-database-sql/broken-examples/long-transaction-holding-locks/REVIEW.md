# Code Review: Long Transaction Holding Locks

## Context
An accounts receivable batch service processes invoices. For each invoice, it acquires a pessimistic row lock (`SELECT FOR UPDATE`), generates a PDF, invokes an external tax validation endpoint, and updates the invoice status.

## Code Under Review
- `InvoiceProcessingService.java` — `processInvoice(invoiceId)` method holding a database transaction and exclusive row lock during slow network I/O.

## Review Questions
1. What happens to database connection pools and concurrent reader/writer queries when slow remote calls are executed inside an exclusive row lock transaction?
2. How should the transaction boundary be refactored to minimize lock holding duration?
3. What is the state machine pattern for decoupling asynchronous I/O from database transactions?
