# Code Review: Missing Method Security & Parameter-Based Authorization

## Context
A document collaboration service handles sensitive corporate reports. Users can view and update documents depending on whether they are the document owner or an administrator.

## Target Files
- [`DocumentTransferService.java`](DocumentTransferService.java)
- [`InsecureDocumentController.java`](InsecureDocumentController.java)

## Task
Review `DocumentTransferService.java` and `InsecureDocumentController.java`. Identify lack of defense-in-depth method authorization, parameter spoofing risks, and missing `@PreAuthorize` annotations.
