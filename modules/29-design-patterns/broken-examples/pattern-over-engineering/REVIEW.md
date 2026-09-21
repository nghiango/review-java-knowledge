# Code Review: Pattern Over-Engineering

## Context
A developer submitted a pull request implementing a utility to export a list of user records as CSV text.
To make it "future-proof" and "enterprise-grade", they introduced `AbstractExportFactoryProviderBridge`, `ExportBridgeVisitor`, and `ExportContextStrategyBuilder`.

## Your Task
Inspect `UserExportService.java`, `AbstractExportFactoryProviderBridge.java`, and `ExportContextStrategyBuilder.java`.
Review the code for:
- Accidental complexity and premature abstraction (YAGNI / KISS violations).
- Cognitive load and maintenance burden.
- How this should be refactored using idiomatic Java 21 features.
